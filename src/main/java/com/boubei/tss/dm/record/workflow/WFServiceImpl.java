/* ==================================================================   
 * Created [2018-07-26] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IMessageService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * TODO 
 * 1、审批人禁止查看自己无审批权限的记录附件（审批角色对流程表有浏览权限）
 * 
 */
@Service("WFService")
public class WFServiceImpl implements WFService {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired ILoginService loginSerivce;
	@Autowired IMessageService msgService;
	@Autowired IUserDao userDao;
	@Autowired IGroupDao commonDao;
	@Autowired RecordService recordService;
 
	public WFStatus getWFStatus(Long tableId, Long itemId) {
		List<?> list = commonDao.getEntities("from WFStatus where tableId = ?1 and itemId = ?2 ", tableId, itemId);
		return  list.isEmpty() ? null : (WFStatus)list.get(0);
	}
	
	
	/** 物理删除记录时，也要删除流程状态数据 */
	public void removeWFStatus(Long tableId, Long itemId, Map<String, Object> item) {
		WFStatus wfStatus = getWFStatus(tableId, itemId);
		if( wfStatus != null ) {
			commonDao.delete( wfStatus );
			
			wfStatus.setCurrentStatus(WFStatus.REMOVED);
			fireWFEvent(wfStatus, tableId, item);
		}
	}
	
	/** 当前流到登录用户的流程汇总 */
	public Map<Object, Object> getMyWFCount() {
		String sql = "SELECT tableId record, count(*) num FROM dm_workflow_status " +
				" where nextProcessor = ? and currentStatus in ('" +WFStatus.NEW+ "','" +WFStatus.APPROVING+ "','" +WFStatus.TRANS+ "') " +
				" group by tableId";
		List<Map<String, Object>> list = SQLExcutor.queryL(sql, Environment.getUserCode());

		Map<Object, Object> result = new HashMap<Object, Object>();
		for( Map<String, Object> m : list ) {
			result.put( m.get("record") , m.get("num"));
		}
		return result;
	}
	
	public List<?> getTransList(Long recordId, Long itemId) {
		WFStatus wfStatus = getWFStatus(recordId, itemId);
		return getUserList(wfStatus.getTrans());
	}
	
	public boolean calculateWFStatus(Long itemId, _Database _db) {
		
		if( !WFUtil.checkWorkFlow(_db.wfDefine) ) return false;
		
		Map<String, Object> item = _db.get(itemId);
		
		/* 如果是修改记录，则先查询流程状态是否已经存在；如果已存在，检查是否已有过处理（含审批、驳回、转审），有则禁止修改 */
		WFStatus wfStatus = getWFStatus(_db.recordId, itemId);
		boolean isCreate = false;
		if(wfStatus == null) {
			wfStatus = new WFStatus();
			wfStatus.setTableName(_db.recordName);
			wfStatus.setTableId(_db.recordId);
			wfStatus.setItemId( itemId );
			wfStatus.setCurrentStatus(WFStatus.NEW);
			wfStatus.setApplier( Environment.getUserCode() );
			wfStatus.setApplierName( Environment.getUserName() );
			wfStatus.setApplyTime( new Date() );
			wfStatus.setDomain( Environment.getDomainOrign() );
			commonDao.createObject(wfStatus);
			isCreate = true;
			
			WFLog wfLog = new WFLog(wfStatus, null);
			wfLog.setProcessResult(WFStatus.APPLIED);
			commonDao.createObject(wfLog);
		} else {
			if( wfStatus.isAutoPassed() ) {
				wfStatus.setCurrentStatus(WFStatus.NEW); // 自动通过的需要重新设置流程状态为 待审批
			}
			
			WFLog wfLog = new WFLog(wfStatus, null);
			wfLog.setProcessResult(WFStatus.MODIFIED);
			commonDao.update(wfLog);
		}
		
		// 把提交人的信息也作为规则运算条件
		Map<String, Object> context = new HashMap<String, Object>( item );
		context.put("userCode", Environment.getUserCode());
		context.put("userName", Environment.getUserName());
		context.put("group", Environment.getUserGroup());
		for( Long role : Environment.getOwnRoles() ) {
			context.put("role_" + role, role);
		}
		for( String role : Environment.getOwnRoleNames() ) {
			context.put( role, role );
		}
		for( String code : item.keySet() ) {
			String label = _db.cnm.get(code);
			context.put(label, item.get(code)); // 流程条件支持按Label解析
		}
		
		Map<String, List<Map<String, String>>> rules = WFUtil.parseWorkflow(_db.wfDefine, context, _db.recordName);
		
		List<Map<String, String>> to = rules.get("to");
		List<Map<String, String>> cc = rules.get("cc");
		List<Map<String, String>> trans = rules.get("trans");
		
		List<String> tos = getUsers(to, true); 
		List<String> ccs = getUsers(cc, false);
		List<String> transs = getUsers(trans, false);
		
		// 审批和转审人员列表不能包含申请人自己，抄送可以
		String creator = Environment.getUserCode(); 
		tos.remove(creator);
		transs.remove(creator);
		
		String nextProcessor = null;
		if( tos.isEmpty() ) { // 没有审批人
			wfStatus.setCurrentStatus(WFStatus.AUTO_PASSED);
		} else {
			nextProcessor = tos.get(0);
		}
		wfStatus.setNextProcessor( nextProcessor );
		
		wfStatus.setStepCount( tos.size() );
		wfStatus.setTo( EasyUtils.list2Str(tos) );
		wfStatus.setCc( EasyUtils.list2Str(ccs) );
		wfStatus.setTrans(  EasyUtils.list2Str(transs)  );
		
		updateWFStatus(wfStatus, isCreate);
		
		return true;
	}
	
