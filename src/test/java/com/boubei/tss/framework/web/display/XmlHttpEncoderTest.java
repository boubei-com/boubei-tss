/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.display;

import org.junit.Test;

import com.boubei.tss.framework.web.display.xmlhttp.XmlHttpEncoder;

import org.junit.Assert;

public class XmlHttpEncoderTest {
	
	@Test
	public void test() {
		XmlHttpEncoder x = new XmlHttpEncoder();
		x.put("k1", null);
		Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>", x.toXml());
	}

}
