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

import org.junit.Test;

import com.boubei.tss.framework.Config;

import junit.framework.Assert;

public class IdentityGetterFactoryTest {
	
	@Test
	public void test1() {
		String old = Config.getAttribute(SSOConstants.IDENTITY_GETTER);
		Config.setProperty(SSOConstants.IDENTITY_GETTER, " ");
		
		try {
			IdentityGetterFactory.create();
//			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals("实例化失败： ", e.getMessage());
		} finally {
			Config.setProperty(SSOConstants.IDENTITY_GETTER, old);
		}
	}
	
	@Test
	public void test2() {
		IdentityGetter instance = IdentityGetterFactory.create();
		Assert.assertNotNull(instance);
		
		IdentityGetterFactory.getter = null;
		instance = IdentityGetterFactory.create();
		Assert.assertNotNull(instance);
	}
	
	@Test
	public void test3() {
		String old = Config.getAttribute(SSOConstants.LOGIN_COSTOMIZER);
		Config.setProperty(SSOConstants.LOGIN_COSTOMIZER, " ");
		
		Assert.assertNotNull( LoginCustomizerFactory.instance().getCustomizer() );
		Config.setProperty(SSOConstants.LOGIN_COSTOMIZER, old);
	}

}
