/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReportQuery {
	
	static Logger log = Logger.getLogger(ReportQuery.class);
	
  	@SuppressWarnings("unchecked")
    public static SQLExcutor excute(Report report, Map<String, String> requestMap, int page, int pagesize) {
    	
		String paramsConfig = report.getParam();
		String reportScript = report.getScript();
		if( EasyUtils.isNullOrEmpty(reportScript) ) {
			return new SQLExcutor();
		}
          
		// 宏代码池
      	Map<String, Object> fmDataMap = DMUtil.getFreemarkerDataMap();
      	
		/* 先预解析，以判断request参数是否用做了宏代码。后续还将有一次解析，以支持宏嵌套。 
		 * eg： ${GetXXXByLogonUser} --> param里的script宏（... creator = ${userCode}) ... ）
		 * 这里先把  ${GetXXXByLogonUser} 解析出来，下面会接着解析${userCode} */
      	reportScript = MacrocodeCompiler.runLoop(reportScript, fmDataMap, true); 
      	
      	// 加入所有request请求带的参数
      	fmDataMap.putAll(requestMap);
      	
      	// 过滤掉用于宏解析（ ${paramX} ）后的request参数
     	Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
     	
     	if( EasyUtils.isNullOrEmpty(paramsConfig) && requestMap.size() > 0 ) {
     		StringBuffer sb = new StringBuffer("[");
     		for(String key : requestMap.keySet()) {
     			if( reportScript.indexOf( MacrocodeCompiler.createMacroCode(key) ) >= 0 ) {
     				sb.append("{'code': '" + key + "'},");
     			}
     		}
     		paramsConfig = sb.append("]").toString().replace(",]", "]");
     	}
		
		if( !EasyUtils.isNullOrEmpty(paramsConfig) ) {
      		List<LinkedHashMap<Object, Object>> list;
      		try {  
      			paramsConfig = paramsConfig.replaceAll("'", "\"");
  				list = new ObjectMapper().readValue(paramsConfig, List.class);  
      	        
      	    } catch (Exception e) {  
      	        throw new BusinessException( EX.parse(EX.DM_15, report, e.getMessage()) );
      	    }  
      		
      		for(int i = 0; i < list.size(); i++) {
  	        	LinkedHashMap<Object, Object> map = list.get(i);
  	        	
  	        	int index = i + 1;
  	        	String paramKey = "param" + index;
  	        	String code = (String) EasyUtils.checkNull(map.get("code"), paramKey); // 允许为每个查询条件自定义code
  	        	String paramValue = (String) EasyUtils.checkNull(requestMap.get(code), requestMap.get(paramKey));
				if ( EasyUtils.isNullOrEmpty(paramValue) ) {
					if( "false".equals(map.get("nullable")) ) {
						throw new BusinessException(EX.parse(EX.DM_20, map.get("label")));
					}
					continue;
				}
				
				// 对paramValue进行检测，防止SQL注入
				paramValue = DMUtil.checkSQLInject( paramValue.trim() );
				
				/* 
				 * 判断是否作为宏定义用于freemarker的模板解析
				 * 如一些只用于多级下拉联动的参数，可能并不用于FM(script+参数）
				 */
				String _script = reportScript.toLowerCase();
				Object ignore = null; 
				String regKey = (code+ "|" +paramKey).toLowerCase();
				if ( Pattern.compile("\\$\\{[\\s]*(" +regKey+ ")[\\s]*\\}").matcher(_script).find() ) {
					ignore = "true";		
				}
				else if( Pattern.compile("if[\\s]+(" +regKey+ ")").matcher(_script).find() && 
						!Pattern.compile("if[\\s]+(" +regKey+ ")\\?\\?").matcher(_script).find() ) {
					
					// <#if param1==1> or <#elseif param1==1>
					// eg1: <#if param1==1> group by week </#if>  --> is macrocode: true 
					// eg2: <#if param1??> createTime > ? </#if>  --> is macrocode: false
					ignore = "true";
				}
				ignore = EasyUtils.checkNull(ignore, map.get("isMacrocode"));
				
				// 隐藏类型的参数
				Object paramType = map.get("type");
				if("hidden".equals(paramType)) {
					ignore = "true";
		  		}
				
				// 将日期的快捷写法，转换成相应日期
				paramValue = DateUtil.fastCast(paramValue);
				Object _paramValue = DMUtil.preTreatValue(paramValue, paramType);
				if( EasyUtils.obj2String(paramValue).indexOf("\'") > 0 ) { // 输入的参数值中含有英文逗号(中间字符)，拼接到SQL里将会报错
					_paramValue = EasyUtils.obj2String(paramValue).replaceAll("\'", "");
	  	  		}

				// 处理in查询的条件值，为每个项加上单引号 
				if ( Pattern.compile("in[\\s]*\\(\\$\\{[\\s]*(" +regKey+ ")[\\s]*\\}\\)").matcher(_script).find() ) {
					
					_paramValue = DMUtil.insertSingleQuotes(paramValue); 
				}
				// 判断参数是否只用于freemarker解析
				else if ( !"true".equals(ignore) ) {
					paramsMap.put(paramsMap.size() + 1, _paramValue);
				}
				
				if(_Field.TYPE_DATE.equals(paramType) || _Field.TYPE_DATETIME.equals(paramType)) {
					_paramValue = paramValue; // 日期用于Freemarker解析时不能是Date对象类型，用于通配符?则可以
				}
				fmDataMap.put(paramKey, _paramValue);
				fmDataMap.put(code, _paramValue);
  	        }
      	}
      	
        // 结合 requestMap 进行 freemarker解析 sql，允许指定sql预处理类。
		String datasource = report.getDatasource();
      	fmDataMap.put("report.info", report.toString()); // 用于解析出错时定位report
      	reportScript = DMUtil.fmParse(reportScript, fmDataMap, true);
          
		SQLExcutor excutor = new SQLExcutor();
		excutor.testFlag = requestMap.containsKey("testFlag");
		try {
			excutor.excuteQuery(reportScript, paramsMap, page, pagesize, datasource);
		} catch (Exception e) {
			String exMsg = e.getMessage();
			log.error( report + exMsg + ", params: " + requestMap + ", visitor: " + Environment.getUserName());
			throw new BusinessException(exMsg);
		}

		return excutor;
  	}
  	
  	/**
  	 * 查出过去N天个人访问过的报表、系统里的热门报表、新出的报表
  	 */
  	public static List<Report> getMyReports(List<Report> list, Long groupId) {
	    
	    List<String> topSelf = getTops(true);
	    List<String> topX = getTops(false);
	    Long selfGroupId = -2L, topGroupId = -3L, newGroupId = -4L;
	    		
	    List<Report> result = new ArrayList<Report>();
    	result.add(new Report(selfGroupId, "最近访问报表", null));
    	result.addAll( cloneTops(selfGroupId, topSelf, list) );

    	result.add(new Report(topGroupId, "近期热门报表", null));
    	result.addAll( cloneTops(topGroupId, topX, list) );
	    
	    result.add(new Report(newGroupId, "近期新出报表", null));
	    List<Report> lastest = new ArrayList<Report>();
    	for(Report report : list) {
    		if( !report.isActive()  || report.getId().equals(groupId) )  continue;
 
    		if( !report.isGroup() 
    				&& report.getCreateTime().after(DateUtil.subDays(DateUtil.today(), 10))
    				&& StringUtil.hasCNChar(report.getName())) {
    			
    			lastest.add(cloneReport(newGroupId, report));
    		}
    		
    		result.add(report); // 此处将list里的所有report及分组放入到result里
    	}
    	sortLastest(result, lastest);
       
        return result;
    }

  	static void sortLastest(List<Report> result, List<Report> lastest) {
		Collections.sort(lastest, new Comparator<Report>() {
            public int compare(Report r1, Report r2) {
                return r2.getId().intValue() - r1.getId().intValue();
            }
        });
    	result.addAll(lastest.size() > 3 ? lastest.subList(0, 3) : lastest);
	}
    
    private static List<Report> cloneTops(Long topGroupId, List<String> topX, List<Report> list) {
    	List<Report> result = new ArrayList<Report>();
    	for(String cn : topX) {
    		for(Report rp : list) {
	    		if(cn.equals(rp.getCode()) && rp.isActive() && !rp.isGroup()) {
	        		result.add( cloneReport(topGroupId, rp) );
	        		break;
	    		}
	    	}
    	}
    	
    	return result;
    }

	private static Report cloneReport(Long topGroupId, Report report) {
		Report clone = new Report();
		BeanUtil.copy(clone, report);
		clone.setParentId(topGroupId);
		return clone;
	}
 
    private static List<String> getTops(boolean onlySelf) {
    	String sql = "select className name, count(*) value, max(l.accessTime) lastTime, max(methodCnName) cn " +
	    		" from dm_access_log l " +
	    		" where l.accessTime >= ? " + (onlySelf ? " and l.userId = ?" : "") +
	    		" group by className " +
	    		" order by " + (onlySelf ? "lastTime" : "value")  + " desc";
    	
	    Map<Integer, Object> params = new HashMap<Integer, Object>();
	    
	    // 日志量大的，不宜取太多天; 默认取3天
	    int historyDays = 3;
		try {
			historyDays = EasyUtils.obj2Int( ParamManager.getValue(PX.TOP_REPORT_LOG_DAYS, "3") ); 
		} catch (Exception e) {}
	    params.put(1, DateUtil.subDays(DateUtil.today(), historyDays));
	    
	    if(onlySelf) {
	    	params.put(2, Environment.getUserId());
	    }
	    
	    SQLExcutor ex = new SQLExcutor();
		ex.excuteQuery(sql, params , DMConstants.LOCAL_CONN_POOL);
	    
	    List<String> tops = new ArrayList<String>();
	    List<Map<String, Object>> list = ex.result.subList(0, Math.min(onlySelf ? 5 : 3, ex.count));
	    for( Map<String, Object> row : list){
    		String reportName = (String) row.get("name");
    		tops.add(reportName);
	    }
	    return tops;
    }

}