	/**
	 * 安装流程规则及当前申请人登录信息，获取审批人（或抄送人）列表
	 */
	public List<String> getUsers(List<Map<String, String>> rule, boolean justOne) {
		// 去除重复的审批人（同个人多个角色）
		Set<String> users = new LinkedHashSet<String>();
		if(rule == null) return new ArrayList<>();
		
		
		for(Map<String, String> m : rule) {
			if(m == null || (m.containsKey("when") && !"true".equals(m.get("when"))) ) {
				continue;
			}
			
			String user = m.get("user"); // 直接指定具体个人为审批人，使用loginName
			if( !EasyUtils.isNullOrEmpty(user) ) { 
				try {
					userDao.getUserByAccount(user, true);  // 检测用户是否存在 且 是否还启用着（要求可登陆的账号）
					users.add( user );
				} 
				catch(Exception e) { }
			}
			
			String role = m.get("roleId"); // 或角色，优先使用和申请人同组的主管
			if( !EasyUtils.isNullOrEmpty(role) ) {
				Long roleId = EasyUtils.obj2Long(role);
				List<Object[]> roleUsers = getSameGroupUserByRole(roleId, justOne);
				if( roleUsers.size() > 0 ) {
					users.addAll( Arrays.asList( EasyUtils.list2Str(roleUsers, 0).split(",") ) );
				}
			}
		}
 
		return new ArrayList<String>(users);
	}
	
