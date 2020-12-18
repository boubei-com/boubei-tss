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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.DisallowConcurrentExecution;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportQuery;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.util.EasyUtils;

/**
 * 按ID抽取Job，可以配置多个，对应不同的间隔时间
 */
@DisallowConcurrentExecution
public class ByIDETLJob extends AbstractETLJob {
	
	protected String etlType() {
		return "byID";
	}
	
	protected Object getMaxID(Task task) {
		// 优先从目标表里取出最大ID
		try {
			String preSQL = task.getPreRepeatSQL();
			if( !EasyUtils.isNullOrEmpty(preSQL) ) {
				Object maxID = SQLExcutor.query(task.getTargetDS(), preSQL).get(0).get("maxid");
				return EasyUtils.obj2Long( maxID );
			}
		} catch(Exception e) { }
		
		// 如果没有设置，则取日志里记录下来的最大ID
		String hql = "select max(maxID) from TaskLog where taskId = ?1 and exception='no'";
		List<?> list = commonService.getList(hql, task.getId());
		Object maxId = null;
		try {
			maxId = list.get(0);
		} catch(Exception e) { }
		
		return maxId;
	}

	public TaskLog excuteTask(Task task) {
		Long maxID = EasyUtils.obj2Long( getMaxID(task) );
		maxID = Math.max( maxID, EasyUtils.obj2Long(task.getStartID()) ); // 如果任务上设置的ID大于日志里记录的最大ID，则说明是人为单独设置了任务上的ID
		
		log.info(task.getName() + " is starting! 【" + maxID + "】" );
		
		long start = System.currentTimeMillis();
		TaskLog tLog = new TaskLog(task);
        
		try {
			Long[] result = etlByID(task, maxID);
			
			tLog.setException("no");
			tLog.setDetail("total=" + result[0]);
			tLog.setMaxID(result[1]);
		} 
		catch(Exception e) {
			setException(tLog, task, e);
		}
		finally {
			 // 记录任务日志，不管是否成功
			tLog.setRunningMS(System.currentTimeMillis() - start);
	        commonService.create(tLog);
		}

		log.info(task.getName() + " Done! Cost time: " + (System.currentTimeMillis() - start));
		return tLog;
	}
	
	protected Long[] etlByID(Task task, Long startID) {
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
    		report.setParam("[{'label':'maxID', 'type':'number'}]");
    	}
		// 始终按ID排序
		report.setScript( "select * from (" +report.getScript()+") t order by id asc" );
		
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("param1", String.valueOf(startID));
		
		SQLExcutor ex = ReportQuery.excute(report, paramsMap, 1, 1);
		int total = ex.count;
        int totalPages = PageInfo.calTotalPages(total, PAGE_SIZE);
        
        // 分页查询，批量插入
        Long maxID = startID;
        String outputScript = task.getTargetScript();
        for(int pageNum = 1; pageNum <= totalPages; pageNum++) {
        	
        	checkTask(task.getId()); // 每次循环开始前先检查任务是否被人为关停了
        	
        	long start = System.currentTimeMillis();
        	ex = ReportQuery.excute(report, paramsMap, pageNum, PAGE_SIZE);
        	
        	List<Map<Integer, Object>> list = new ArrayList<Map<Integer, Object>>();
        	for (Map<String, Object> row : ex.result) {
	        	Map<Integer, Object> item = new HashMap<Integer, Object>();
	        	for(String field : ex.selectFields) {
	        		Object value = row.get(field);
	        		item.put(item.size()+1, value); 
	        	}
	        	
	        	list.add(item);
	        	maxID = Math.max(maxID, EasyUtils.obj2Long(row.get("id")));
	        }
        	
        	SQLExcutor.excuteBatch(outputScript, list, task.getTargetDS());
        	
        	// 如果分多页插入，则每每一页插入记录日志（除了最后一页，最后一页在excuteTask里记）
        	if(pageNum < totalPages) {
	        	TaskLog tLog = new TaskLog(task);
	        	tLog.setException("no");
				tLog.setDetail("page-" +pageNum+ "=" + list.size());
				tLog.setMaxID(maxID);
				tLog.setRunningMS(System.currentTimeMillis() - start);
		        commonService.create(tLog);
        	}
        }
        
        return new Long[] { (long) total, maxID};
	}
}
