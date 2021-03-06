/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence.pagequery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;

/**
 * 支持宏定义的查询条件类基类 
 */
public abstract class MacrocodeQueryCondition  {
    
    /**
     * 分页信息对象
     */
    PageInfo page;  
    
    /**
     * 排序字段，格式如 ["o.decode asc", "u.createdTime desc"]
     */
    List<String> orderByFields;  
    
    /**
     * 条件对象中不理睬的属性名称集合
     */
    Set<String> ignores; 
 
    public PageInfo getPage() {
        if(page == null) {
            page = new PageInfo();
        }
        return page;
    }
 
    public Set<String> getIgnoreProperties() {
        if(ignores == null) {
            ignores = new HashSet<String>(); 
            ignores.add("orderByFields");
            ignores.add("ignores");
            ignores.add("page");
        }
        return ignores;
    }
    
	public List<String> getOrderByFields() {
	    if(orderByFields == null) {
	        orderByFields = new ArrayList<String>();
	    }
		return orderByFields;
	}

	public String toConditionString() {
		StringBuffer buffer = new StringBuffer();
		Map<String, Object> conditionsMap = getConditionMacrocodes();
		for(String macro : conditionsMap.keySet()) {
			if( macro.startsWith("${") ) {
				buffer.append(macro).append(" ");
			}
		}
		return buffer.toString();
	}
	
	public String toString() {
		return toConditionString();
	}
	
    /**
     * 获取条件查询HQL/SQL条件语句宏代码字典.
     * 
     * 正常条件表示式用占位符： map.put("${xxx}", and o.xxx = :xxx);
     * 如果条件表达式是确定的，则用 #{xxx}，eg: map.put( "#{domain}", "and o.createId in (1,2,3)" );
     * 
     * @return Map 
     * 			条件宏代码字典对象
     */
	public Map<String, Object> getConditionMacrocodes() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("#{domain}", " and o.domain in ('" +Environment.getDomain()+ "')");
        
        return map;
	}
	
	public static String wrapLike(String val) {
		if( !EasyUtils.isNullOrEmpty(val) ) {
			val = "%" + val.trim() + "%";           
        }
		return val;
	}
}

	