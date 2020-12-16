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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;

public class GetPasswordTest extends AbstractTest4UM {
    
    @Autowired IUserService userService;
    
    @Test
    public void testDoPost() {
        MockHttpServletRequest request = new MockHttpServletRequest(); 
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        request.addParameter(SSOConstants.USER_ACCOUNT, "Admin001");
        request.addParameter("passwordAnswer", "?");
        request.addParameter("passwordQuestion", "!");
        
        CheckAnswer servlet = new CheckAnswer();
        
        try {
            servlet.doPost(request, response);
            
            request.removeParameter(SSOConstants.USER_ACCOUNT);
            request.addParameter(SSOConstants.USER_ACCOUNT, "Admin");
            servlet.doPost(request, response);
            
            User user = userService.getUserByLoginName("Admin");
            user.setPasswordQuestion("1+1=?");
            user.setPasswordAnswer("=2");
            servlet.doPost(request, response);
            
            request.removeParameter("passwordAnswer");
            request.addParameter("passwordAnswer", "************");
            servlet.doGet(request, response);
            
            request.removeAllParameters();
            request.addParameter(SSOConstants.USER_ACCOUNT, "Admin");
            request.addParameter("passwordQuestion", "1+1=?");
            request.addParameter("passwordAnswer", "=2");
            servlet.doGet(request, response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        }
    }
    
}
