/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.dom4j.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.dml.MultiSQLExcutor;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.dm.record.permission.RecordPermission;
import com.boubei.tss.dm.record.permission.RecordResource;
import com.boubei.tss.dm.record.permission._DataItemChecker;
import com.boubei.tss.dm.record.workflow.WFService;
import com.boubei.tss.dm.record.workflow.WFStatus;
import com.boubei.tss.dm.record.workflow.WFUtil;
import com.boubei.tss.dm.report.log.AccessLogRecorder;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.exception.ExceptionEncoder;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.grid.DefaultGridNode;
import com.boubei.tss.framework.web.display.grid.GridDataEncoder;
import com.boubei.tss.framework.web.display.grid.IGridNode;
import com.boubei.tss.framework.web.display.tree.DefaultTreeNode;
import com.boubei.tss.framework.web.display.tree.ITreeNode;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.filter.Filter8APITokenCheck;
import com.boubei.tss.framework.web.mvc.ProgressActionSupport;
import com.boubei.tss.modules.HitRateManager;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Message;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.Imager;
import com.boubei.tss.util.StringUtil;

@Controller
@RequestMapping({ "/auth/xdata", "/xdata/api", "/xdata" })
public class _Recorder extends ProgressActionSupport {
	
	static Logger log = Logger.getLogger(_Recorder.class);

	public static final int PAGE_SIZE = 50;

	@Autowired RecordService recordService;
	@Autowired WFService wfService;
	@Autowired ILoginService loginSerivce;
	@Autowired ICommonService commService;

	_Database getDB(Long recordId, String... permitOptions) {
		// 检测当前用户对当前录入表是否有指定的操作权限
		boolean flag = permitOptions.length == 0;
		for (String permitOption : permitOptions) {
			flag = flag || checkPermission(recordId, permitOption);
		}
		if (!flag) {
			String opts = Arrays.asList(permitOptions).toString();
			opts = opts.replaceFirst("1", "录入");
			opts = opts.replaceFirst("4", "查看");
			opts = opts.replaceFirst("5", "维护");
			List<String> hasRoles = new ArrayList<>( Environment.getOwnRoleNames() );
			hasRoles.remove( UMConstants.ANONYMOUS_ROLE );
			String errorMsg = EX.parse(EX.DM_09, recordService.getRecord(recordId).getName(), opts, hasRoles);
			throw new BusinessException(errorMsg);
		}

		return recordService._getDB(recordId);
	}

	@RequestMapping("/id")
	@ResponseBody
	public List<Long> getRecordIDs(String nameOrTables) {
		String[] list = nameOrTables.split(",");
		List<Long> idList = new ArrayList<Long>();
		for (String nt : list) {
			Long id = recordService.getRecordID(nt, Record.TYPE1, false);
			idList.add(id);
		}

		return idList;
	}

