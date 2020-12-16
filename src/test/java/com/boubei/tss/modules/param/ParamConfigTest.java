/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.param;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;


public class ParamConfigTest {
	
	@Test
	public void testGetAttribute() {
		
		 assertEquals("TSS", ParamConfig.getAttribute("application.code"));
		 
		 assertEquals("TSS", ParamConfig.getAttribute("application.code", "WMS"));
		 assertEquals("WMS", ParamConfig.getAttribute("xxx.code", "WMS"));
		 
		 Assert.assertNull( ParamConfig.getAttribute("not exists")  );
	}

}