	/**
	 * 按角色获取拥有此角色的所有用户 及 组织
	 */
	private List<Object[]> getSameGroupUserByRole(Long roleId, boolean justOne) {
		
		List<Object[]> result = new ArrayList<Object[]>();
		if( justOne && Environment.getOwnRoles().contains(roleId) ) { // 如果自己就拥有该角色，则无需此角色的人审批；抄送和转审，允许同角色
			return result;
		}
		
		Group inGroup = commonDao.getEntity( (Long) Environment.getInSession(SSOConstants.USER_GROUP_ID) );
		List<OperatorDTO> list = loginSerivce.getUsersByRoleId(roleId);
		
		for(OperatorDTO dto : list) {
			String userCode = dto.getLoginName();
			
			// 剔除名称和域编码一致的域管理员账号（比如：CX、BD，其它购买生成的域管理员不剔除）
			if( userCode.equals(Environment.getDomain()) || userCode.endsWith("_manager") ) continue;
			
			result.add( new String[]{ userCode, dto.getUserName()} );
			
			// 只取一个同类角色的用户作为审批人，比如部门经理
			if( justOne ) {
				// 如果用户拥有此角色且其所在组为提交人所在组（或父组，优先用子组里的），则优先使用
				List<Group> fatherGroups = loginSerivce.getGroupsByUserId(dto.getId());
				if( fatherGroups.size() > 0 ) {
					Group lastGroup = fatherGroups.get( fatherGroups.size() - 1 );
					String decode = lastGroup.getDecode();
		        	
		        	if( inGroup != null && inGroup.getDecode().startsWith( decode ) ) {
		        		int size = result.size();
						return result.subList(size - 1, size ); // 取最后一个
		        	}
				}
			}
		}
		
		return justOne ? result.subList(0, Math.min(result.size(), 1)) : result; // 只取第一个
	}
	
	/**
	 * 我审批的: 
	 * 		1、待审批的  nextProcessor = 我
	 * 		2、已审批的  processors.contains(我): 含已审批、已转审、已驳回）
	 * 		3、抄送给我的 cc.contains(我)
	 * 注：剔除已撤销的
	 */
	public SQLExcutor queryMyTasks(_Database _db, Map<String, String> params, int page, int pagesize) {
		String userCode = Environment.getUserCode(); 
    	String wrapCode = MacrocodeQueryCondition.wrapLike(userCode);
    	
    	/* 支持按类型分开查询: wfing: 待审批  wfdone: 已审批  cc: 抄送  */
		String _condition = "<#if wfing?? >  or nextProcessor = '" +userCode+ "' </#if> " +
				            "<#if wfdone?? > or processors like '" +wrapCode+ "' </#if> " +
				            "<#if wfcc?? >   or cc like '" +wrapCode+ "' </#if> ";
		
		String condition = EasyUtils.fmParse(_condition, params);
		if( EasyUtils.isNullOrEmpty(condition) ) {
			params.put("wfing", "true");
			params.put("wfdone", "true");
			params.put("wfcc", "true");
			condition = EasyUtils.fmParse(_condition, params);
		}
		String hsql = "from WFStatus where ( 1=0 " +condition+ " )  and tableId = ?1 and currentStatus ";
		String currStatus = params.remove("wf_status"); // 按流程状态查询
		if( currStatus != null ) {
			hsql += " = ?2 ";
		} else {
			currStatus = WFStatus.CANCELED;
			hsql += " <> ?2 ";
		}
		List<?> statusList = commonDao.getEntities(hsql, _db.recordId, currStatus);
		
		boolean isApprover = statusList.size() > 0; // 判断当前用户是否为审批人
    	List<Long> itemIds = new ArrayList<Long>();
    	Map<Long, WFStatus> statusMap = new HashMap<Long, WFStatus>();
    	
    	for(Object obj : statusList) {
    		WFStatus status = (WFStatus) obj;
    		Long itemId = status.getItemId();
			itemIds.add(itemId);
    		statusMap.put(itemId, status);
    	}
    	
    	itemIds.add(-999L); // 防止id条件为空把所有记录都查出来了
    	String ids = (String) EasyUtils.checkNull(params.get("ids"), EasyUtils.list2Str(itemIds)); // 前台可能已传入了指定的ids
    	params.put("ids", ids);
    	SQLExcutor ex = _db.select(page, pagesize, params, isApprover);
    	params.remove("ids");
		List<Map<String, Object>> items = ex.result;
		
		Map<String, String> usersMap = loginSerivce.getUsersMap(Environment.getDomain());
    	
    	// 加上每一行的流程状态： 审批中、已通过、已撤销、已驳回、已转审
    	for(Map<String, Object> item : items) {
    		Object itemId = item.get("id");
			WFStatus wfStatus = statusMap.get(itemId);
			wfStatus = (WFStatus) EasyUtils.checkNull(wfStatus, new WFStatus());
			
			item.put("wfstatus", wfStatus.getCurrentStatus());
			item.put("wfapplier", wfStatus.getApplierName());
			item.put("wfapplyTime", item.get("createtime"));
			item.put("wfto", transAccout2CName(usersMap,  wfStatus.getTo()));
			item.put("nextProcessor", transAccout2CName(usersMap, wfStatus.getNextProcessor()));
			item.put("wfcc", transAccout2CName(usersMap, wfStatus.getCc()));
			
			item.put("lastProcessor",transAccout2CName(usersMap,  wfStatus.getLastProcessor()));
			item.put("processors", wfStatus.getProcessors());
    	}
    	
    	return ex;
	}
	
