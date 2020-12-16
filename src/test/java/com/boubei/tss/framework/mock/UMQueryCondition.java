/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;

public class UMQueryCondition extends MacrocodeQueryCondition {
 
	private String userName;   // 姓名
	private String ids;
	private String domain;
	
    public Map<String, Object> getConditionMacrocodes() {
        Map<String, Object> map = super.getConditionMacrocodes();
        
        map.put("${userName}",   " and o.userName = :userName");
        map.put("${ids}",   " and o.id in (:ids)");
        map.put("${domain}",   " and '无域' <> :domain");
        map.put(" and 1=1 ",   " ");
        
        return map;
    }

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public List<String> getIds() {
		if (ids != null) {
			return Arrays.asList(ids.split(","));
		}
		return null;
	}

	public void setIds(String ids) {
		this.ids = ids;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}
