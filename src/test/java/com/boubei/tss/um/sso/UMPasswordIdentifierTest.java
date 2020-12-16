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
import com.boubei.tss.framework.sso.DemoIdentityGetter;
import com.boubei.tss.framework.sso.IdentityGetterFactory;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.InfoEncoder;

public class UMPasswordIdentifierTest extends AbstractTest4UM {
	
	@Autowired IUserService userService;
	
	public void init() {
		super.init();
		
		User admin = userService.getUserById(-1L);
		admin.setPassword("123456");
		userService.createOrUpdateUser(admin, "-2", "-1");
	}
	
	@Test
	public void testIdentifyInUM() {
		initContext();
		
		request.addParameter(SSOConstants.USER_ACCOUNT, InfoEncoder.simpleEncode("Admin", 100));
		request.addParameter(SSOConstants.USER_PASSWD, InfoEncoder.simpleEncode("123456", 100));
		
		UMPasswordIdentifier indentifier = new UMPasswordIdentifier();
		
		try {
			indentifier.identify();
		} catch (Exception e) {
			Assert.assertFalse(e.getMessage(), true);
		} 
		
		request.removeParameter(SSOConstants.USER_ACCOUNT);
		request.addParameter(SSOConstants.USER_ACCOUNT, InfoEncoder.simpleEncode("wrongAccout", 100));
		
		try {
			indentifier.identify();
			Assert.fail("should throw exception but did't");
		} catch (Exception e) {
			Assert.assertEquals(  EX.parse(EX.U_00, "wrongAccout") , e.getMessage());
		} 
	}
	
	@Test
	public void testIdentifyInOA2() {
		initContext();
		
		IdentityGetterFactory.getter = new DemoIdentityGetter();
		
		request.addParameter(SSOConstants.USER_ACCOUNT, InfoEncoder.simpleEncode("Admin", 100));
		request.addParameter(SSOConstants.USER_PASSWD, InfoEncoder.simpleEncode("wrongpassword", 100));
		
		UMPasswordIdentifier indentifier = new UMPasswordIdentifier();
		try {
			indentifier.identify();
		} catch (Exception e) {
			Assert.assertFalse(e.getMessage(), true);
		} 
	}

}
