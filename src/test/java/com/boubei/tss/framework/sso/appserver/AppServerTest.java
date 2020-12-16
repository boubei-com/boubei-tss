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

import org.junit.Assert;
import org.junit.Test;
 
public class AppServerTest  {
 
	@Test
    public void testGetDomain() {
        AppServer server = new AppServer();
        server.setBaseURL("http://localhost:8088");
        assertEquals("localhost", server.getDomain());
        
        server.setBaseURL("http://localhost:8088/");
        assertEquals("localhost", server.getDomain());
        
        server.setBaseURL("http://localhost");
        assertEquals("localhost", server.getDomain());
        
        server.setBaseURL("http://localhost/");
        assertEquals("localhost", server.getDomain());
        
        server.setBaseURL("http://127.0.0.1:8088");
        assertEquals("127.0.0.1", server.getDomain());
        
        server.setBaseURL("http://127.0.0.1:8088/tss");
        assertEquals("127.0.0.1", server.getDomain());
        
        server = new AppServer();
        Assert.assertNull( server.getDomain() );
        Assert.assertNull( server.getPath() );
        
        server.setBaseURL("http://127.0.0.1");
        assertEquals("/", server.getPath());
    }

}

