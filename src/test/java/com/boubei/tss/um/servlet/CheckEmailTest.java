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

import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;

public class CheckEmailTest extends AbstractTest4UM {
    
    @Autowired IUserService userService;
    
    @Test
    public void testDoPost() {
        MockHttpServletRequest request = new MockHttpServletRequest(); 
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        request.addParameter(SSOConstants.USER_ACCOUNT, "Admin001");
        
        CheckEmail servlet = new CheckEmail();
        
        try {
        	// user not exist
            servlet.doGet(request, response);
            
            // user no email
            request.removeParameter(SSOConstants.USER_ACCOUNT);
            request.addParameter(SSOConstants.USER_ACCOUNT, "Admin");
            servlet.doPost(request, response);
            
            // user has email
            User user = userService.getUserByLoginName("Admin");
            user.setEmail("xxx@xxx.com");
            userService.createOrUpdateUser(user, ""+UMConstants.MAIN_GROUP_ID, "-1");
            
            request.addParameter("email", user.getEmail());
            servlet.doPost(request, response);
            
            // test reset password
            HttpSession session = request.getSession();
			Integer ckcode = (Integer) session.getAttribute(SSOConstants.RANDOM_KEY);
            ResetPassword servlet2 = new ResetPassword();
            
            request.addParameter("password", "www.boubei.com");
            request.addParameter("type", "reset");
            request.addParameter("ckcode", ckcode.toString());
            servlet2.doGet(request, response);
            
            // test wrong ckcode
            request.removeParameter("ckcode");
            request.addParameter("ckcode", "12345678");
            try {
            	servlet2.doGet(request, response);
                Assert.fail("should throw exception but didn't.");
            } catch (Exception e) {
                Assert.assertEquals(EX.U_45, e.getMessage());
            }
            
        } catch (Exception e) {
        	e.printStackTrace();
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        }
    }
    
}
