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

public class Demo2Job extends AbstractJob {

	protected String excuteJob(String jobConfig, Long jobID) {
		// do nothing
		
		System.out.println("Demo2Job excuting......");
		return "done";
	}

}
