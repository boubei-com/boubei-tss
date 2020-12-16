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


public class DemoIdentityGetter implements IdentityGetter {
    
    public IOperator getOperator(Long standardUserId) {
        return new DemoOperator(standardUserId);
    }

	public boolean indentify(IPWDOperator operator, String password) {
		return true;
	}
}
