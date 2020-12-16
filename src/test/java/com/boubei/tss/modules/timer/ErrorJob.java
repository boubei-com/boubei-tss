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

import com.boubei.tss.framework.exception.BusinessException;

public class ErrorJob extends AbstractJob {
	
	protected boolean needSuccessLog() {
		return true;
	}

	protected String excuteJob(String jobConfig, Long jobID) {
		// do nothing
		
		throw new BusinessException("ErrorJob excuting......");
	}

}
