/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.servlet;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.um.AbstractTest4UM;

public class GetLoginInfoTest extends AbstractTest4UM {
    
	@Test
    public void testDoPost() {
        request = new MockHttpServletRequest(); 
        response = new MockHttpServletResponse();
        
        request.addParameter(SSOConstants.USER_ACCOUNT, "Admin");
        
        try {
            new GetLoginInfo().doGet(request, response);
            
        } catch (Exception e) {
        	e.printStackTrace();
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        }
    }
	
	@Test
    public void testError() {
        request = new MockHttpServletRequest(); 
        response = new MockHttpServletResponse();
        
        request.addParameter(SSOConstants.USER_ACCOUNT, "Admin-X");
        
        try {
        	new GetLoginInfo().doGet(request, response);
            Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
            Assert.assertEquals(  EX.parse(EX.U_00, "Admin-X"), e.getMessage());
        }
    }
    
}
