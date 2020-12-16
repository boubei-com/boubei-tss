/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.timer;

import org.junit.Test;

import com.boubei.tss.framework.sso.Environment;

public class JobTest {

	@Test
	public void test1() {
		new DemoJob().excuteJob("XXX");
		
		System.out.println(Environment.getUserCode());
	}

	@Test
	public void test2() {
		try {
			new ErrorJob().excuteJob("XXX");
		} catch(Exception e) {
			
		}
	}
}
