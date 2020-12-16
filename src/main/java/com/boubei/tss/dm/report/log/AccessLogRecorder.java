/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report.log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.boubei.tss.cache.extension.workqueue.AbstractTask;
import com.boubei.tss.cache.extension.workqueue.OutputRecordsManager;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.EasyUtils;

public class AccessLogRecorder extends OutputRecordsManager {

    private static AccessLogRecorder instance;

    private AccessLogRecorder() {}

    public static AccessLogRecorder getInstanse() {
        if (instance == null) {
            instance = new AccessLogRecorder();
        }
        return instance;
    }

    protected int getMaxSize() {
        return 20;
    }

    protected int getMaxTime() {
        return 5 * 60 * 1000;
    }

    protected void excuteTask(List<Object> logs) {
        AbstractTask task = new AccessLogOutputTask();
        task.fill(logs);

        tpool.excute(task);
    }

    private final class AccessLogOutputTask extends AbstractTask {
        public void excute() {
            List<Map<Integer, Object>> paramsMapList = new ArrayList<Map<Integer, Object>>();
            for (Object temp : records) {
                AccessLog log = (AccessLog) temp;
                Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
                int index = 1;
                paramsMap.put(index++, log.getClassName());
                paramsMap.put(index++, log.getMethodName());
                paramsMap.put(index++, log.getMethodCnName());
                paramsMap.put(index++, new Timestamp(log.getAccessTime().getTime()));
                paramsMap.put(index++, log.getRunningTime());
                paramsMap.put(index++, log.getParams());
                paramsMap.put(index++, log.getUserId());
                paramsMap.put(index++, log.getIp());
                paramsMap.put(index++, log.getOrigin());

                paramsMapList.add(paramsMap);
            }

            String script = "insert into dm_access_log " +
        			"(className, methodName, methodCnName, accessTime, runningTime, params, userId, ip, origin) " +
        			"values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            SQLExcutor.excuteBatch(script, paramsMapList, DMConstants.LOCAL_CONN_POOL);
        }
    }
    
	public static void outputAccessLog(String className, String name, 
			String methodName, Map<String, String> requestMap, long start) {
		
		requestMap.remove("uName");
        requestMap.remove("uToken");
        requestMap.remove("uSign");
        
		String params = "";
		for(Entry<String, String> entry : requestMap.entrySet()) {
			params += entry.getKey() + "=" + entry.getValue() + ", ";
		}
        params = DMUtil.cutParams(params);
		
		// 方法的访问日志记录成败不影响方法的正常访问，所以对记录日志过程中各种可能异常进行try catch
        try {
            AccessLog log = new AccessLog(start, params);
            log.setClassName(className);
            log.setMethodCnName( name );
    		log.setMethodName( methodName );
            
            AccessLogRecorder.getInstanse().output(log);
        } 
        catch(Exception e) { }
	}
	
	/** 记录下报表的访问信息。 */
	public static void outputAccessLog(ReportService reportService, Long reportId, 
			String methodName, Map<String, String> requestMap, long start) {
		
		Report report = reportService.getReport(reportId);
		boolean ignoreLog = ParamConstants.FALSE.equals(report.getNeedLog());
		if( !ignoreLog || System.currentTimeMillis() - start > 1000 ) {
			String reportName = report.getName();
			String tempName = (String) EasyUtils.checkNull(report.getCode(), "Report-"+reportId);
			AccessLogRecorder.outputAccessLog(tempName, reportName, methodName, requestMap, start);
		}
	}
}
