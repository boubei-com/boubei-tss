/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.etl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.DisallowConcurrentExecution;

import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportQuery;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * 按天抽取Job，可以配置多个，在不同时间点触发。
 */
@DisallowConcurrentExecution
public class ByDayETLJob extends AbstractETLJob {
	
	protected String etlType() {
		return "byDay";
	}
 
	protected  List<String> getExsitDays(Long taskId) {
		String hql = "select distinct dataDay from TaskLog where taskId = ?1 and exception='no'";
		List<String> exsitDays = new ArrayList<String>();
		List<?> list = commonService.getList(hql, taskId);
		for(Object obj : list) {
			exsitDays.add((String) obj);
		}
		
		return exsitDays;
	}
	
	/*
	 * 注：今天的数据也会汇总一份，所以 repeatDays 里至少应大于等于 1 
	 */
	public TaskLog excuteTask(Task task) {
		// 获取已经存在的日结日期 exsitDays
		List<String> exsitDays = getExsitDays(task.getId());
		Date currDay = DateUtil.noHMS(task.getStartDay());  // 默认是今天24点

		Set<Date> dateList = new LinkedHashSet<Date>();
		List<Date> repeatList = new ArrayList<Date>();
		Date today = DateUtil.addDays(DateUtil.today(), 1); // 包含今天,今天的数据也会汇总一份
		while (currDay.before(today)) {
			if ( !exsitDays.contains( DateUtil.format(currDay) ) ) {
				dateList.add(currDay); // 缺失的天
			}
			currDay = DateUtil.addDays(currDay, 1);
		}
		
		int repeats = EasyUtils.obj2Int(task.getRepeatDays());
		while ( repeats > 0 ) {
			currDay = DateUtil.subDays(today, repeats);
			dateList.add(currDay);
			repeatList.add(currDay);
			repeats --;
		}
				
		log.info(task.getName() + " is starting! total days = " +dateList.size()+ "" );
		
		long start = System.currentTimeMillis();
		int index = 0;
		TaskLog tLog = null;
		for (final Date day : dateList) {
			tLog = new TaskLog(task);
			tLog.setDataDay( DateUtil.format(day) ); //记录执行日期
	        
			try {
				long startTime = System.currentTimeMillis();
				
				String result = etlByDay(task, day, repeatList, index == 0);
				
				tLog.setException("no");
				tLog.setDetail(result);
				tLog.setRunningMS(System.currentTimeMillis() - startTime);
			} 
			catch(Exception e) {
				setException(tLog, task, e);
				return tLog;
			}
			finally {
				 // 记录任务日志，不管是否成功
		        commonService.create(tLog);
		        index++;
			}
		}

		log.info("Done! Cost time: " + (System.currentTimeMillis() - start));
		return tLog;
	}
	
	/* 按天ETL */
	protected String etlByDay(Task task, Date day, List<Date> repeatList, boolean isFirstDay) {
		// 判断是否重新抽取以更新当前日期的数据，是的话先清除已存在的改天数据
		String preRepeatSQL = task.getPreRepeatSQL();
		if( !EasyUtils.isNullOrEmpty(preRepeatSQL) ) {
			if(repeatList.contains(day)) {
				Map<Integer, Object> params = new HashMap<Integer, Object>();
				params.put(1, new Timestamp(day.getTime()));
				SQLExcutor.excute(preRepeatSQL, params, task.getTargetDS());
			}
			else if( repeatList.isEmpty() ) { // eg: truncate table, repeat = 0
				SQLExcutor.excute(preRepeatSQL, task.getTargetDS());
			}
		}
		
		Report report;
		String source = task.getSourceScript();
		try { 
    		Long reportId = EasyUtils.obj2Long(source);
    		report = reportService.getReport(reportId, false);
    	} catch(Exception e) {
    		report = new Report();
    		report.setName(task.getName());
    		report.setDatasource(task.getSourceDS());
    		report.setScript(task.getSourceScript());
    		report.setParam("[{'label':'fromDay', 'type':'date'}, {'label':'toDay', 'type':'date'}]");
    	}
		
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("param1", DateUtil.format(day));
		paramsMap.put("param2", DateUtil.format(DateUtil.addDays(day, 1)));
		
		SQLExcutor ex = ReportQuery.excute(report, paramsMap , 1, 1);
		int total = ex.count;
        int totalPages = PageInfo.calTotalPages(total, PAGE_SIZE);
        
        // 分页查询，批量插入
        String target = task.getTargetScript();
        for(int pageNum = 1; pageNum <= totalPages; pageNum++) {
        	
        	checkTask(task.getId()); // 每次循环开始前先检查任务是否被人为关停了
        	
        	ex = ReportQuery.excute(report, paramsMap, pageNum, PAGE_SIZE);
        	
        	List<Map<Integer, Object>> list1 = new ArrayList<Map<Integer, Object>>();
        	List<Map<String, String>> list2 = new ArrayList<Map<String, String>>();
        	for (Map<String, Object> row : ex.result) {
	        	Map<Integer, Object> item1 = new HashMap<Integer, Object>();
	        	Map<String, String> item2 = new HashMap<String, String>();
	        	for(int i=0; i < ex.selectFields.size(); i++) {
	        		String field = ex.selectFields.get(i); 

	        		Object value = row.get(field);
	        		item1.put(i+1, value); 
	        		item2.put(field, EasyUtils.obj2String(value));
	        	}
	        	
	        	list1.add(item1);
	        	list2.add(item2);
	        }
        	
        	// check target is a RecordTable or SQL, if SQL, this will throw exception, then do catch { ... }
        	if( target.trim().split(" ").length == 1 ) {
        		Long recordId = recordService.getRecordID(target, false);  
        		_Database db = recordService._getDB(recordId);
				db.insertBatch(list2);
        	} 
        	else {
        		SQLExcutor.excuteBatch(target, list1, task.getTargetDS());
        	}
        }
        
        return "total=" + total;
	}
}
