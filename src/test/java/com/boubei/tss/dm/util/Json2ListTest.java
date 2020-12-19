/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.util;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Json2ListTest {
	
    @Test
    public void testJson2List() {
    	String paramsConfig = "[{'label':'仓库', 'type':'String', 'nullable':'false', 'jsonUrl':'../wms/whList','multiple':'true', 'height':'250px', 'onchange':'getOWCustomer'}," +
	    	  "{'label':'货主', 'type':'String', 'nullable':'false', 'options': {'codes': '', 'names': ''}, 'multiple':'true', 'height':'80px'}," +
	    	  "{'label':'货物', 'width':'300'}," +
	    	  "{'label':'库位', 'width':'300'}," +
		      "{'label':'起始时间', 'type':'date', 'nullable':'false'}, " +
			  "{'label':'结束时间', 'type':'date', 'nullable':'false'}]";
    	
    	paramsConfig = paramsConfig.replaceAll("'", "\"");
    	
    	try {  
			ObjectMapper objectMapper = new ObjectMapper();
			
	        @SuppressWarnings("unchecked")
			List<LinkedHashMap<Object, Object>> list = objectMapper.readValue(paramsConfig, List.class);  
	        for(int i = 0; i < list.size(); i++) {
	        	LinkedHashMap<Object, Object> map = list.get(i);
                
                System.out.println(map.get("label").toString().toLowerCase());
	        }
	        
	    } catch (Exception e) {  
	        e.printStackTrace();
	    }  
    }
}
