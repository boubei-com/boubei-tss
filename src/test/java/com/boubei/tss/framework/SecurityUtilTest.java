/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import org.junit.Assert;

public class SecurityUtilTest {
	
	@Test
	public void test() {
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		Assert.assertEquals("alert(222);", SecurityUtil._fuckXSS("javascript:alert(222);", request));
		Assert.assertEquals("", SecurityUtil._fuckXSS("<script>alert(222)</script>", request));
		Assert.assertEquals("", SecurityUtil._fuckXSS("src='http://www.yihaomen.com/article/java/...'", request));
		Assert.assertEquals("", SecurityUtil._fuckXSS("src=\"http://www.yihaomen.com/article/java/...\"", request));
		Assert.assertEquals("", SecurityUtil._fuckXSS("eval( 1+1=2; )", request));
		Assert.assertEquals("alert(222);", SecurityUtil._fuckXSS("onload=alert(222);", request));
		Assert.assertEquals("alert(222);", SecurityUtil._fuckXSS("onerror=alert(222);", request));
	
		String level = Config.getAttribute(SecurityUtil.SECURITY_LEVEL);
		Config.setProperty(SecurityUtil.SECURITY_LEVEL, "wrong V");
		Assert.assertTrue(SecurityUtil.SECURITY_LEVELS[0] == SecurityUtil.getLevel());
		
		Assert.assertEquals("xxx", SecurityUtil.fuckXSS("xxx", null));
		
		Config.setProperty(SecurityUtil.SECURITY_LEVEL, level);
	}

}