	private String transAccout2CName(Map<String, String>  usersMap, String userCode) {
		userCode = EasyUtils.obj2String(userCode);
		String[] userCodes = userCode.split(",");
		List<String> cNames = new ArrayList<>();
		for( String code : userCodes ) {
			String cName = usersMap.get(code);
			cNames.add( (String) EasyUtils.checkNull(cName, code)  );
		}
		
		return EasyUtils.list2Str(cNames);
	}
	
	public void fixWFStatus(_Database _db, List<Map<String, Object>> items) {
		if( !WFUtil.checkWorkFlow(_db.wfDefine) ) return;
		
		List<Long> itemIds = new ArrayList<Long>();
		Map<Long, Map<String, Object>> itemsMap = new HashMap<Long, Map<String, Object>>();
		for(Map<String, Object> item : items) {
			Long itemId = (Long) item.get("id");
			itemIds.add( itemId );
			itemsMap.put(itemId, item);
		}
		itemIds.add(-999L);
		
		Map<String, String> usersMap = loginSerivce.getUsersMap(Environment.getDomain());
		List<?> statusList = commonDao.getEntities("from WFStatus where tableId = ?1 and itemId in (" +EasyUtils.list2Str(itemIds)+ ") ", _db.recordId);
    	
    	for(Object obj : statusList) {
    		WFStatus wfStatus = (WFStatus) obj;
    		Map<String, Object> item = itemsMap.get( wfStatus.getItemId() );
			item.put("wfstatus", wfStatus.getCurrentStatus());
			item.put("wfapplier", wfStatus.getApplierName());
			item.put("wfapplyTime", item.get("createtime"));
			item.put("wfto", transAccout2CName(usersMap,  wfStatus.getTo()));
			item.put("nextProcessor", transAccout2CName(usersMap, wfStatus.getNextProcessor()));
			item.put("wfcc", transAccout2CName(usersMap, wfStatus.getCc()));
			item.put("lastProcessor",transAccout2CName(usersMap,  wfStatus.getLastProcessor()));
    	}
	}
	
	private List<Map<String, Object>> getUserList(String userCodes) {
		userCodes += ",noThis";
		String sql = "select username, loginName usercode from um_user where loginName in (" +DMUtil.insertSingleQuotes(userCodes)+ ")";
		return SQLExcutor.queryL(sql);
	}

