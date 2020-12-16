/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.file;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record._Recorder;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.ProgressPool;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.modules.timer.JobService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

/**
 * var url = URL_UPLOAD_FILE + "?afterUploadClass=com.boubei.tss.dm.record.file.ImportCSV";
   url += "&record=" + rcTable;
   url += "&uniqueCodes=oto,phone";
   url += "&together=false";
   url += "&ignoreExist=true";
   url += "&headerTL=订单号:订单编码|订单编号,货品:sku,数量:qty,类型:{良品},买家:${userCode}; // 模板表头字段映射，适用于第三方系统导出的数据导入至TSS数据表，映射表以外的字段则被忽略不进行导入
    
 * 根据数据表提供的导入模板，填写后导入实现批量录入数据。
 * CSV文件需满足条件：
 * 1、模板从 xdata/import/tl/ 接口导出
 * 2、表头列的字段名需要 和 录入表定义的字段Label严格一致，且不能重复
 * 3、每一行数据的列数及顺序 == 表头的列数及顺序，不能多也不能少
 * 4、每个字段值不允许存在 换行符、英文逗号
 * 5、覆盖式导入，需定义判断规则，支持多个字段（ 定义在数据表页面【全局脚本】里：uniqueCodes="oto,sjphone";）
 */
public class ImportCSV implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	RecordService recordService = (RecordService) Global.getBean("RecordService");
 
	protected List<List<String>> readData(File targetFile, String charSet, String headerTL, List<String> originData, _Database _db) {
		
		String dataStr = FileHelper.readFile(targetFile, charSet); 
		dataStr = dataStr.replaceAll(";", ","); // mac os 下excel另存为csv是用分号;分隔的
		String[] rows = EasyUtils.split(dataStr, "\n");
		
		// 先移除空行
		List<List<String>> rowList = new ArrayList<List<String>>();
		for(String row : rows) {
			if( row.replaceAll(",", "").trim().length() > 0 ) {
				originData.add( row );
				rowList.add( EasyUtils.toList(row) );
			}
		}
		
		List<String> headers = new ArrayList<String>( rowList.get(0) );
		EasyUtils.fixRepeat(headers); // 对重复表头进行处理，自动加2
		
		/* 
		 * 检查是否有表头转换模板（比如将一个三方系统导出数据导入到数据表），格式:
		 * headerTL = 订单号:订单编码|订单编号,货品:sku,数量:qty,类型:状态:{良品},买家:${userCode} 
		 * 注：类型:状态:{良品} 表示 如果Excel没有“状态”列，则取“良品”为默认值
		 */
		if( headerTL != null ) {
			Map<String, String> columnMap = new HashMap<String, String>();
			Map<String, String> adds = new LinkedHashMap<String, String>();
			List<String> lostHeadrs = new ArrayList<String>();
			
			String[] pairs = headerTL.split(",");
			for( String _pair : pairs ) {
				String[] pair = _pair.split(":"); 
				if( pair.length < 2 ) {
					throw new BusinessException("headerTL在【" + _pair + "】附近配置有误");
				}
				
				String tssColumn = pair[0].trim();  // 唯一
				String xxxColumn = pair[1].trim();  // 可以是竖线分隔的多个不同来源
				
				// 新增列：含默认值
				if( xxxColumn.startsWith("{") && xxxColumn.endsWith("}") ) {
					adds.put(tssColumn, xxxColumn.substring(1, xxxColumn.length() -1));
				} 
				else if( xxxColumn.startsWith("${") && xxxColumn.endsWith("}") ) {
					adds.put(tssColumn, DMUtil.fmParse(xxxColumn));
				}
				else { // 映射列
					boolean findHeader = false;
					String[] xxxColumns = xxxColumn.split("\\|");
					for(String _xxxColumn : xxxColumns) {
						columnMap.put(_xxxColumn, tssColumn);
						
						// 如果映射列不在当前导入Excel里，则记录下来，一起抛出异常提醒：缺失xxx/yyy/zzz等列
						if( headers.contains(_xxxColumn) ) {
							findHeader = true;
						}
					}
					
					if( !findHeader ) {
						if( pair.length == 3 ) { // Excel里没有，但headerTL里指定了默认值
							String defaultVal = pair[2].trim();
							adds.put(tssColumn, defaultVal.replaceAll("\\{", "").replaceAll("\\}", ""));
						} else {
							lostHeadrs.add( xxxColumn );
						}
					}
				}
			}
			
			if( lostHeadrs.size() > 0 ) {
				throw new BusinessException("导入文件里数据列" + lostHeadrs + "缺失，请检查.");
			}
			
			int index = 0;
			List<Integer> surplus = new ArrayList<Integer>();
			for(String column : headers) {
				String tssColumn = columnMap.get(column);
				if( tssColumn == null || EasyUtils.isNullOrEmpty(column) ) {
					surplus.add(0, index);
				} else {
					rowList.get(0).set(index, tssColumn);
				}
				index ++;
			}
			
			// 对数据进行处理，如果表头为空，则值也全部置为空；加上表头模板里设置的新增列
			index = 0;
			int columnSize = 0;
			for(List<String> row : rowList) {
				// 删除列
				for(int idx : surplus) { 
					if(idx < row.size())
						row.remove(idx);
				}
				
				// 如果行的数据列大于表头的数据量，则削掉多的列
				if(index == 0) {
					columnSize = row.size();
				}
				if( index > 0 && row.size() > columnSize ) {
					rowList.set(index, row = row.subList(0, columnSize));
				}
				
				// 增加列
				for( String key : adds.keySet() ) {
					row.add( index == 0 ? key : adds.get(key) );
				}
				
				index ++;
			}
		}
		else { // 如果没有自定义hearderTL，则替换里里面默认的必填项
			int index = 0;
			for(String column : headers) {
				if( column.startsWith("*") && column.endsWith("*") ) {
					column = column.substring(1, column.length() - 1 );
					rowList.get(0).set(index, column);
					
					String fCode = _db.ncm.get(column);
					_db.cnull.put(fCode, "false"); // 只要模板头要求必填，则无论字段是否设置必填，都必填
				}
				index ++;
			}
		}
		
		return rowList;
	}

	public String processUploadFile(HttpServletRequest request,
			String filepath, String oldfileName) throws Exception {

		Long start = System.currentTimeMillis();
		String _record = request.getParameter("recordId");
		_record = (String) EasyUtils.checkNull( _record, request.getParameter("record") );
		
		Long recordId = recordService.getRecordID(_record, false);
		Record record = recordService.getRecord(recordId);
		_Database _db = recordService._getDB(recordId);
		
		// 检查录入表是否允许批量导入（设置为手动录入的）; 用户对录入表没有录入权限，则也禁止批量导入
		if( ParamConstants.FALSE.equals( record.getBatchImp() ) ) {
			return "parent.alert('【" +record.getName()+ "】不允许批量导入');"; 
		}
		if( !_Recorder.checkPermission(recordId, Record.OPERATION_CDATA) ) {
			return "parent.alert('【" +record.getName()+ "】批量导入失败，权限不足.');";
		}
		
		Map<String, String> requestMap = DMUtil.parseRequestParams(request, false); 
		String headerTL = requestMap.get("headerTL"); // 防中文乱码 
		String charSet  = (String) EasyUtils.checkNull(request.getParameter(DataExport.CHARSET), DataExport.SYS_CHAR_SET); // 默认GBK

		// 解析附件数据   
		// 如果上传的是一个 Excel，先转换为CSV文件
		if( Excel.isXLS(filepath) || Excel.isXLSX(filepath) ) { 
			filepath = Excel.excel2CSV( filepath, charSet );
			log.debug("excel2CSV end");
		}
		
		File targetFile = new File(filepath);
		List<String> originData = new ArrayList<String>(); // 导入的原始数据
		List<List<String>> rowList = readData(targetFile, charSet, headerTL, originData, _db);
		if( rowList.size() < 2) {
			return "parent.alert('导入文件没有数据');" + charSet + " " + rowList;
		}
		log.debug("readData end");
		 
		List<String> headers = rowList.get(0);
		int messyCount = 0;
		for(String fieldName : headers) {
			if( !_db.ncm.containsKey(fieldName) ) messyCount++; // 表头名 在数据表字段定义里不存在
		}
		// header都找不着，可能是CSV文件为UTF-8编码，以UTF-8再次尝试读取（注：也可能是选错了Excel文件，致使表头都对不上）
		if( messyCount == headers.size() && !DataExport.CSV_UTF8.equals(charSet) ) {
			originData.clear();
			rowList = readData(targetFile, DataExport.CSV_UTF8, headerTL, originData, _db);
			headers = rowList.get(0);
			log.debug("readData second end");
		}
		
		// 生成进度信息
		int total = rowList.size() - 1;
		int x = Math.max(total/8, 1);
		Progress progress = new Progress(x + x + total + x);  // 3个x默认作为upload、valid 和 after的进度控制
		String pgCode = getPgCode(request, _db);
		ProgressPool.putSchedule(pgCode, progress);
		progress.add(x);
		
		// 校验数据
		List<String> errLines = new ArrayList<String>(); // errorLine = lineIndex + errorMsg + row
		List<Integer> errLineIndexs = new ArrayList<Integer>();
		
		// vailderClass = com.boubei.tss.dm.record.file.EmptyDataVaild 可忽略校验
		String vailderClass = request.getParameter("vailderClass");
		String uniqueCodes  = request.getParameter("uniqueCodes");
		vailderClass = (String) EasyUtils.checkNull(vailderClass, DefaultDataVaild.class.getName());
		IDataVaild vailder = (IDataVaild) BeanUtil.newInstanceByName(vailderClass);

		List<String> valSQLFields = new ArrayList<String>();
		for(String code : _db.csql.keySet()) {
			if( _db.csql.get(code) != null && !_Field.TYPE_HIDDEN.equals(_db.ctype.get(code)) ) {
				String name = _db.cnm.get(code);
				valSQLFields.add(name);
			}
		}
		vailder.vaild(_db, rowList, headers, uniqueCodes, valSQLFields, errLines, errLineIndexs);
		progress.add(x); // 进度更新
		log.debug("vaild end");
		
		String fileName = null;
		StringBuffer logMsg = new StringBuffer();
		if(errLineIndexs.size() > 0) {
			// 将 errorLines 输出到一个单独文件
			StringBuffer errs = new StringBuffer();
			for(String err : errLines) {
				errs.append(err).append("\n");
			}
			logMsg.append("校验失败记录: " + errs.substring(0, Math.min(250, errs.length())) + "\n");
			String header = "行号,导入失败原因," + EasyUtils.list2Str(rowList.get(0));
			String body = errs.toString();
			
			// 按headerTL（如果有） 把字段名替换回原始Excel的列名
			if( headerTL != null ) {
				String[] pairs = headerTL.split(",");
				for( String _pair : pairs ) {
					String[] pair = _pair.split(":"); 
					String tssColumn = pair[0].trim();
					String xxxColumn = pair[1].trim().split("\\|")[0];  
					if( xxxColumn.indexOf("{") < 0 ) {
						header = header.replaceAll("," + tssColumn, "," + xxxColumn);
						body = body.replaceAll(tssColumn, xxxColumn);
					}
				}
			}
			String content = header + "\n" + body;
			
			fileName = "err-" + recordId + Environment.getUserId()+ ".csv";
	        String exportPath = DataExport.getExportPath() + "/" + fileName ;
	        DataExport.exportCSV(exportPath, content); // 先输出内容到服务端的导出文件中
			
			// 根据配置，是够终止导入。默认要求一次性导入，不允许分批
			if( !"false".equals( request.getParameter("together") ) ) {
				ProgressPool.finish( new Progress(100) ); // 进度设置为完成
				return "parent.alert('导入失败，" +EX.parse(EX.DM_29, errLineIndexs.size(), fileName)+ "'); ";
			}
		}
		
		// 执行导入到数据库
		headers = rowList.get(0);
		log.debug("import2db start");
		String result = import2db(_db, request, rowList, headers, originData, errLineIndexs, fileName);
		logMsg.append(result.replace("parent.alert('", "").replace("');", ""));
		log.debug("import2db end");
		
		// 导入完成触发 ETL
		JobService jobService = (JobService) Global.getBean("JobService");
		Object tag = System.currentTimeMillis();
		String etlKey = request.getParameter("etlAfterImport");
		String jobKey = request.getParameter("jobAfterImport");
		if( !EasyUtils.isNullOrEmpty(etlKey) ) {
			String[] etlKeys = etlKey.split(",");
			for(String key : etlKeys) {
				String rt = jobService.excuteTask(key, tag);
				logMsg.append("\nexecute ETL[" +key+ "] atfer import: " + rt);
			}
		}
		if( !EasyUtils.isNullOrEmpty(jobKey) ) {
			String[] jobKeys = jobKey.split(",");
			for(String key : jobKeys) {
				String rt = jobService.excuteJob(key, tag);
				logMsg.append("\nexecute Job[" +key+ "] atfer import: " + rt);
			}
		}
		ProgressPool.finish(progress); // 进度设置为完成
		
		logMsg.append( "\nrequest params: " + requestMap.toString() );
		log.info(logMsg);
		BusinessLogger.log("导入Excel", _db.recordName, logMsg.toString(), String.valueOf(total), start);
		
		return result;
	}

	protected String getPgCode(HttpServletRequest request, _Database _db) {
		String pgCode = request.getParameter("pgCode");
		pgCode = (String) EasyUtils.checkNull( pgCode, _db.recordId + "-" + Environment.getUserCode() );
		return pgCode;
	}

	/**
	 * 对于有特殊需求的数据导入，可继承本Class，然后在录入表的全局JS里重新定义afterUploadClass的值为自定义的Class
	 * @param _db
	 * @param ignoreExist  忽略or覆盖已存在的记录
	 * @param uniqueCodes  唯一性字段（一个或多个）
	 * @param rows
	 * @param headers
	 * @param errLineIndexs 校验错误的记录行
	 * @return
	 */
	protected String import2db(_Database _db, HttpServletRequest request, List<List<String>> rows, List<String> headers, List<String> originData,
			List<Integer> errLineIndexs, String fileName) {
		
		boolean ignoreExist = "true".equals( request.getParameter("ignoreExist") );
		String uniqueCodes = request.getParameter("uniqueCodes");
		
		int errLineSize = errLineIndexs.size();
		int insertCount = 0, updateCount = 0;
		List<Integer> ignoreLines = new ArrayList<Integer>();
		List<Map<String, String>> valuesMaps = new ArrayList<Map<String, String>>();
		
		List<String> snList = null; // 自动取号
		int total = rows.size();
		Progress progress = ProgressPool.getSchedule( getPgCode(request, _db) );
		
		for(int index = 1; index < total; index++) { // 第一行为表头，不要
			
			if (index % 10 == 0 || index == total - 1) {
				progress.add(10); // 进度加10
			}
			
			if( errLineIndexs.contains(index) ) continue;
			
			List<String> fieldVals = rows.get(index);
			Map<String, String> valuesMap = new HashMap<String, String>();
			for(int j = 0; j < fieldVals.size(); j++) {
    			String value = fieldVals.get(j).trim();
    			value = value.replaceAll("，", ","); // 导出时英文逗号替换成了中文逗号，导入时替换回来
    			
    			String filedLabel = headers.get(j);
    			String fieldCode = _db.ncm.get(filedLabel);
    			
    			String defaultVal = _db.cval.get(fieldCode);
    			if( EasyUtils.isNullOrEmpty(value) &&  _Field.isAutoSN(defaultVal) ) {    				
    				// 检查值为空的字段，是否配置自动取号规则，是的话先批量取出一串连号
    				if(snList == null) {
    					int snNum = total - 1 - errLineSize;
						snList = SerialNOer.create(defaultVal, snNum);
    				}
    				value = snList.get(insertCount);
    			}
    			
				valuesMap.put(fieldCode, value);
        	}
			
			/* 支持覆盖式导入，覆盖规则为参数指定的某个（或几个）字段; uniqueCodes的字段都需要建索引 */
			if( !EasyUtils.isNullOrEmpty(uniqueCodes) ) {
				// 检测记录是否已经存在
				Map<String, String> params = new HashMap<String, String>();
				
				String[] codes = uniqueCodes.trim().split(",");
				boolean hasNullParam = false;
				for(String code : codes) {
					code = code.trim();
					String value = valuesMap.get(code); // 值不能为空，为空会查出无辜的数据覆盖【查询条件为空致使查出无关的数据】
					if( EasyUtils.isNullOrEmpty(value) ) {
						hasNullParam = true;
					}
					params.put(code, value);
				}
				if( !hasNullParam ) {
					List<Map<String, Object>> result = _db.select(1, 1, params).result;  // TODO 有性能隐患，循环内SQL查询 
					if( result.size() > 0 ) {
						// 是否覆盖已存在数据
						if( ignoreExist ) {
							ignoreLines.add(index);
						} 
						else {
							Map<String, Object> old = result.get(0);
							Long itemId = EasyUtils.obj2Long(old.get("id"));
							_db.update(itemId, valuesMap);
							
							updateCount ++;
						}
						
						continue;
					}
				}
			}
			
			// 批量新增
			valuesMaps.add(valuesMap);
			insertCount ++;
			
			if(valuesMaps.size() == 10000) { // 按每一万批量插入一次
				_db.insertBatch(valuesMaps);
				valuesMaps.clear();
			}
		}
    	_db.insertBatch(valuesMaps);
		
		// 向前台返回成功信息
    	String noInserts = ignoreExist ? ("忽略了第【" +EasyUtils.list2Str(ignoreLines)+ "】行，") : ("覆盖" +updateCount+ "行，");
    	String errMsg = errLineSize == 0 ? "请刷新查看。" : EX.parse(EX.DM_29, errLineSize, fileName);
		return "parent.alert('导入完成：共新增" +insertCount+ "行，" + noInserts + errMsg + "');parent.$('.progressBar').hide();";
	}
	
}