/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.identifier.BaseUserIdentifier;
import com.boubei.tss.framework.web.display.xmlhttp.XmlHttpEncoder;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.InfoEncoder;

public class SMSIdentifierTest extends AbstractTest4UM {
	
	@Autowired IUserService userService;
	
	User user;
	String mobile = "13588833833";
	
	public void init() {
		super.init();
		
		user = new User();
		user.setLoginName(mobile);
		user.setUserName(mobile);
		user.setTelephone(mobile);
		user.setPassword("123456");
		userService.createOrUpdateUser(user, "-2", "-1");
	}
	
	@Test
	public void testIdentifyInUM() {
		initContext();
		
		BaseUserIdentifier indentifier = new SMSIdentifier();
		Object smsCode = indentifier.before(user, new XmlHttpEncoder(), request.getSession());
		
		request.addParameter(SSOConstants.USER_ACCOUNT, InfoEncoder.simpleEncode(mobile, 100));
		request.addParameter(SSOConstants.USER_PASSWD, InfoEncoder.simpleEncode(smsCode.toString(), 100));
		
		try {
			indentifier.identify();
		} catch (Exception e) {
			Assert.assertFalse(e.getMessage(), true);
		} 
		
		request.removeParameter(SSOConstants.USER_PASSWD);
		request.addParameter(SSOConstants.USER_PASSWD, InfoEncoder.simpleEncode("9999", 100));
		
		try {
			indentifier.identify();
			Assert.fail("should throw exception but did't");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_51, e.getMessage());
		} 
	}
}
