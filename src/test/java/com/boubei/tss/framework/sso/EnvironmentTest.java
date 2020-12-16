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

import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.um.UMConstants;

public class EnvironmentTest {
	
	@Test
	public void test() {
		Context.destroy();
		
		System.out.println( Environment.threadID() );
		
		Assert.assertNull(Environment.getClientIp());
		Assert.assertEquals("/tss", Environment.getContextPath());
		Assert.assertNull(Environment.getUserCode());
		Assert.assertNull(Environment.getSessionId());
		Assert.assertTrue(Environment.getOwnRoles().contains(UMConstants.ANONYMOUS_ROLE_ID));
		
		Assert.assertNull( Environment.getDomainOrign() );
		Assert.assertNotNull( Environment.getDomain() );
		Assert.assertNull( Environment.getDomainInfo("prefix") );
		
		Assert.assertNull(Environment.getUserName());
		Assert.assertNull(Environment.getUserId());
		
		Assert.assertNull(Environment.getUserInfo("fromUserId"));
		
		Assert.assertNull(Context.getIdentityCard());
		
		Assert.assertFalse( Environment.isAdmin() );
		Assert.assertFalse( Environment.isRobot() );
		
		Assert.assertEquals("unknown", Environment.getOrigin());
	}

}
