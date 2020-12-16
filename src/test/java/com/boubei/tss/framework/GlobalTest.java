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

public class GlobalTest {
	
	@Test
	public void test() {
		
		Global.destroyContext();
		
		Assert.assertNotNull(Global.getContext());
		
		Global.destroyContext();
	}
}