	public void appendWFInfo(_Database _db, Map<String, Object> item, Long itemId) {
		WFStatus wfStatus = getWFStatus(_db.recordId, itemId);
		if( !WFUtil.checkWorkFlow(_db.wfDefine) || wfStatus == null ) return;
		
		// 流程状态
		String processors = EasyUtils.obj2String( wfStatus.getProcessors() );
		String curStatus = wfStatus.getCurrentStatus();
		
		item.put("wfstatus", curStatus);
		item.put("nextProcessor", wfStatus.getNextProcessor());
		item.put("processors", processors);
		item.put("applier", wfStatus.getApplierName() );
		
		// 抄送人列表（中文姓名）
		String cc = wfStatus.getCc();
		item.put("cc_list", EasyUtils.attr2Str(getUserList(cc), "username"));
		
		// 流程日志
		String hql = "from WFLog where tableId = ?1 and itemId = ?2 and processResult <> ?3 order by id ";
		@SuppressWarnings("unchecked")
		List<WFLog> logs = (List<WFLog>) commonDao.getEntities(hql, _db.recordId, itemId, WFStatus.MODIFIED);
		
		// 根据to审批人列表，模拟出审批步骤
		if( WFStatus.NEW.equals( curStatus ) || WFStatus.APPROVING.equals( curStatus ) || WFStatus.TRANS.equals( curStatus ) ) {
			String[] toList = wfStatus.getTo().split(",");
			for( String to : toList ) {
				if( !wfStatus.processorList().contains(to) ) {
					WFLog log = new WFLog();
					log.setProcessor( EasyUtils.attr2Str(getUserList(to), "username") );
					log.setProcessorCode(to);
					log.setProcessResult(WFStatus.UNAPPROVE);
					
					logs.add(log);
				}
			}
		}
		
		item.put("wf_logs", logs);
	}
	
	private WFStatus setWFStatus(Long recordId, Long itemId, boolean isCancel, boolean isApprove) {
		
		String userCode = Environment.getUserCode();
		WFStatus wfStatus = this.getWFStatus(recordId, itemId);
		if( wfStatus.getNextProcessor() == null) {
    		throw new BusinessException(EX.WF_3);
        }
		
		wfStatus.currStepIndex = wfStatus.toUsers().indexOf(userCode);
    	if( wfStatus.currStepIndex < 0 ) {
    		if( !isCancel || !userCode.equals(wfStatus.getApplier()) ) {
    			throw new BusinessException(EX.WF_4);
    		} 
    	}
    	
    	if( isApprove && !Environment.getUserCode().equals(wfStatus.getNextProcessor()) ) {
    		throw new BusinessException(EX.WF_4); 
        }
    	
    	wfStatus.setLastProcessor(Environment.getUserName());
    	wfStatus.setLastProcessTime( new Date() );
    	
    	String processors = EasyUtils.obj2String(wfStatus.getProcessors()) + "," + userCode;
    	if(processors.startsWith(",")) {
    		processors = processors.substring(1);
    	}
		wfStatus.setProcessors( processors );
    	
    	return wfStatus;
	}
	
	public String approve(Long recordId, Long itemId, String opinion) {

		WFStatus wfStatus = setWFStatus(recordId, itemId, false, true);
		String processors = wfStatus.getProcessors();
		
		String currentStatus, nextProcessor;
    	if( processors.split(",").length == wfStatus.getStepCount() ) {
    		currentStatus = WFStatus.PASSED;
    		nextProcessor = null;
    	} else {
    		currentStatus = WFStatus.APPROVING;
    		nextProcessor = wfStatus.toUsers().get(wfStatus.currStepIndex + 1);
    	}
    	wfStatus.setCurrentStatus(currentStatus);
    	wfStatus.setNextProcessor(nextProcessor);
    	updateWFStatus(wfStatus);
    	
    	WFLog wfLog = new WFLog(wfStatus, opinion);
    	wfLog.setProcessResult( WFStatus.APPROVED );
		commonDao.createObject(wfLog);
		
		return currentStatus;
	}
	
