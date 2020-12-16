/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules;

import org.junit.Test;

import com.boubei.tss.AbstractTest4TSS;

public class HitRateManagerTest extends AbstractTest4TSS {
	
	@Test
	public void test() {
		for(int i = 0; i < 10; i++) {
			HitRateManager.getInstanse("dm_record_attach").output(1);
			HitRateManager.getInstanse("cms_article").output(1);
			HitRateManager.getInstanse("cms_attachment").output(1);
		}
		
		try {
			Thread.sleep(3*1000);
		} catch (InterruptedException e) {
		}
	}

}
