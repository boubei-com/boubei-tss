/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.online;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * 内存管理方式在线用户管理系统
 */
public class CacheOnlineUserManagerTest {
    
    protected Logger log = Logger.getLogger(this.getClass());    
 
    @Test
    public void testDBOnlineUserManager() {
    	CacheOnlineUserManager manager = new CacheOnlineUserManager();
        manager.register("token", "TSS",  "sessionId",  new Long(1), "Jon.King");
        manager.register("token", "TSS1", "sessionId1", new Long(1), "Jon.King");
        manager.register("token", "TSS2", "sessionId2", new Long(2), "Jon.King2");
        manager.register("token3", "TSS", "sessionId3", new Long(3), "Jon.King3");

        Set<OnlineUser> userSet = manager.tokenMap.get("token");
        assertNotNull(userSet);
        assertEquals(3, userSet.size());
        
        // testGetAllOnlineInfos4Token
        Set<OnlineUser> userInfos = manager.tokenMap.get("token");
        assertEquals(3, userInfos.size());
        assertTrue(userInfos.contains(new OnlineUser(new Long(1), "TSS", "sessionId", "token")));
        assertTrue(userInfos.contains(new OnlineUser(new Long(2), "TSS2", "sessionId2", "token")));
        assertTrue(userInfos.contains(new OnlineUser(new Long(1), "TSS1", "sessionId1", "token")));
        
        assertFalse(new OnlineUser(new Long(1), "TSS1", "sessionId1", "token").equals(null));
        
        assertNull(manager.tokenMap.get("notToken"));
        
        // testIsOnline
        assertTrue(manager.isOnline("token"));
        assertFalse(manager.isOnline("NotLoginToken"));
        
        // testDelete
        assertEquals(3, manager.tokenMap.get("token").size());

        manager.logout("TSS1", "sessionId1");
        assertEquals(2, manager.tokenMap.get("token").size());
        
        manager.logout("TSS", "sessionId3");
        assertEquals(2, manager.tokenMap.get("token").size());
    }

}