	public void reject(Long recordId, Long id, String opinion) {

		WFStatus wfStatus = setWFStatus(recordId, id, false, false);
  
    	wfStatus.setCurrentStatus(WFStatus.REJECTED);
    	wfStatus.setNextProcessor(null);
    	updateWFStatus(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
	
	// 重新修改状态为待审批，清空已审批人列表。完提交人还可以重新编辑申请内容及附件
	public void reApply(Long recordId, Long id, String opinion) {
		WFStatus wfStatus = this.getWFStatus(recordId, id);
		  
    	wfStatus.setCurrentStatus(WFStatus.NEW);
    	wfStatus.setNextProcessor( wfStatus.toUsers().get(0) );
    	wfStatus.setProcessors(null);
    	wfStatus.setLastProcessor(Environment.getUserCode());
    	wfStatus.setLastProcessTime(new Date());
    	updateWFStatus(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		wfLog.setProcessResult(WFStatus.REAPPLY);
		commonDao.createObject(wfLog);
	}
	
	public void transApprove(Long recordId, Long id, String opinion, String target) {
		
		if( EasyUtils.isNullOrEmpty(target) || this.getUserList(target).isEmpty() ) {
			throw new BusinessException(EX.parse(EX.WF_5, target));
		}
		if( getWFStatus(recordId, id).toUsers().contains(target) ) {
			throw new BusinessException( EX.parse(EX.WF_6, target));
		}

		WFStatus wfStatus = setWFStatus(recordId, id, false, false);
    	wfStatus.setCurrentStatus(WFStatus.TRANS);
    	wfStatus.setNextProcessor(target);
    	wfStatus.setStepCount( wfStatus.getStepCount() + 1 );
    	
    	String userCode = Environment.getUserCode();
		wfStatus.setTo( wfStatus.getTo().replaceAll(userCode, userCode + "," + target) );
		
    	updateWFStatus(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
	
	public void cancel(Long recordId, Long id, String opinion) {

		WFStatus wfStatus = setWFStatus(recordId, id, true, false);
		if( !WFStatus.NEW.equals(wfStatus.getCurrentStatus()) ) {
			throw new BusinessException(EX.WF_2);
		}
  
    	wfStatus.setCurrentStatus(WFStatus.CANCELED);
    	wfStatus.setNextProcessor(null);
    	updateWFStatus(wfStatus);
    	
		WFLog wfLog = new WFLog(wfStatus, opinion);
		commonDao.createObject(wfLog);
	}
	
	private void updateWFStatus(WFStatus wfStatus) {
		updateWFStatus(wfStatus, false);
	}
	
	private void updateWFStatus(WFStatus wfStatus, boolean isCreate) {
		commonDao.update(wfStatus);
		
		String tableName = wfStatus.getTableName();
		Long tableId = wfStatus.getTableId();
		Long itemId = wfStatus.getItemId();
		
		// 触发审批流程操作自定义事件（审批通过、驳回、撤销等都可以自定义相应事件）
		_Database db = recordService._getDB(tableId);
		fireWFEvent(wfStatus, tableId, db.get(itemId));
		
		// 发送站内信请求
		String url = "javascript:void(0)"; // "'/tss/modules/dm/recorder.html?id=" +tableId+ "&itemId=" +itemId+ "' target='_blank'";
		String onclick = "parent.openUrl('more/bi_nav.html?_default=" +tableId+ "&_defaultItem=" +itemId+ "')";
		
		// 分别给流程发起人及下一步处理人，发送站内信、邮件、短信等通知；每个申请只在第一次新建流程的时候发送，修改时不发送
		if( !Environment.getUserCode().equals(wfStatus.getApplier()) && !isCreate ) {
			String title = "您提交的流程【" + tableName + "】" + wfStatus.getCurrentStatus();
			String content = title + "，<a href=\"" +url+ "\" onclick=\"" +onclick+ "\">查看最新进度</a>";
			msgService.sendMessage(title, content, wfStatus.getApplier());
		}
		
		String title = wfStatus.getApplierName() + "的【" + tableName + "】待您审批";
		String content = title + "，<a href=\"" +url+ "\" onclick=\"" +onclick+ "\">点击打开处理</a>";
		msgService.sendMessage(title, content, wfStatus.getNextProcessor());
	}


	private void fireWFEvent(WFStatus wfStatus, Long tableId, Map<String, Object> item) {
		Record record = (Record) commonDao.getEntity(Record.class, tableId);
		_Database db = recordService._getDB(tableId);
		
		String wfEventClazz = DMUtil.getExtendAttr(record.getRemark(), "wfEventClass");
		wfEventClazz = (String) EasyUtils.checkNull(wfEventClazz, WFEventN.class.getName());
		WFEvent we = (WFEvent) BeanUtil.newInstanceByName(wfEventClazz);
		we.after(item, wfStatus, db);
	}
}
