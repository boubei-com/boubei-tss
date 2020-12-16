/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso;

import java.util.HashMap;
import java.util.Map;
 
public class DemoOperator implements IOperator {
 
    private static final long serialVersionUID = 1L;

    private Map<String, Object> attributesMap = new HashMap<String, Object>();

    private Long userId;

    public DemoOperator(Long userId) {
        this.userId = userId + 1;
        attributesMap.put("id", this.userId);
        attributesMap.put("loginName", getLoginName());
        attributesMap.put("userName", getUserName());
    }
 
    public Map<String, Object> getAttributesMap() {
        return attributesMap;
    }
 
    public Long getId() {
        return userId;
    }

    public String getLoginName() {
        return "Jon.King";
    }

    public String getUserName() {
        return "JinPujun";
    }

    public boolean isAnonymous() {
        return false;
    }

    public void setId(Long id){
    	this.userId = id;
    }

}
