/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.appserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.framework.exception.BusinessException;

/**
 * 测试应用服务器文件管理器（绝对路径配置方式）
 */
public class FileAppServerStorerTest {
	
	Logger log = Logger.getLogger(this.getClass());  
	
    IAppServerStorer storer = new FileAppServerStorer();
    
    @Test
    public void testAppStore() {
    	assertEquals(4, storer.getAppServers().size());
    }
 
    @Test
    public void testGetAppServer() {
        AppServer server = storer.getAppServer("TSS");
        assertEquals("TSS", server.getCode());
        assertEquals("http://localhost:8011/tss", server.getBaseURL());
        assertEquals("localhost", server.getDomain());
        assertEquals("TSS", server.getName());
        assertEquals("/tss", server.getPath());
        assertEquals("JSESSIONID", server.getSessionIdName());
        
		try {
			storer.getAppServer("NOT_EXISTS");
			Assert.fail();
		} catch(Exception e) {
			log.debug(e.getMessage());
		}

        try {
            storer.getAppServer("CMS");
        } catch (BusinessException e) {
            assertTrue(true);
        }
    }
}
