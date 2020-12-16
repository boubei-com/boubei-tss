/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso;

import com.boubei.tss.framework.exception.BusinessException;

/**
 * for test
 */
public class DoErrorLoginCustomizer implements ILoginCustomizer {
	
	static int count = 1;
 
    public void execute() {
    	if(count++ == 1) {
    		throw new BusinessException("just throw one exception");
    	}
    	throw new NullPointerException();
    }
}

	