/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cache;

import com.boubei.tss.cache.extension.workqueue.TaskPoolCustomizer;

public class ScannerTaskPoolCustomizer extends TaskPoolCustomizer {

	protected String getTaskClass() {
		return ScannerTask.class.getName();
	}

}
