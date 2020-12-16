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

import org.junit.Test;

import com.boubei.tss._TestUtil;

public class ImportAppConfigTest {
	
	@Test
	public void test() {
		String filepath = _TestUtil.getSQLDir() + "/tss-resource-config.xml";
		try {
			new ImportAppConfig().processUploadFile(null, filepath, "");
		} catch (Exception e) {
//			Assert.fail();
		}
	}

}
