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

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.sso.context.Context;

public class PasswordPassportTest extends AbstractTest4F {
	
	@Test
	public void test() {
		Context.initRequestContext(request);
		
		request.addHeader(SSOConstants.LOGIN_CHECK_KEY, "12");
		request.getSession(true).setAttribute(SSOConstants.LOGIN_CHECK_KEY, 13);
		
        try{
        	new PasswordPassport();
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertEquals(EX.U_01, e.getMessage());
	    }
        
        request.removeParameter(SSOConstants.LOGIN_CHECK_KEY);
        request.getSession().setAttribute(SSOConstants.LOGIN_CHECK_KEY, 12);
        try{
        	new PasswordPassport();
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertEquals(EX.U_02, e.getMessage());
	    }
		
        request.addHeader(SSOConstants.USER_ACCOUNT, "70X99X98X71X101X98X107");
        request.addHeader(SSOConstants.USER_PASSWD, "61X62X63X56X57X58");
        try{
        	new PasswordPassport();
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertEquals(EX.U_03, e.getMessage());
	    }
        
        request.addHeader(SSOConstants.RANDOM_KEY, "8341");
        new PasswordPassport();
	}

}
