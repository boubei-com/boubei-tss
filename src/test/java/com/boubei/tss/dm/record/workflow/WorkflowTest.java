/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record._Recorder;
import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.SystemInfo;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.permission.PermissionService;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MacrocodeCompiler;

import org.junit.Assert;

/**
 * 每一步流程操作都邮件给创建人、及下一级审批人
 * 如果是主管提交请教流程，则直接由总经理审批
 * 开始走流程后，表单内容不允许再修改
 */
@SuppressWarnings({ "unchecked" })
public class WorkflowTest extends AbstractTest4DM {
	
	@Autowired RecordService recordService;
	@Autowired WFService wfService;
	@Autowired _Recorder recorder;
	@Autowired PermissionService permissionService;
	@Autowired SystemInfo si;
	
	Long recordId;
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		SecurityUtil.LEVEL_6 = 6;
	}
	
	private void initTable(String tableName) {
		super.init();
		Logger.getLogger("com.boubei").setLevel(Level.INFO);
		SecurityUtil.LEVEL_6 = 2;
		
		super.initDomain();
        Assert.assertEquals(11, groupService.findGroups().size()); // 含辅助组
        Assert.assertEquals(1, generalSearcher.getDomains().size());
		
        List<Map<String, String>> rule = new ArrayList<>();
        Map<String, String> m = new HashMap<>();
        m.put("roleId", role1.getId().toString());
        rule.add(m);
		wfService.getUsers(rule , true);
        
        Map<String, String> context = new HashMap<String, String>();
        context.put("role0", role0.getId().toString());
        context.put("role1", role1.getId().toString());
        context.put("role2", role2.getId().toString());
        context.put("role3", role3.getId().toString());
        
        _TestUtil.printEntity(commonDao, RoleUser.class.getName());
        try { Thread.sleep(100L); } catch ( Exception e ) { }
        
        Assert.assertTrue( loginSerivce.getRoleIdsByUserId( jk.getId() ).contains(role1.getId()) );
        Assert.assertTrue( loginSerivce.getRoleIdsByUserId( staff1.getId() ).contains(role0.getId()) );
        Collection<Long> l3 = loginSerivce.getRoleIdsByUserId( ceo.getId() );
		Assert.assertTrue( l3.contains(role3.getId()) );
		
		String tblDefine = "[ " +
				"	{'label':'申请人', 'code':'applier', 'type':'string'}," +
        		"	{'label':'日期', 'code':'fromDay', 'type':'date'}," +
        		"	{'label':'天数', 'code':'days', 'type':'number'}," +
        		"	{'label':'状态', 'code':'state'}," +
        		"   {'label':'实请天数','code':'wf_realdays','type':'number'}" +
        		"]";
		
		Record record = new Record();
		record.setName("请假提报");
		record.setType(Record.TYPE1);
		record.setParentId(Record.DEFAULT_PARENT_ID);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable(tableName);
		record.setDefine(tblDefine);
		String wfDef = FileHelper.readResource("workflow/wf_define1");
		record.setWorkflow( MacrocodeCompiler.run(wfDef, context) );
		record.setLogicDel(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		recordId = record.getId();
		
		// 授权给角色，审批角色需要有浏览权限
		String permissions = role1.getId() + "|01100, " +role2.getId()+ "|01100, " +role3.getId()+ "|01100, " +Anonymous._ID+ "|00100";
        permissionService.saveResource2Roles("tss", Record.RESOURCE_TYPE, recordId, "1", permissions);
	}
	
	// 我的审批列表: wfing: 待审批  wfdone: 已审批  cc: 抄送
	private List<Map<String, Object>> my_wf_list(String...types) {
		request = new MockHttpServletRequest();
		request.addParameter("my_wf_list", "true");
		for( String type : types ) {
			request.addParameter(type, "true");
		}
		
		recorder.export(request, new MockHttpServletResponse(), recordId);
		List<Map<String, Object>> list = (List<Map<String, Object>>) recorder.showAsJSON(request, recordId, 1);
		return list;
	}
	
	@Test
	public void testApprove() {
        
		initTable("t_qj_1");
 
		// 普通员工请假（分别请1天、3.5天）
        super.logout();
        super.login(staff1);
        
        request = new MockHttpServletRequest();
        request.addParameter("saveDraft", "true"); // 先存为草稿状态
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "1");
		request.addParameter("state", "新建");
		Long itemID1 = (Long) recorder.createAndReturnID(request, recordId);
		Assert.assertNull(wfService.getWFStatus(recordId, itemID1));
		
		recorder.update(request, response, recordId, itemID1); // 继续保存草稿
		Assert.assertNull(wfService.getWFStatus(recordId, itemID1));
		
		request.removeParameter("saveDraft"); 
		recorder.update(request, response, recordId, itemID1); // 正式提交(保存提交)
		Assert.assertNotNull(wfService.getWFStatus(recordId, itemID1));
		
        request = new MockHttpServletRequest();
        request.addParameter("saveDraft", "true"); // 先存为草稿状态
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-05");
		request.addParameter("days", "3.5");
		request.addParameter("state", "新建");
		Long itemID2 = (Long) recorder.createAndReturnID(request, recordId);
		Assert.assertNull(wfService.getWFStatus(recordId, itemID2));
		
		request.removeParameter("saveDraft"); 
		recorder.apply(new MockHttpServletResponse(), recordId, itemID1 + "," + itemID2); // 正式提交(批量提交)
		Assert.assertNotNull(wfService.getWFStatus(recordId, itemID2));
		
		recorder.get(request, recordId, itemID2);
		Assert.assertEquals(0, my_wf_list().size());
		
		Assert.assertEquals(1, wfService.getWFStatus(recordId, itemID1).toUsers().size());
		Assert.assertEquals(2, wfService.getWFStatus(recordId, itemID2).toUsers().size());
		Assert.assertEquals(1, wfService.getWFStatus(recordId, itemID2).toCCs().size());
		
		// 主管请假 及 审批staff1
		super.logout();
        super.login(jk);
 
		request = new MockHttpServletRequest();
		request.addParameter("applier", "JK");
		request.addParameter("from", "2015-04-05");
		request.addParameter("days", "2");
		request.addParameter("state", "新建");
		Long itemID3 = (Long) recorder.createAndReturnID(request, recordId);
		
		Assert.assertEquals(2, my_wf_list().size()); // 待审批列表
		Assert.assertEquals(2, my_wf_list("wfing").size());
		Assert.assertEquals(0, my_wf_list("wfdone").size());
		
		request = new MockHttpServletRequest();
		request.addParameter("opinion", "同意");
		recorder.approve(request, response, recordId, itemID1);
		request.addParameter("wf_realdays", "4.0");
		recorder.approve(request, response, recordId, itemID2);
		
		recorder.get(request, recordId, itemID2);
		Assert.assertEquals(0, my_wf_list("wfing").size());
		Assert.assertEquals(2, my_wf_list("wfdone").size());
		
		// test query by wf_status
		request = new MockHttpServletRequest();
		request.addParameter("wf_status", WFStatus.DRAFT);
		Assert.assertEquals(0, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size() );
		request.addParameter("my_wf_list", "true");
		Assert.assertEquals(0, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size());
		
		request = new MockHttpServletRequest();
		request.addParameter("wf_status", WFStatus.APPROVING);
		request.addParameter("my_wf_list", "true");
		Assert.assertEquals(1, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size());
		
		request = new MockHttpServletRequest();
		request.addParameter("wf_next_processor", "jk");
		request.addParameter("my_wf_list", "true");
		Assert.assertEquals(0, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size());
		
		request = new MockHttpServletRequest();
		request.addParameter("wf_next_processor", "Jenny");
		request.addParameter("my_wf_list", "true");
		Assert.assertEquals(1, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size());
		
		request = new MockHttpServletRequest();
		request.addParameter("wf_status", WFStatus.NEW);
		Assert.assertEquals(1, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size());
		
		request = new MockHttpServletRequest();
		request.addParameter("wf_process_time", "[2018-01-01,2099-12-31]");
		request.addParameter("my_wf_list", "true"); // 审批模式
		Assert.assertEquals(2, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size());
		
		request = new MockHttpServletRequest();
		request.addParameter("wf_process_time", "2018-01-01"); // 提交模式
		Assert.assertEquals(0, ((List<?>)recorder.showAsJSON(request, recordId, 1)).size());
		
		// 总经理审批
		super.logout();
        super.login(ceo);
        
        Assert.assertEquals(2L, wfService.getMyWFCount().get(recordId));
        
        recorder.approve(request, response, recordId, itemID2);
        recorder.approve(request, response, recordId, itemID3);
        
        recorder.get(request, recordId, itemID2);
        recorder.get(request, recordId, itemID3);
        
        // 总经理自己提报一个申请，找不到审批人
        request = new MockHttpServletRequest();
		request.addParameter("applier", "Jenny");
		request.addParameter("from", "2015-05-05");
		request.addParameter("days", "10");
		request.addParameter("state", "新建");
		Long itemID4 = (Long) recorder.createAndReturnID(request, recordId);
		
		WFStatus wfStatus4 = wfService.getWFStatus(recordId, itemID4);
		Assert.assertNull( wfStatus4.getNextProcessor() );
		Assert.assertEquals(WFStatus.AUTO_PASSED, wfStatus4.getCurrentStatus() );
		Assert.assertEquals(super.domain, wfStatus4.getDomain() );
		
		recorder.update(request, response, recordId, itemID4);
		Assert.assertEquals(WFStatus.AUTO_PASSED, wfStatus4.getCurrentStatus() );
		
		login(staff1);
		try { 
			recorder.get(request, recordId, itemID3);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.DM_08, e.getMessage() );
        }
        
        recordService.getDB(recordId).getGridTemplate();
        printWFLog();
        
        // test 审核后公开: markPublic = true
        logout();
        try { 
			recorder.get(request, recordId, itemID3);
			Assert.fail();
        } catch (Exception e) {
        	Assert.assertEquals( EX.DM_08, e.getMessage() );
        }
        
        login(UMConstants.ADMIN_USER);
        Record rc = recordService.getRecord(recordId);
        rc.setMakePublic( ParamConstants.TRUE );
        recordService.updateRecord(rc);
        
        logout();
        _Database newDB = recordService.getDB(recordId);
        Assert.assertEquals( ParamConstants.TRUE, recordService.getRecord(recordId).getMakePublic() );
        Assert.assertTrue( newDB.makePublic );
        CacheHelper.getLongCache().putObject(_Database._CACHE_KEY(recordId), newDB);
        try { 
        	recorder.get(request, recordId, itemID3);
        } catch (Exception e) {
        	Assert.fail();
        }
	}
	
	@Test
	public void testCancel() {
        
		initTable("t_qj_2");
 
		// 普通员工请假
        super.logout();
        super.login(staff1);
        
        request = new MockHttpServletRequest();
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "1");
		request.addParameter("state", "新建");
		Long itemID1 = (Long) recorder.createAndReturnID(request, recordId);
		
		request = new MockHttpServletRequest();
		request.addParameter("opinion", "同意");
		try {
			recorder.approve(request, response, recordId, itemID1);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.WF_4, e.getMessage() );
        }
		
		request = new MockHttpServletRequest();
		recorder.cancel(request, response, recordId, itemID1);
	
		// 主管登录
		super.logout();
        super.login(jk);
        
        // 撤销的申请主管不可见
		Assert.assertEquals(0, my_wf_list().size()); // 待审批列表
        
        request = new MockHttpServletRequest();
		request.addParameter("opinion", "同意");
		try {
			recorder.approve(request, response, recordId, itemID1);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.WF_3, e.getMessage() );
        }
		
		printWFLog();
	}
	
	@Test
	public void testReject() {
        
		initTable("t_qj_3");
 
		// 普通员工请假
        super.logout();
        super.login(staff1);
        request = new MockHttpServletRequest();
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "1");
		request.addParameter("state", "新建");
		Long itemID1 = (Long) recorder.createAndReturnID(request, recordId);
	
		// 部门经理驳回
		super.logout();
        super.login(jk);
        request = new MockHttpServletRequest();
		request.addParameter("opinion", "不同意，请补充");
		recorder.reject(request, response, recordId, itemID1);
		
		try {
			recorder.approve(request, response, recordId, itemID1);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.WF_3, e.getMessage() );
        }
		
		// 申请人重新发起申请
        super.logout();
        super.login(staff1);
        request = new MockHttpServletRequest();
        request.addParameter("opinion", "再次申请");
		recorder.reApply(request, response, recordId, itemID1);
		
		request = new MockHttpServletRequest();
		recorder.sendWFMessage(request, recordId, itemID1);
		
		request = new MockHttpServletRequest();
		request.addParameter("wfMsg", "已补充详细，请重新审批");
		recorder.sendWFMessage(request, recordId, itemID1);
		
		// 部门经理再次审批通过
		super.logout();
        super.login(jk);
        request = new MockHttpServletRequest();
		request.addParameter("opinion", "同意");
		recorder.approve(request, response, recordId, itemID1);
		
		printWFLog();
	}
	
	@Test
	public void testTransApprove() {
        
		initTable("t_qj_4");
 
		// 普通员工请假
        super.logout();
        super.login(staff1);
        
        request = new MockHttpServletRequest();
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "4");
		request.addParameter("state", "新建");
		Long itemID1 = (Long) recorder.createAndReturnID(request, recordId);
	
		super.logout();
        super.login(jk);
        
        List<?> transList = recorder.transList(request, response, recordId, itemID1);
        Assert.assertEquals(1, transList.size());
        recorder.transListXML(request, response, recordId, itemID1);
        
        // 转审target必填，且用户必须存在
        request = new MockHttpServletRequest();
		request.addParameter("opinion", "我不确定，你来决定");
		try {
			recorder.transApprove(request, response, recordId, itemID1);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.WF_5, ""), e.getMessage() );
        }
		
		request.addParameter("target", "路人甲");
		try {
			recorder.transApprove(request, response, recordId, itemID1);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.WF_5, "路人甲"), e.getMessage() );
        }
		
		// ceo 已在审批人名单中，无需再转审
		request.removeParameter("target");
		request.addParameter("target", ceo.getLoginName());
		try {
			recorder.transApprove(request, response, recordId, itemID1);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(  EX.parse(EX.WF_6, ceo.getLoginName()), e.getMessage() );
        }
		
		// 转给人事经理
		request.removeParameter("target");
		request.addParameter("target", tom.getLoginName());
		recorder.transApprove(request, response, recordId, itemID1);
		
		try {
			recorder.approve(request, response, recordId, itemID1);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.WF_4, e.getMessage() );
        }
		
		// 人事经理审批
		super.logout();
        super.login(tom);
        
        request = new MockHttpServletRequest();
		request.addParameter("opinion", "同意");
		recorder.approve(request, response, recordId, itemID1);
		
		printWFLog();
	}
	
	@Test
	public void testCancelError() {
        
		initTable("t_qj_5");
 
		// 普通员工请假（分别请1天、3.5天）
        super.logout();
        super.login(staff1);
 
        request = new MockHttpServletRequest();
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-05");
		request.addParameter("days", "3.5");
		request.addParameter("state", "新建");
		Long itemID2 = (Long) recorder.createAndReturnID(request, recordId);
		
		Map<String, Object> item1 = recorder.get(request, recordId, itemID2);
		Assert.assertEquals(WFStatus.NEW, item1.get("wfstatus"));
		Assert.assertEquals(jk.getLoginName(), item1.get("nextProcessor"));
		Assert.assertTrue( EasyUtils.isNullOrEmpty(item1.get("processors")) );
		
		List<?> logs = (List<?>) item1.get("wf_logs");
		Assert.assertEquals(3, logs.size());
		log.info( item1 );
		
		// 此时允许修改
		request = new MockHttpServletRequest();
		request.addParameter("days", "4.5");
		recorder.update(request, response, recordId, itemID2);
		
		// 主管登录审批
		super.logout();
        super.login(jk);
 
		request = new MockHttpServletRequest();
		request.addParameter("opinion", "同意");
		recorder.approve(request, response, recordId, itemID2);
		
		item1 = recorder.get(request, recordId, itemID2);
		Assert.assertEquals(WFStatus.APPROVING, item1.get("wfstatus"));
		Assert.assertEquals(ceo.getLoginName(), item1.get("nextProcessor"));
		Assert.assertEquals(jk.getLoginName(), item1.get("processors") );
		
		logs = (List<?>) item1.get("wf_logs");
		Assert.assertEquals(3, logs.size());
		log.info( logs );
		
		// 已审批的不能修改, 也不能取消
		super.logout();
        super.login(staff1);
		try {
			recorder.cancel(request, response, recordId, itemID2);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.WF_2, e.getMessage() );
        }
		
		try {
			request = new MockHttpServletRequest();
			request.addParameter("days", "4.5");
			recorder.update(request, response, recordId, itemID2);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue( e.getMessage().indexOf(EX.WF_1) >= 0  ); // 在 _Recorder.checkRowEditable里校验
        }
		
		// 删除附件
		try {
			uploadDocFile(recordId, itemID2);
			List<?> attachList = recorder.getAttachList(request, recordId, itemID2);
			Assert.assertTrue(attachList.size() == 1);
			RecordAttach ra = (RecordAttach) attachList.get(0);
			
			request = new MockHttpServletRequest();
			recorder.deleteAttach4WX(request, response, ra.getId());
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue( e.getMessage().indexOf(EX.WF_1) >= 0  );
        }
	}
	
	@Test
	public void testRecycle() {
        
		initTable("t_qj_6");
 
		// 普通员工请假
        super.logout();
        super.login(staff1);
        
        request = new MockHttpServletRequest();
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "1");
		request.addParameter("state", "新建");
		Long itemID1 = (Long) recorder.createAndReturnID(request, recordId);
		
		request = new MockHttpServletRequest();
		recorder.export(request, response, recordId);
		
		// 逻辑删除
		request = new MockHttpServletRequest();
		recorder.delete(request, response, recordId, itemID1); 
		recorder.getAttachList(request, recordId, itemID1); // 能看回收站记录的附件
		
		String sql = "select * from dm_workflow_status where tableId = " +this.recordId+ " and itemId=" + itemID1;
		Map<String, Object> result = SQLExcutor.queryL(sql).get(0);
		Assert.assertEquals(WFStatus.REMOVED, result.get("currentstatus") );
		
		// 还原
		recorder.restore(request, response, recordId, itemID1);
		result = SQLExcutor.queryL(sql).get(0);
		Assert.assertEquals(WFStatus.NEW, result.get("currentstatus") );
	
		// 再逻辑删除 + 物理删除（两次逻辑删除 == 物理删除）
		recorder.delete(request, response, recordId, itemID1); 
		recorder.delete(request, response, recordId, itemID1); 
		
		Assert.assertNull( recordService.getDB(recordId).get(itemID1) );
		Assert.assertNull( wfService.getWFStatus(recordId, itemID1) );
	}
	
	@Test
	public void testWFJob() {
		initTable("t_qj_j");
		
		super.logout();
        super.login(staff1);
		
		Object[] params = new Object[] { "staff1", DateUtil.parse("2018-08-01"), 4, "新建", new Date(), "staff1", 0, domain };
		Long itemID1 = SQLExcutor.excuteInsert("insert into t_qj_j(applier,fromDay,days,state,createtime,creator,version,domain) " +
				"values (?,?,?,?,?,?,?,?)", params , "connectionpool");
		
		@SuppressWarnings("deprecation")
		WFJob wfJob = new WFJob();
		wfJob.excuteJob( recordId.toString() );
		Assert.assertNotNull( wfService.getWFStatus( recordId, itemID1 ) );
		
		IOperator excutor = new OperatorDTO(UMConstants.ROBOT_USER_ID, UMConstants.ROBOT_USER_NAME);
		String token = TokenUtil.createToken("1234567890", excutor.getId());
		IdentityCard card = new IdentityCard(token, excutor);
		Context.initIdentityInfo(card);
		wfJob.excuteJob( recordId.toString() );
		
		super.logout();
        super.login(jk);
        
        request = new MockHttpServletRequest();
		request.addParameter("opinion", "不同意");
		recorder.reject(request, response, recordId, itemID1);
	}
	
	@Test
	public void testOther() {
		initTable("t_qj_x");
		
		Assert.assertTrue( wfService.getUsers(null, true).isEmpty() );
		
		new WFLog().setId(null);
		new WFStatus().setId(null);
		try {
			WFUtil.parseWorkflow("abc123\"", new HashMap<String, Object>(), "rc1");
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        }
		
		// 用【主用户组】下账号（staff0）提交申请
        super.logout();
        login(staff0); 
        
        request = new MockHttpServletRequest();
		request.addParameter("applier", "staff0");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "4");
		request.addParameter("state", "新建");
		Long itemID1 = (Long) recorder.createAndReturnID(request, recordId);
		WFStatus ws = wfService.getWFStatus(recordId, itemID1);
		Assert.assertEquals(UMConstants.ADMIN_USER, ws.getNextProcessor() );
		
		Assert.assertTrue(api.getRoles().size() > 0);
		Assert.assertTrue(api.getUsers().size() >= 0);
		Assert.assertNotNull( api.getSessionAttr("userName") );
		Assert.assertNull( aapi.getDomainPic("BD").get("logo") );
		
		request = new MockHttpServletRequest();
		request.addParameter("userCode", "abcd");
		request.addParameter("token", "1234");
		Assert.assertTrue( aapi.checkOnline(request) == 0 );
		
		RecordAttach ra = new RecordAttach();
		ra.setType(1);
		ra.setName("logo.png");
		ra.setFileName("logo");
		commonDao.createObject(ra);
		SQLExcutor.excute("update x_domain set logo='logo.png#" +ra.getId()+ "', ggpic='x#0,y#1' where domain='BD'", DMConstants.LOCAL_CONN_POOL);
		Assert.assertNotNull( aapi.getDomainPic("BD").get("logo") );
		Assert.assertNotNull( aapi.getDomainPic("BD").get("_logo") );
		Assert.assertNull( aapi.getDomainPic("BDxxx").get("logo") );
		Assert.assertEquals("0,1", aapi.getDomainPic("BD").get("_ggpic") );
		
		try {
			aapi.domainPic(response, "BD", "logo");
			commonDao.executeHQL("delete from RecordAttach");
			aapi.domainPic(response, "BD", "logo");
		} catch (Exception e1) {
			Assert.fail();
		}
		
		request = new MockHttpServletRequest();
		request.addParameter("table", "xx");
		request.addParameter("code", "c1");
		request.addParameter("content", "test");
		request.addParameter("udf1", "e8");
		api.createLog(request);
		
		Map<String, String> l1 = loginSerivce.getUsersMap(Environment.getDomain());
		Assert.assertTrue(l1.size() > 0);
		
		Map<Long, String> l2 = loginSerivce.getUsersMapI(Environment.getDomain());
		Assert.assertTrue(l2.size() > 0);
		
		// test API
		String permissions = role1.getId() + "|11, " + role2.getId()+ "|11," + role3.getId()+ "|11";
        permissionService.saveResources2Role("tss", UMConstants.ROLE_RESOURCE_TYPE_ID, -8L, "1", permissions);
        
		login(domainUser);
		Assert.assertTrue(api.getRoles().size() > 0);
		Assert.assertTrue(api.getUsers().size() >= 0);
		boolean ifExist1 = api.setRole4User(request, "newUser001", domainGroup.getName(), role1.getId() + "," + role2.getId());
		request.removeParameter("group");
		boolean ifExist2 = api.setRole4User(request, "newUser001", null, role1.getId() + ", ," + role3.getId());
		api.setRole4User(request, "newUser001", domainGroup.getName(), role1.getId() + ", ,");
		Assert.assertTrue(ifExist2);
		Assert.assertFalse(ifExist1);
		try {
			api.setRole4User(request, "newUser001", domainGroup.getName(), "-1");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.U_54, "(id:-1, name:Admin)"), e.getMessage());
        }
		
		request.addParameter("disabled", "1");
		api.setRole4User(request, "newUser001", domainGroup.getName(), "-1");
		request.removeParameter("disabled");
		request.addParameter("disabled", "0");
		request.addParameter("ignoreExistRoles", "true");
		api.setRole4User(request, "newUser001", domainGroup.getName(), "");
		
		OperatorDTO o = loginSerivce.getOperatorDTOByLoginName("newUser001");
		Assert.assertEquals(1, loginSerivce.getRoleIdsByUserId(o.getId()).size());
		
		Assert.assertEquals(6, generalSearcher.getDomainGroups().size());
		
		// test query wflist by status
		super.logout();
	    login(-1L, "Admin");
		request = new MockHttpServletRequest();
		request.addParameter("my_wf_list", "true");
		request.addParameter("wf_status", WFStatus.NEW);
		List<Map<String, Object>> list = (List<Map<String, Object>>) recorder.showAsJSON(request, recordId, 1);
		Assert.assertTrue(list.size() > 0);
		
		recorder.showAsGrid(request, response, recordId, 1);
		
		// test admin_su
		super.logout();
	    login(domainUser);
	    try {
	    	userService.moveUser(domainUser.getId(), domainGroup.getId() + 1);
	    } catch (Exception e) {
        	Assert.assertEquals(EX.U_52, e.getMessage());
        }
	    Assert.assertNotNull( si.su( request, jk.getLoginName() ) );
		
		super.logout();
	    login(domainUser);
		Assert.assertEquals("Target user is not in your domain", si.su( request, "AdminXXX" ));
		Assert.assertNull( si.su( request, "Admin" ) );
		
		super.logout();
	    login(ceo);
		Assert.assertNull(si.su( request, jk.getLoginName() ));
		
		si.transDomainAdmin(domain, domainUser.getId(), domainUser.getId());
		login("Admin");
		si.transDomainAdmin(domain, domainUser.getId(), domainUser.getId());
	}
	
	private void printWFLog() {
		request = new MockHttpServletRequest();
		Object records = recorder.showAsJSON(request, recordId.toString());
		log.info("apply_list: \n" + records.toString().replaceAll(", \\{", ", \n {") + "\n" );
		
		log.info("my_wf_list: \n" + my_wf_list().toString().replaceAll(", \\{", ", \n {") + "\n" );
		
		log.info("WFStatus: \n" + commonDao.getEntities("from WFStatus").toString().replaceAll(", \\{", ", \n {") + "\n" );
		log.info("WFLogs:  \n" + commonDao.getEntities("from WFLog").toString().replaceAll(", \\{", ", \n {") + "\n" );
		
		log.info("WFMsgs:  \n" + commonDao.getEntities("from Message").toString().replaceAll(", \\{", ", \n {") + "\n" );
	}
}
