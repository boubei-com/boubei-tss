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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.EasyUtils;

/**
 * 1、报销 + 费用 审批后自动流到 付款流程 ---- 用【ETL 抽取】 或 【Excel批量导入】
 * 2、WFJob批量计算wfStatus，适合数据清洗进来的流程数据（逐个取出计算一遍即可）;
 * 
 * TODO 定期器里没有request，没有session，而mockLogin需要session。 只能手动执行触发，页面加刷新按钮
 *    
 *  com.boubei.tss.dm.record.workflow.WFJob | 0 07 * * * ? | 12,13
 */
@Deprecated
public class WFJob extends AbstractJob {
	
	APIService apiService = (APIService) Global.getBean("APIService");
	WFService wfService = (WFService) Global.getBean("WFService");
	RecordService recordService = (RecordService) Global.getBean("RecordService");

	// jobConfig 为 tableIds
	protected String excuteJob(String jobConfig, Long jobID) {
		
		String currUserCode = Environment.getUserCode();
    	String[] jobConfigs = EasyUtils.split(jobConfig, ","); 
    	for(String _tableId : jobConfigs) {
    		Long tableID = EasyUtils.obj2Long(_tableId);
    		_Database _db = recordService.getDB(tableID);
    		CacheHelper.getLongCache().putObject(  _Database._CACHE_KEY(tableID), _db);
    		
    		Map<String, String> params = new HashMap<String, String>();
    		params.put("sortField", "updatetime");
    		params.put("sortType", "onlynull");
    		
    		List<Map<String, Object>> result = _db.select(0, 0, params ).result;
    		for( Map<String, Object> row : result ) {
    			Long id = (Long) row.get("id");
    			
    			apiService.mockLogin( (String) row.get("creator") ); 
    			wfService.calculateWFStatus(id, _db);
    		}
    		
    		String updateSQL = "update " + _db.table + " set updatetime = ? where updatetime is null";
			Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
			paramsMap.put(1, new Date());
			SQLExcutor.excute(updateSQL, paramsMap, _db.datasource);
		}
    	
    	// 手动触发WFJob时，需重新登录回触发用户的账号
    	if( !UMConstants.ROBOT_USER_NAME.equals(currUserCode) ) {
        	apiService.mockLogin( currUserCode );  
    	}
    	
    	return "success";
	}

}
