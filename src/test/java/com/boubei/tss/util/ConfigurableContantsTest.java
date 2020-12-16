/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.util;

import org.junit.Test;

import junit.framework.Assert;

public class ConfigurableContantsTest extends ConfigurableContants {
	
	@Test
	public void test() {
		super.init("log4j.properties");
		
		Assert.assertEquals(null, super.getProperty("XXX"));
		Assert.assertEquals("123456", super.getProperty("XXX", "123456"));
		
		try { super.init("xxx.properties"); } catch(Exception e) { }
	}

}