	@RequestMapping("/define/{record}")
	@ResponseBody
	public Object getDefine(HttpServletRequest request, @PathVariable("record") Object record) {

		Long recordId = recordService.getRecordID(record, false);
		prepareParams(request, recordId);

		Record _record = recordService.getRecord(recordId);
		if (!_record.isActive()) {
			throw new BusinessException(EX.DM_10);
		}

		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_EDATA, Record.OPERATION_VDATA);
		return new Object[] { 
				_db.getFields(), 
				_record.getCustomizeJS(), 
				_record.getCustomizeGrid(), 
				_record.getNeedFile(), 
				_record.getBatchImp(),
				_record.getName(), 
				_record.getCustomizePage(),
				_db.getVisiableFields(false), 
				WFUtil.checkWorkFlow(_record.getWorkflow()),
				_db.isLogicDelete(),
				_record.getTable(),
				DMUtil.getExtendAttr(_record.getRemark(), "page_help")
			};
	}
	
	@RequestMapping("/cfields")
	@ResponseBody
	public Collection<?> getCustomizeFields(HttpServletRequest request, String tbl, Long userOrg) {
		
		Filter8APITokenCheck.checkAPIToken(request, "dm_record_field", "D2");
		
		userOrg = (Long) EasyUtils.checkNull(userOrg, Environment.getUserOrg());
		
		boolean pointedTbl = !EasyUtils.isNullOrEmpty(tbl);
		return _Database.queryRecordFields(tbl, userOrg, pointedTbl).values();
	}

	public Map<String, String> prepareParams(HttpServletRequest request, Long recordId) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false);

		/* 其它系统调用接口时，传入其在TSS注册的用户ID; 检查令牌，令牌有效则自动完成登陆 */
		if (recordId > 0) {
			Record record = recordService.getRecord(recordId);
			Filter8APITokenCheck.checkAPIToken(request, record);
		}
		else {
			Filter8APITokenCheck.checkAPIToken(request, "SSO", "0");
		}
		/* 当recordId < 0 时，通常是为定制的表（比如TMS员工表）虚拟一张录入表，用以保存附件 */

		return requestMap;
	}

	@RequestMapping("/xml/{record}/{page}")
	public void showAsGrid(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record,
			@PathVariable("page") int page) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);
		boolean pointedFileds = requestMap.containsKey("fields");
		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_VDATA, Record.OPERATION_EDATA);

		// 默认模糊查询
		String strictQuery = (String) EasyUtils.checkNull(requestMap.get(_Field.STRICT_QUERY), "false");
		requestMap.put(_Field.STRICT_QUERY, strictQuery);

		SQLExcutor ex = queryRecordData(_db, page, PAGE_SIZE, requestMap, pointedFileds, true);

		List<IGridNode> temp = new ArrayList<IGridNode>();
		for (Map<String, Object> item : ex.result) {

			DefaultGridNode gridNode = new DefaultGridNode();
			gridNode.getAttrs().putAll(item);
			
			if( pointedFileds ) {
				for(String label : item.keySet()) {
	            	String code = (String) EasyUtils.checkNull(ex.labelsMap.get(label), label);
	            	gridNode.getAttrs().put(code, item.get(label));
	            }
			}

			temp.add(gridNode);
		}

		Document gridTemplate = pointedFileds ? ex.getGridTemplate(_db.cnm, _db.ctype, _db.cpattern) : _db.getGridTemplate();
		GridDataEncoder gEncoder = new GridDataEncoder(temp, gridTemplate);

		PageInfo pageInfo = new PageInfo();
		pageInfo.setPageSize(PAGE_SIZE);
		pageInfo.setTotalRows(ex.count);
		pageInfo.setPageNum(page);

		print(new String[] { "RecordData", "PageInfo" }, new Object[] { gEncoder, pageInfo });
	}

	private SQLExcutor queryRecordData(_Database _db, int page, int pagesize, Map<String, String> requestMap, boolean pointedFileds, boolean isTssGrid) {
		SQLExcutor ex;
		boolean isWFQuery = requestMap.remove("my_wf_list") != null;
		if (isWFQuery) {
			ex = wfService.queryMyTasks(_db, requestMap, page, pagesize);
		} else {
			ex = _db.select(page, pagesize, requestMap);

			/* 添加工作流信息 */
			wfService.fixWFStatus(_db, ex.result);
		}

		if (pointedFileds && !ex.selectFields.contains("id")) {
			return ex;
		}

		List<Long> itemIDs = new ArrayList<Long>();
		for (Map<String, Object> item : ex.result) {
			itemIDs.add((Long) item.get("id"));
		}

		Map<String, String> usersMap = loginSerivce.getUsersMap(Environment.getDomain());

		/* 读取记录的附件信息 */
		Map<Object, Object> itemAttach = new HashMap<Object, Object>();
		if (_db.needFile && itemIDs.size() > 0) {
			String sql = "select itemId item, group_concat(id ORDER BY seqNo ASC) ids from dm_record_attach " + " where recordId = ? and itemId in ("
					+ EasyUtils.list2Str(itemIDs) + ") " + " group by itemId";
			List<Map<String, Object>> attachResult = SQLExcutor.queryL(sql, _db.recordId);
			for (Map<String, Object> temp : attachResult) {
				itemAttach.put(temp.get("item").toString(), temp.get("ids"));
			}
		}
		for (Map<String, Object> item : ex.result) {
			// 把附件字段替换为链接
			int index = 0;
			for (String field : _db.fieldCodes) {
				boolean isFileField = _Field.TYPE_FILE.equals(_db.fieldTypes.get(index++));
				if (isFileField) {
					// eg: logo.png#12,logo2.png#13
					String[] values = EasyUtils.obj2String(item.get(field)).split(",");
					String urls = "";
					List<String> ids = new ArrayList<>();
					for (String value : values) {
						int splitIndex = value.indexOf("#"); 
						if (splitIndex < 0)
							continue;

						String name = value.substring(0, splitIndex);
						String id = value.substring(splitIndex + 1);
						String downloadUrl = "/tss/xdata/attach/download/" + id;
						urls += "<a href='" + downloadUrl + "' target='_blank'>" + name + "</a>&nbsp&nbsp";
						ids.add(id);
					}

					item.put(field, urls);
					item.put(field + "_ids", EasyUtils.list2Str(ids));
				}
			}

			Object itemId = item.get("id").toString();
			String fileIds = (String) itemAttach.get(itemId);
			int fileCount = fileIds == null ? 0 : fileIds.split(",").length;

			if (isTssGrid || requestMap.containsKey("showAttach")) {
				String onclick = "manageAttach(" + itemId + ", " + _db.recordId + ")";
				Object attachTag = fileCount > 0 ? fileCount : (isWFQuery ? "0" : "上传");
				item.put("fileNum", "<a href='javascript:void(0)' onclick='" + onclick + "'>" + attachTag + "</a>");
				item.put("fileIds", EasyUtils.obj2String(fileIds));
			}
			item.put("_fileNum", EasyUtils.obj2Int(fileCount));

			Object account = item.get("creator");
			item.put("creatorName", EasyUtils.checkNull(usersMap.get(account), account));
		}

		return ex;
	}

	@RequestMapping("/json/{record}/{page}")
	@ResponseBody
	public Object showAsJSON(HttpServletRequest request, @PathVariable("record") Object record, @PathVariable("page") int page) {

		// 如果同时传递了录入表名，优先用之（适合中文名表名）
		String recordName = request.getParameter("recordName");
		if (!EasyUtils.isNullOrEmpty(recordName)) {
			record = recordName;
		}
		Long recordId = recordService.getRecordID(record, false);

		Map<String, String> requestMap = prepareParams(request, recordId);
		boolean pointedFileds = requestMap.containsKey("fields");
		int _pagesize = getPageSize(requestMap, PAGE_SIZE * 20);

		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_VDATA, Record.OPERATION_EDATA);

		// 默认模糊查询（EasyUI网页上需要模糊查询）
		String strictQuery = (String) EasyUtils.checkNull(requestMap.get(_Field.STRICT_QUERY), "false");
		requestMap.put(_Field.STRICT_QUERY, strictQuery);

		SQLExcutor ex = queryRecordData(_db, page, _pagesize, requestMap, pointedFileds, false);

		if (requestMap.containsKey("debugSQL")) { // 调试SQL解析
			return ex.sql.replaceAll("\n", " ");
		}

		if (requestMap.containsKey("rows")) { // for EasyUI
			return ex.toEasyUIGrid();
		}

		return ex.result;
	}

	/** EasyUI 翻页调用此处接口 */
	@RequestMapping("/json/{record}")
	@ResponseBody
	public Object showAsJSON(HttpServletRequest request, @PathVariable("record") String record) {

		Object page = EasyUtils.checkNull(request.getParameter("page"), 1);
		return showAsJSON(request, record, EasyUtils.obj2Int(page));
	}

	private int getPageSize(Map<String, String> m, int defaultSize) {
		Object pagesize = EasyUtils.checkNull(m.get("pagesize"), m.get("rows"), defaultSize);
		return EasyUtils.obj2Int(pagesize);
	}

	@RequestMapping("/export/{record}")
	public void export(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record) {
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, true);  // GET
																					// Method
																					// Request
		boolean pointed = requestMap.containsKey("fields");
		requestMap.put("export", "true");

		Long recordId = recordService.getRecordID(record, true);
		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_VDATA);

		int _page = EasyUtils.obj2Int(EasyUtils.checkNull(requestMap.get("page"), "1"));
		int _pagesize = getPageSize(requestMap, 10 * 10000);

		// 默认模糊查询
		String strictQuery = (String) EasyUtils.checkNull(requestMap.get(_Field.STRICT_QUERY), "false");
		requestMap.put(_Field.STRICT_QUERY, strictQuery);

		SQLExcutor ex;
		boolean isWFQuery = "true".equals(requestMap.remove("my_wf_list"));
		if (isWFQuery && !pointed) {
			ex = wfService.queryMyTasks(_db, requestMap, _page, _pagesize);
		} else {
			ex = _db.select(_page, _pagesize, requestMap);

			/* 添加工作流信息 */
			wfService.fixWFStatus(_db, ex.result);
		}
		List<Map<String, Object>> result = ex.result;

		String fileName = _db.recordName + DateUtil.format(new Date()).replaceAll("-", "") + Environment.getUserId() + ".csv";
		for (Map<String, Object> row : result) { // 剔除
			row.remove("domain");
			row.remove("version");
			row.remove("id");
			row.remove("updatetime");
			row.remove("updator");

			if (!_db.showCreator) {
				row.remove("createtime");
				row.remove("creator");
			}
			row.remove("lastProcessor");
		}

		// 过滤出用户可见的表头列
		List<String> visiableFieldNames = _db.getVisiableFields(true, pointed ? ex.selectFields : _db.fieldCodes);
		if (_db.showCreator && !pointed) {
			visiableFieldNames.add("创建时间");
			visiableFieldNames.add("创建人");
		}
		if (WFUtil.checkWorkFlow(_db.wfDefine) && !pointed) {
			visiableFieldNames.add("流程状态");
			visiableFieldNames.add("发起人");
			visiableFieldNames.add("发起时间");
			visiableFieldNames.add("审批人列表");
			visiableFieldNames.add("当前审批人");
			visiableFieldNames.add("抄送");
		}

		String charSet = (String) EasyUtils.checkNull(request.getParameter(DataExport.CHARSET), DataExport.SYS_CHAR_SET);
		String exportPath = DataExport.exportCSV(fileName, result, visiableFieldNames, charSet);
		DataExport.downloadFileByHttp(response, exportPath, charSet, false);
	}

	@RequestMapping(value = "/{record}/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> get(HttpServletRequest request, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String,String> params = prepareParams(request, recordId);
		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_VDATA, Record.OPERATION_EDATA);

		Map<String, Object> result = _db.get(id);
		if (result == null) {
			throw new BusinessException(EX.parse(EX.DM_13B, id));
		}
		
		if( !checkRowVisible(recordId, id, params) ) {
			throw new BusinessException(EX.DM_08);
		}

		/* 添加附件信息 */
		result.put("attachList", recordService.getAttachList(recordId, id));

		/* 添加工作流信息 */
		wfService.appendWFInfo(_db, result, id);
		
		// 添加流程提醒，由发起人提醒审批人（站内信）
		result.put("wf_msgs", commService.getList("from Message where workflow=?1 and workflowItem=?2 order by id", recordId, id));

		result.put("id", id);
		return result;
	}

	@RequestMapping(value = "/{record}", method = RequestMethod.POST)
	public void create(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record) {

		Object newID = createAndReturnID(request, record);
		printSuccessMessage(String.valueOf(newID));
	}

	@RequestMapping(value = "/rid/{record}", method = RequestMethod.POST)
	@ResponseBody
	public Long createAndReturnID(HttpServletRequest request, @PathVariable("record") Object record) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);
		Long _tempID = EasyUtils.obj2Long(requestMap.remove("_tempID"));
		boolean isDraft = Config.TRUE.equalsIgnoreCase(requestMap.remove("saveDraft"));

		_Database _db = getDB(recordId, Record.OPERATION_CDATA);
		Long newID = null;
		try {
			newID = _db.insertRID(requestMap);

			// fix 用_tempID上传的附件
			String attachSeqNos = requestMap.remove("remainAttachs"); // 提交记录时还存在的附件序号，有些上传错的附件可能已经删除了
			fixAttachs(recordId, _tempID, newID, attachSeqNos);

			// 新增时带附加操作，使用了自定义操作来支持多表数据操作；修改、删除不带附件操作，直接用MultiSQLExcutor执行即可
			exeAfterOperation(requestMap, _db, newID);

			// 计算并初始化流程
			if (!isDraft) {
				wfService.calculateWFStatus(newID, _db);
			}
		} catch (Exception e) {
			_db.delete(newID); // 回滚
			throwEx(e, _db + " create ");
		}
		return newID;
	}
	
	public static void fixAttachs(Long recordId, Long _tempID, Long newID, String attachSeqNos) {
		File attachDir1 = new File(RecordAttach.getAttachDir(recordId, _tempID));
		if ( attachDir1.exists() ) { // 将挂在临时记录ID下的附件挂到新生成的记录ID上
			String fixSql = "update dm_record_attach set itemId=? where recordId=? and itemId=?";
			if (!EasyUtils.isNullOrEmpty(attachSeqNos)) {
				fixSql += " and seqNo in (" + attachSeqNos + ") "; // 不在attachSeqNos里的附件成为废弃记录，可写一个定时Job清理这类记录
			}

			Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
			paramsMap.put(1, newID);
			paramsMap.put(2, recordId);
			paramsMap.put(3, _tempID);
			SQLExcutor.excute(fixSql, paramsMap, DMConstants.LOCAL_CONN_POOL);
			
			log.debug( fixSql + paramsMap );

			File attachDir2 = new File(RecordAttach.getAttachDir(recordId, newID));
			attachDir1.renameTo(attachDir2);
		}
	}

	/**
	 * 
	 *  var params = {....};
		var json = [ { "sqlCode": "after_xxx_insert" } ];
		params._after_ = JSON.stringify(json);
		tssJS.post("/tss/xdata/xxx", params, function(result) { console.log(result); } );
	
	 * 注：_after_ data参数名不能有和requestMap里重复的，不然用不上，优先被fmParse掉了
	 */
	protected void exeAfterOperation(Map<String, String> requestMap, _Database _db, Long itemId) throws Exception {
		String afterOption = requestMap.remove("_after_");
		if (!EasyUtils.isNullOrEmpty(afterOption)) {
			Map<String, Object> data = _db.get(itemId);
			data.put("id", itemId);

			for (String key : requestMap.keySet()) {
				if (!data.containsKey(key)) {
					data.put(key, requestMap.get(key));
				}
			}

			afterOption = EasyUtils.fmParse(afterOption, data);
			MultiSQLExcutor mex = new MultiSQLExcutor();
			mex.recordService = this.recordService;
			mex._exeMultiSQLs(afterOption, _db.datasource, data);
		}
	}

	private void throwEx(Exception e, String op) {
		Throwable firstCause = ExceptionEncoder.getFirstCause(e);
		String errorMsg = op + " error: " + firstCause;
		errorMsg = errorMsg.replaceAll("com.boubei.tss.framework.exception.BusinessException: ", "");

		ExceptionEncoder.logException(errorMsg, e);
		throw new BusinessException(errorMsg);
	}
	
	
	// 克隆一条记录，包括附件；注：要求不能有唯一性约束字段
	@RequestMapping(value = "/clone/{record}/{id}", method = RequestMethod.POST)
	@ResponseBody
	public Long clone(HttpServletRequest request, @PathVariable("record") Object record, @PathVariable("id") Long id) {
		
		Long recordId = recordService.getRecordID(record, false);
		Map<String,String> params = prepareParams(request, recordId);
		checkRowVisible(recordId, id, params);

		_Database _db = getDB(recordId, Record.OPERATION_CDATA);
		Map<String, Object> old = _db.get(id);
		
		Map<String, String> valuesMap = new HashMap<>();
		for(String key : old.keySet()) {
			valuesMap.put(key, EasyUtils.obj2String(old.get(key)));
		}
		Long newID = _db.insertRID(valuesMap);
		
		List<?> list = recordService.getAttachList(recordId, id);
		for(Object o : list) {
			RecordAttach ra1 = (RecordAttach) o;
			RecordAttach ra2 = new RecordAttach();
			BeanUtil.copy(ra2, ra1, "id,itemId".split(","));
			ra2.setItemId(newID);
			commService.create(ra2);
			
			// 复制附件
			FileHelper.copyFile( new File(ra2.getAttachDir()), new File(ra1.getAttachPath()), true, false );
		}
		
		return newID;
	}

	@RequestMapping(value = "/{record}/{id}", method = RequestMethod.POST)
	public void update(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);
		boolean isDraft = Config.TRUE.equalsIgnoreCase(requestMap.remove("saveDraft"));

		// 检查用户对当前记录是否有编辑权限，防止篡改别人创建的记录
		checkRowEditable(recordId, id,  requestMap);

		_Database _db = getDB(recordId);
		Map<String, Object> old = _db.get(id);
		try {
			_db.update(id, requestMap);
		} catch (Exception e) {
			throwEx(e, _db + " update ");
		}

		try {
			exeAfterOperation(requestMap, _db, id);

			// 计算并初始化流程
			if (!isDraft) {
				wfService.calculateWFStatus(id, _db);
			}

			printSuccessMessage();
		} 
		catch (Exception e) {
			_db.rollback(id, old); // 回滚
			throwEx(e, _db + " update after ");
		}
	}

	/**
	 * 批量更新选中记录行的某个字段值，用在批量审批等场景: $.post("/tss/xdata/batch/13", {'ids':
	 * '1,2,3,4', 'field': 'brand', 'value': '农夫'} , function(data) {});
	 */
	@RequestMapping(value = "/batch/{record}", method = RequestMethod.POST)
	public void updateBatch(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, String ids,
			String field, String value) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		String _value = requestMap.get("value");
		value = (String) EasyUtils.checkNull(_value, value);

		// 检查用户对当前记录是否有编辑权限，防止篡改别人创建的记录
		if (!checkPermission(recordId, Record.OPERATION_EDATA) && !checkPermission(recordId, Record.OPERATION_CDATA)) {
			throw new BusinessException(EX.DM_12);
		}

		boolean addVersion = !"false".equals(requestMap.get("addVersion"));
		getDB(recordId).updateBatch(ids, field, value, addVersion);
		printSuccessMessage();
	}

	@RequestMapping(value = "/{record}/{id}", method = RequestMethod.PUT)
	public void restore(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String,String> params = prepareParams(request, recordId);

		checkRowEditable(recordId, id, params);
		_Database db = getDB(recordId);
		db.restore(id);

		printSuccessMessage();
	}

	@RequestMapping(value = "/{record}/{id}", method = RequestMethod.DELETE)
	public void delete(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		exeDelete(recordId, id, requestMap);
		printSuccessMessage();
	}

	private void exeDelete(Long recordId, Long id, Map<String, String> requestMap) {
		// 检查用户对当前记录是否有编辑权限
		checkRowEditable(recordId, id, requestMap);

		_Database db = getDB(recordId);

		Map<String, Object> item = db.get(id);
		String domain = EasyUtils.obj2String(item.get("domain")); // 已逻辑删除的再次删除，则直接删除
		boolean isRecycled = domain.endsWith(_Database.deletedTag);

		// 判断是逻辑删除还是物理删除（系统级、单个表、单次请求）
		boolean loginDel = db.isLogicDelete();
		if (loginDel && !isRecycled) {
			db.logicDelete(id);
		} 
		else { // 物理删除
			db.delete(id);

			wfService.removeWFStatus(recordId, id, item);

			// 删除附件
			List<?> attachs = recordService.getAttachList(recordId, id);
			for (Object obj : attachs) {
				RecordAttach attach = (RecordAttach) obj;
				recordService.deleteAttach(attach.getId());
				FileHelper.deleteFile(new File(attach.getAttachPath()));
			}
		}
	}

	// for 不支持method=DELETE的客户端，微信小程序等
	@RequestMapping(value = "/batch/{record}/del", method = RequestMethod.POST)
	public void deleteBatchII(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, String ids) {
		deleteBatch(request, response, record, ids);
	}

	@RequestMapping(value = "/batch/{record}", method = RequestMethod.DELETE)
	public void deleteBatch(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, String ids) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		ids = EasyUtils.obj2String(ids);
		String[] idArray = ids.split(",");
		for (String id : idArray) {
			if( EasyUtils.isNullOrEmpty(id) ) continue;
			exeDelete(recordId, EasyUtils.obj2Long(id), requestMap);
		}
		printSuccessMessage();
	}

	/**
	 * 批量新增、修改、删除，All in one。
	 * 
	 * @param request
	 * @param recordId
	 * @param csv
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/cud/{record}", method = RequestMethod.POST)
	@ResponseBody
	public Object cudBatch(HttpServletRequest request, @PathVariable("record") Object record) throws Exception {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);
		String csv = requestMap.get("csv");

		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_EDATA);

		String[] rows = EasyUtils.split(csv, "\n");
		List<Map<String, String>> insertList = new ArrayList<Map<String, String>>();
		int updateCount = 0, deleteCount = 0;

		String[] headers = rows[0].split(",");
		for (int index = 1; index < rows.length; index++) { // 第一行为表头，不要
			String row = rows[index];
			String[] values = (row + " ").split(","); // 加个空格防止一组逗号结尾，被split忽略:'1,2,,,'.split(,) = [1,2]

			Map<String, String> item = new HashMap<String, String>();
			for (int j = 0; j < values.length; j++) {
				item.put(headers[j], values[j].trim());
			}

			String _itemID = item.get("id");
			if (EasyUtils.isNullOrEmpty(_itemID)) {
				insertList.add(item);
			} else {
				Long itemID = EasyUtils.obj2Long(_itemID);
				if (row.replaceAll(",", "").trim().equals(_itemID.trim())) { // 除了ID其它都为空
					exeDelete(recordId, itemID, requestMap);
					deleteCount++;
				} else {
					checkRowEditable(recordId, itemID, requestMap);
					_db.update(itemID, item);
					updateCount++;
				}
			}
		}
		_db.insertBatch(insertList); // 所有新增是一个事务的，但删除和修改不在一个事务内

		exeAfterOperation(requestMap, _db, null);

		Map<String, Object> rtMap = new HashMap<String, Object>();
		rtMap.put("created", insertList.size());
		rtMap.put("updated", updateCount);
		rtMap.put("deleted", deleteCount);
		return rtMap;
	}

	@RequestMapping(value = "/cud/json/{record}", method = RequestMethod.POST)
	@ResponseBody
	public Object cudBatchJSON(HttpServletRequest request, @PathVariable("record") Object record) throws Exception {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);
		String json = requestMap.get("json");

		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_EDATA);

		List<Map<String, Object>> list = EasyUtils.json2List(json);

		List<Map<String, String>> insertList = new ArrayList<Map<String, String>>();
		int updateCount = 0, deleteCount = 0;

		for (Map<String, Object> row : list) {

			Map<String, String> item = new HashMap<String, String>();
			List<String> codes = new ArrayList<String>(_db.fieldCodes);
			codes.add("_version");
			for (String field : codes) {
				if (row.containsKey(field)) {
					Object value = row.get(field);
					item.put(field, EasyUtils.obj2String(value));
				}
			}

			if (!row.containsKey("id")) {
				insertList.add(item);
			} else {
				Long itemID = EasyUtils.obj2Long(row.get("id"));
				if (item.isEmpty()) { // 只有ID
					exeDelete(recordId, itemID, requestMap);
					deleteCount++;
				} else {
					checkRowEditable(recordId, itemID, requestMap);
					_db.update(itemID, item);
					updateCount++;
				}
			}
		}
		_db.insertBatch(insertList); // 所有新增是一个事务的，但删除和修改不在一个事务内

		exeAfterOperation(requestMap, _db, null);

		Map<String, Object> rtMap = new HashMap<String, Object>();
		rtMap.put("created", insertList.size());
		rtMap.put("updated", updateCount);
		rtMap.put("deleted", deleteCount);
		return rtMap;
	}

	/************************************* record workflow **************************************/

	// 批量提交草稿状态流程申请
	@RequestMapping(value = "/apply/{record}/batch", method = RequestMethod.POST)
	public void apply(HttpServletResponse response, @PathVariable("record") Object record, String ids) {

		Long recordId = recordService.getRecordID(record, false);
		_Database _db = recordService.getDB(recordId);

		String[] idArray = ids.trim().split(",");
		int count = 0;
		for (String id : idArray) {
			Long itemId = EasyUtils.obj2Long(id);
			WFStatus wfStatus = wfService.getWFStatus(recordId, itemId);
			if (wfStatus == null) {
				wfService.calculateWFStatus(itemId, _db);
				count++;
			}
		}
		printJSON("成功提交" + count + "条" + _db.recordName + "申请");
	}

	// 审核
	@RequestMapping(value = "/approve/{record}/{id}", method = RequestMethod.POST)
	public void approve(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		String opinion = requestMap.remove("opinion");

		// 审批时允许审批者补充填写 wf_ 打头的数据表字段
		if (requestMap.size() > 0) {
			_Database db = recordService.getDB(recordId);
			db.update(id, requestMap);
		}

		String msg = "审批成功";
		String wfStatus = wfService.approve(recordId, id, opinion);
		if (WFStatus.PASSED.equals(wfStatus)) {
			msg = "审批通过";
		}

		printJSON(msg);
	}

	// 驳回
	@RequestMapping(value = "/reject/{record}/{id}", method = RequestMethod.POST)
	public void reject(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		wfService.reject(recordId, id, requestMap.get("opinion"));

		printJSON("驳回成功");
	}

	// 重新发起
	@RequestMapping(value = "/reapply/{record}/{id}", method = RequestMethod.POST)
	public void reApply(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		wfService.reApply(recordId, id, requestMap.get("opinion"));

		printJSON("重新发起成功");
	}

	// 转审
	@RequestMapping(value = "/transApprove/{record}/{id}", method = RequestMethod.POST)
	public void transApprove(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record,
			@PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		wfService.transApprove(recordId, id, requestMap.get("opinion"), requestMap.get("target"));

		printJSON("转审成功");
	}

	@RequestMapping(value = "/translist/{record}/{id}")
	@ResponseBody
	public List<?> transList(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record,
			@PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		prepareParams(request, recordId);

		return wfService.getTransList(recordId, id);
	}

	@RequestMapping(value = "/translist/xml/{record}/{id}")
	public void transListXML(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record,
			@PathVariable("id") Long id) {

		@SuppressWarnings("unchecked")
		List<Map<String, String>> users = (List<Map<String, String>>) transList(request, response, record, id);

		List<ITreeNode> nodes = new ArrayList<ITreeNode>();
		for (Map<String, String> m : users) {
			nodes.add(new DefaultTreeNode(m.get("usercode"), m.get("username")));
		}

		TreeEncoder treeEncoder = new TreeEncoder(nodes);
		treeEncoder.setNeedRootNode(false);
		print("UserTree", treeEncoder);
	}

	// 撤销
	@RequestMapping(value = "/cancel/{record}/{id}", method = RequestMethod.POST)
	public void cancel(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record, @PathVariable("id") Long id) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> requestMap = prepareParams(request, recordId);

		wfService.cancel(recordId, id, requestMap.get("opinion"));

		printJSON("撤销成功");
	}
	
    @RequestMapping(value = "/wfmsg", method = RequestMethod.POST)
    @ResponseBody
    public void sendWFMessage(HttpServletRequest request, Long wfId, Long itemId) {
    	Map<String, String> requestMap = prepareParams(request, wfId);
    	
    	String wfMsg = requestMap.get("wfMsg");
    	if( EasyUtils.isNullOrEmpty(wfMsg) || !checkRowVisible(wfId, itemId, requestMap) ) return;
    	
    	Message temp = new Message();
		temp.setContent( wfMsg );
		temp.setWorkflow(wfId);
		temp.setWorkflowItem(itemId);
		
        commService.create(temp);
    }

	/************************************* record batch import **************************************/

	/**
	 * 将前台（一般为生成好的table数据）数据导出成CSV格式
	 * http://localhost:9000/auth/xdata/import/tl/xx
	 */
	@RequestMapping("/import/tl/{record}")
	public void getImportTL(HttpServletResponse response, @PathVariable("record") Object record) {

		Long recordId = recordService.getRecordID(record, false);
		_Database _db = getDB(recordId, Record.OPERATION_CDATA, Record.OPERATION_EDATA, Record.OPERATION_VDATA);

		List<String> columns = new ArrayList<String>();
		String fieldNames = DMUtil.getExtendAttr(_db.remark, DMConstants.IMPORT_TL_FIELDS); // 允许在录入表备注里配置导入模板的列
		if (fieldNames != null) {
			fieldNames = StringUtil.fixSplit(fieldNames, ",");
			columns.addAll(Arrays.asList(fieldNames.split(",")));
		} else {
			for( String fieldName : _db.fieldNames ) {
				String code = _db.ncm.get(fieldName);
				String type = _db.ctype.get(code);
				if( !_Field.TYPE_HIDDEN.equals(type) ) {
					columns.add(fieldName);
				}
			}
		}

		String fieldIgnores = DMUtil.getExtendAttr(_db.remark, DMConstants.IMPORT_TL_IGNORES); // 可以是label也可以是code
		if (fieldIgnores != null) {
			String[] _fieldIgnores = StringUtil.fixSplit(fieldIgnores, ",").split(",");
			for (String ignore : _fieldIgnores) {
				columns.remove(ignore);
				columns.remove(_db.cnm.get(ignore));
			}
		}

		/* 1、不带任何样式的Excel模板 
		 * int index = 0; 
		 * for(String fname : columns) { 
		 * 		String fcode = _db.ncm.get(fname); 
		 * 		boolean notnull = "false".equals(_db.cnull.get(fcode)); 
		 * 		if( notnull ) {
		 * 			columns.set(index, "*" +fname+ "*"); 
		 * 		} 
		 * 		index++; 
		 * }
		 * 
		 * String exportPath = DataExport.getExportPath() + "/" + _db.recordName + "-tl.csv";
		 * DataExport.exportCSV(exportPath, EasyUtils.list2Str(columns));
		 */

		/* 2、带样式的Excel模板 */
		String exportPath = DataExport.getExportPath() + "/" + _db.recordName + "-导入模板" + Excel.XLSX_FIX;
		FileHelper.createFile(exportPath);
		
		Workbook wb = null;
		FileOutputStream ios = null;
		try {
			wb = new SXSSFWorkbook(5000);
			Sheet ws = wb.createSheet(_db.recordName);
			Row row = ws.createRow(0);
			row.setHeightInPoints(22);

			int j = 0;
			for (String fname : columns) {
				String fcode = _db.ncm.get(fname);

				Font font = wb.createFont();

				boolean notnull = "false".equals(_db.cnull.get(fcode));
				if (notnull) {
					fname = "*" + fname + "*";
					font.setColor(IndexedColors.RED.getIndex()); // 必填字段为红色 
				}

				Cell cell = row.createCell(j++, CellType.STRING);
				cell.setCellValue(fname);

				XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
				style.setFont(font);
				cell.setCellStyle(style);
				
				// 配置了options的列生成：下拉列表
				String options = _db.coptions.get(fcode);
				if( options != null ) {
					CellRangeAddressList cra = new CellRangeAddressList(1, 65535, j-1, j-1);
					DataValidationHelper helper = ws.getDataValidationHelper();
			        DataValidationConstraint constraint = helper.createExplicitListConstraint(options.split("\\|"));
			        DataValidation dataValidation = helper.createValidation(constraint, cra);
			        dataValidation.setSuppressDropDownArrow(true);
			        dataValidation.setShowErrorBox(true);
			        dataValidation.setEmptyCellAllowed(true);
			        dataValidation.setShowPromptBox(true);
			        dataValidation.createPromptBox("提示", "只能选择下拉框里面的数据");
			        ws.addValidationData(dataValidation);
				}
			}
			wb.write(ios = new FileOutputStream(exportPath));
			
		} catch (Exception e) {
//			log.error(e.getMessage(), e.getCause());
		} finally {
			try { wb.close();  } catch (Exception e) { }
			try { ios.close(); } catch (Exception e) { }
		}

		DataExport.downloadFileByHttp(response, exportPath);
	}

	/************************************* record attach operation **************************************/
	
	@RequestMapping("/attach/xml/{record}/{itemId}")
	public void getAttachListXML(HttpServletRequest request, HttpServletResponse response, @PathVariable("record") Object record,
			@PathVariable("itemId") Long itemId) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String,String> params = prepareParams(request, recordId);

		// 检查用户对当前记录是否有查看权限
		if (!checkRowVisible(recordId, itemId, params)) {
			throw new BusinessException(EX.DM_08);
		}

		List<?> list = recordService.getAttachList(recordId, itemId);
		GridDataEncoder attachGrid = new GridDataEncoder(list, DMConstants.GRID_RECORD_ATTACH);
		print("RecordAttach", attachGrid);
	}

	@RequestMapping("/attach/json/{record}/{itemId}")
	@ResponseBody
	public List<?> getAttachList(HttpServletRequest request, @PathVariable("record") Object record, @PathVariable("itemId") Long itemId) {

		Long recordId = recordService.getRecordID(record, false);
		Map<String, String> params = prepareParams(request, recordId);
		
		if( itemId == 0 ) { // 支持自定义类通过其他查询条件获取ItemId，及校验权限，eg:  /attach/json/-3/0
			itemId = _DataItemChecker.getInstance().queryItemId(recordId, params);
		}

		// 检查用户对当前记录是否有查看权限
		if (!checkRowVisible(recordId, itemId, params)) {
			throw new BusinessException(EX.DM_08);
		}

		return recordService.getAttachList(recordId, itemId);
	}

	@RequestMapping(value = "/attach/{id}", method = RequestMethod.DELETE)
	public void deleteAttach(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") Long id) {
		RecordAttach attach = recordService.getAttach(id);
		if (attach == null) {
			throw new BusinessException(EX.DM_06);
		}
		Long recordId = attach.getRecordId();

		Map<String, String> params = prepareParams(request, recordId); // 远程访问预登录

		// 检查用户对当前附件所属记录是否有编辑权限
		checkRowEditable(recordId, attach.getItemId(), params);

		recordService.deleteAttach(id);
		
		// 检查是否还有其它记录应用到当前附件（clone后的记录引用的是同一附件）
		FileHelper.deleteFile(new File(attach.getAttachPath()));

		// 记录附件删除日志，关联到附件所属的录入表 
		BusinessLogger.log(getRecordName(recordId), "删除附件, " + id, attach);

		printSuccessMessage();
	}
	
	private String getRecordName(Long recordId) {
		String recordName = recordId.toString(); // maybe recordId < 0, 虚拟的录入表
		try {
			recordName = recordService.getRecord(recordId).getName();
		} catch(Exception e) { }
		
		return recordName;
	}

	// for 不支持method=DELETE的客户端，微信小程序等
	@RequestMapping(value = "/attach/del/{id}", method = RequestMethod.POST)
	public void deleteAttach4WX(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") Long id) {
		deleteAttach(request, response, id);
	}

	@RequestMapping("/attach/download/{id}")
	public void downloadAttach(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable("id") Long id, String smaller) throws IOException {

		RecordAttach attach = recordService.getAttach(id);
		if (attach == null) {
			throw new BusinessException(EX.DM_06);
		}
		Long recordId = attach.getRecordId();

		Map<String,String> params = prepareParams(request, recordId); // 远程访问预登录

		// 检查用户对当前附件所属记录是否有查看权限
		if ( !checkRowVisible( recordId, attach.getItemId(), params) ) {
			throw new BusinessException(EX.DM_07);
		}
		
		String attachPath = attach.getAttachPath();
		
		// 是否用缩略图
		if( "true".equals( smaller ) ) {
			attachPath = Imager.markSLPic(new File(attachPath), 64); // 64k
		}

		FileHelper.downloadFile(response, attachPath, attach.getName());
		HitRateManager.getInstanse("dm_record_attach").output(id); // 更新浏览次数
		
		String rcName = getRecordName(recordId);
		AccessLogRecorder.outputAccessLog("record-" + recordId, "下载附件", rcName + "_" + id, new HashMap<String, String>(),
				System.currentTimeMillis());
	}
	
	@RequestMapping("/attach/download/smaller/{id}")
	public void downloadPicSL(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") Long id) throws IOException {
		downloadAttach(request, response, id, "true");
	}

	@RequestMapping(value = "/attach/top/{id}", method = RequestMethod.POST)
	@ResponseBody
	public RecordAttach setTopAttach(@PathVariable("id") Long id) {
		RecordAttach attach = recordService.getAttach(id);
		Long recordId = attach.getRecordId();

		// 检查用户对当前附件所属记录是否有编辑权限
		Long itemId = attach.getItemId();
		checkRowEditable(recordId, itemId, new HashMap<String,String>());

		attach.setSeqNo(1);
		commService.update(attach);

		List<?> list = recordService.getAttachList(recordId, itemId);
		int index = 2;
		for (Object obj : list) {
			RecordAttach _attach = (RecordAttach) obj;
			if (!_attach.getId().equals(id)) {
				_attach.setSeqNo(index++);
				commService.update(_attach);
			}
		}

		return attach;
	}
	
	/** 附件排序 */
	@RequestMapping(value = "/attach/sort", method = RequestMethod.POST)
	@ResponseBody
	public Object sortAttach(String ids) {
        List<?> list = commService.getList("from RecordAttach where id in (" +ids+ ")");
        Map<Long, RecordAttach> m = new HashMap<>();
        for(Object o : list) {
        	RecordAttach ra = (RecordAttach) o;
        	m.put(ra.getId(), ra);
        }
        
        String[] _ids = ids.split(",");
        for(int i = 0; i < _ids.length; i++) {
        	Long id = EasyUtils.obj2Long(_ids[i]);
        	RecordAttach ra = m.get(id);
        	if(ra == null) continue;
        	
        	checkRowEditable(ra.getRecordId(), ra.getItemId(), new HashMap<String,String>());
        	
        	ra.setSeqNo(i+1);
        	commService.updateWithLog(ra);
        }
        
        return "success";
	}

	/************************************* check permissions：安全级别 >= 6 才启用 **************************************/

	public static boolean checkPermission(Long recordId, String permitOption) {
		if (!SecurityUtil.isHardMode() || recordId < 0)
			return true;

		PermissionHelper helper = PermissionHelper.getInstance();
		String permissionTable = RecordPermission.class.getName();
		return helper.checkPermission(recordId, permissionTable, RecordResource.class, permitOption);
	}

	/**
	 * 检查用户对当前记录是否有编辑权限，防止篡改别人创建的记录
	 * 
	 * @param recordId
	 * @param itemId
	 */
	private void checkRowEditable(Long recordId, Long itemId, Map<String, ?> params) {
		if ( EasyUtils.isTimestamp(itemId) ) { // 临时ID（即新增尚未保存时上传的附件）无需校验
			return;
		}
		
		// 非标准录入表，默认允许编辑（附件）; 控制权限需单独定制 安全校验类
		_DataItemChecker.getInstance().editable(recordId, itemId, params); 
		
		if( recordId < 0 || !SecurityUtil.isHardMode() ) { 
			return; 
		}

		// 先检查流程是否存在且是否已开始处理
		List<String> statusList = new ArrayList<String>();
		statusList.add(WFStatus.NEW);         // 待审批允许继续修改
		statusList.add(WFStatus.REMOVED);     // 还原
		statusList.add(WFStatus.AUTO_PASSED); // 自动通过的允许修改

		WFStatus wfStatus = wfService.getWFStatus(recordId, itemId);
		String userCode = Environment.getUserCode();
		if (wfStatus != null && !(statusList.contains(wfStatus.getCurrentStatus()) && userCode.equals(wfStatus.getApplier()))) {
			throw new BusinessException(EX.WF_1);
		}

		boolean flag = false;
		if (checkPermission(recordId, Record.OPERATION_EDATA)) {
			flag = checkRowVisible(recordId, itemId, params); // 如果有【维护数据】权限，则只要可见就能编辑
		}
		if (!flag && checkPermission(recordId, Record.OPERATION_CDATA)) {
			flag = checkRowAuthor(recordId, itemId); // 如果没有【维护数据】只有【新建】权限，则只能编辑自己创建的记录
		}

		if (!flag) {
			throw new BusinessException(EX.parse(EX.DM_05, itemId));
		}
	}

	/**
	 * 因db.select方法里对数据进行了权限过滤（以及按域过滤），所以能按ID查询出来的都是有权限查看的；
	 */
	private boolean checkRowVisible(Long recordId, Long itemId, Map<String,?> params) {
		
		if ( EasyUtils.isTimestamp(itemId) ) { // 临时记录ID，新建时
			return true;
		}

		// 非标准录入表，默认允许查看; 控制权限需单独定制 安全校验类
		_DataItemChecker.getInstance().visiable(recordId, itemId, params); 
		
		if ( recordId < 0 || !SecurityUtil.isHardMode() ) {
			return true;
		}
		
		_Database _db = getDB(recordId);

		WFStatus wf = wfService.getWFStatus(recordId, itemId);
		if (wf != null ) {
			if( _db.makePublic && wf.isPassed() ) {
				return true; // 审核后公开（所有人都可见）
			}
			
			String userCode = Environment.getNotnullUserCode();
			if( wf.processorList().contains(userCode) || userCode.equals(wf.getNextProcessor()) || wf.toCCs().contains(userCode) ) {
				return true; // 所有审批记录对审批人(已审批、待审批、抄送人员)可见
			}
		}

		// 非审批流记录
		Map<String, String> _params = new HashMap<String, String>();
		_params.put("id", EasyUtils.obj2String(itemId));
		SQLExcutor ex = _db.select(1, 1, _params);
		return !ex.result.isEmpty() || checkInRecycleBin(_db, _params);
	}

	private boolean checkRowAuthor(Long recordId, Long itemId) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("id", EasyUtils.obj2String(itemId));
		params.put("creator", Environment.getUserCode());

		_Database _db = getDB(recordId);
		SQLExcutor ex = _db.select(1, 1, params); /* 此处用select不用get，select还会拼上domain条件 ，get不会；用户如果变化了域，将看不到以前域的数据 */
		return !ex.result.isEmpty() || checkInRecycleBin(_db, params);
	}

	private boolean checkInRecycleBin(_Database _db, Map<String, String> params) {
		params.put("domain", EasyUtils.obj2String(Environment.getDomainOrign()) + _Database.deletedTag);

		SQLExcutor ex = _db.select(1, 1, params);
		return !ex.result.isEmpty();
	}
}
