/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report;

import java.util.Date;
import java.util.Map;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.util.DateUtil;

public class XScriptParser implements ScriptParser {

	public String parse(String script, Map<String, Object> dataMap) {
		
		Date now = new Date();
		
		String curMonth = DateUtil.toYYYYMM(now, "-");
		dataMap.put("cur_month", curMonth);
		dataMap.put("cur_month_01", curMonth + "-01");
		
		dataMap.put("cur_year_01", DateUtil.getYear(now) + "-01-01");
			
		int deltaDays = DateUtil.getDayOfWeek(now) - 1;
		dataMap.put("cur_week_01", DateUtil.format(DateUtil.subDays(now, deltaDays)) );
    	
		return DMUtil.freemarkerParse(script, dataMap);
	}

}
