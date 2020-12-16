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

public class DemoJob extends AbstractJob {
	
	protected boolean needSuccessLog() {
		return true;
	}

	protected String excuteJob(String jobConfig, Long jobID) {
		// do nothing
		
		System.out.println("DemoJob excuting......");
		return "done";
	}

}
