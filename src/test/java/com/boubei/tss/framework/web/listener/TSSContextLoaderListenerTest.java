/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.listener;

import javax.servlet.ServletContextEvent;

import org.junit.Test;

public class TSSContextLoaderListenerTest {
	
	@Test
	public void test() {
		TSSContextLoaderListener l = new TSSContextLoaderListener();
		
		ServletContextEvent sce = null;
		
		try {
			l.contextInitialized(sce);
		} 
		catch(Exception e) { }
		
		try {
			l.contextDestroyed(sce);
		} 
		catch(Exception e) { }
	}

}
