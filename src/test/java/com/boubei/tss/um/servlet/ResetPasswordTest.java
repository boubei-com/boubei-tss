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
import com.boubei.tss.framework.Config;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;

public class ResetPasswordTest extends AbstractTest4UM {

	@Test
	public void testDoPost() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addParameter("userId", "-1");

		request.getSession().setAttribute("userId", -1L);
		request.addParameter("password", "********");
		request.addParameter("newPassword", "123456");
		request.addParameter("type", "not-reset");

		ResetPassword servlet = new ResetPassword();

		try {
			servlet.doPost(request, response);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_35, e.getMessage());
		}

		request.removeParameter("password");
		request.addParameter("password", "123456");
		try {
			servlet.doPost(request, response);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_36, e.getMessage());
		}

		request.removeParameter("newPassword");
		request.addParameter("newPassword", "123456789");
		request.getSession().setAttribute("userId", -112L); // 伪造的userId，不做响应

		try {
			servlet.doPost(request, response);
		} catch (Exception e) {
			Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
		} finally {
			servlet.destroy();
		}

		request.getSession().setAttribute("userId", -1L);
		request.removeParameter("password");
		request.removeParameter("type");
		request.addParameter("password", "123456");
		request.addParameter("type", "reset");
		try {
			servlet.doPost(request, response);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_37, e.getMessage());
		}

		Group mg = super.createGroup("BD", UMConstants.DOMAIN_ROOT_ID);
		super.createUser("JK", mg.getId(), -1L);
		login("JK");
		try {
			request.removeParameter("password");
			request.addParameter("password", "123456");
			request.removeParameter("type");
			Config.setProperty("security.level", "1");
			servlet.doGet(request, response);

			request.removeParameter("password");
			request.removeParameter("newPassword");
			request.addParameter("password", "123456");
			request.addParameter("newPassword", "123456789@&*(");
			Config.setProperty("security.level", "4");
			servlet.doGet(request, response);

		} catch (Exception e) {
			Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
		} finally {
			servlet.destroy();
		}
	}

}
