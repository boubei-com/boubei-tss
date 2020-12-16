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

import org.junit.Assert;
import org.junit.Test;

public class ConfigTest {
	
	@Test
	public void testConfig() {
		
		Assert.assertNull( Config.getAttributesSet("not exsits"));
		Assert.assertEquals(2, Config.getAttributesSet("test").size());
		Assert.assertEquals(2, Config.getAttributesSet("test").size());
		
		Assert.assertEquals("TSS", Config.getAttribute("application.code"));
	}
}
