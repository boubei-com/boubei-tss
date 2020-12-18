/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso.online;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.framework.sso.online.OnlineUser;
import com.boubei.tss.framework.web.XHttpServletRequest;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.action.UserAction;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

public class DBOnlineUserManagerTest extends AbstractTest4UM { 
    
	@Autowired UserAction action;
	
    IOnlineUserManager manager;
    
    public void init() {
    	super.init();
    	manager = (IOnlineUserManager) Global.getBean("DBOnlineUserService");
    }
    
    @Test
    public void testDBOnlineUserManager() {
        
    	manager.register("token_0", "TSS", "sessionId",  new Long(-1), "Admin");
        manager.register("token_1", "TSS", "sessionId",  new Long(1), "Jon.King");
        manager.register("token_1", "TSS", "sessionId",  new Long(1), "Jon.King");
        manager.register("token_1", "TSS", "sessionId1", new Long(1), "Jon.King");
        manager.register("token_1", "TSS", "sessionId2", new Long(2), "Jon.King2");
        manager.register("token3",  "TSS", "sessionId3", new Long(3), "Jon.King3");

        List<?> list = this.getOnlineUsersByToken("token_1");
        assertEquals(2, list.size());
        
        OnlineUser first = (OnlineUser) list.toArray()[0];
        DBOnlineUser second = (DBOnlineUser) list.toArray()[1];
        second.setId((Long) second.getPK());
        
        log.debug(second.getId());
        log.debug(second.getLoginTime());
        log.debug(second.getUserName());
        
        Assert.assertFalse(first.equals(second));
        log.debug(first.hashCode());
        
        // testGetAllOnlineInfos4Token
        List<?> userInfos = commonDao.getEntities("from DBOnlineUser o where o.token = ?1 ", "token_1");
        assertEquals(2, userInfos.size());
        assertTrue(userInfos.contains(new OnlineUser(new Long(2), "TSS", "sessionId2", "token_1")));
        assertTrue(userInfos.contains(new OnlineUser(new Long(1), "TSS", "sessionId1", "token_1")));
        
        assertTrue(this.getOnlineUsersByToken("notToken").isEmpty());
        
        // testIsOnline
        assertTrue(manager.isOnline("token_1"));
        assertFalse(manager.isOnline("NotLoginToken"));
        
        Context.sessionMap.put("sessionId1", new MockHttpSession());
        Context.sessionMap.put("sessionId2", new MockHttpSession());
        Context.sessionMap.put("sessionId3", new MockHttpSession());
        Context.sessionMap.put("xxxxxxx", new MockHttpSession());
        
        Context.sessionMap.get("sessionId1").setAttribute("sessionCTime", DateUtil.subDays(new Date(), 0.1));
        Context.sessionMap.get("sessionId2").setAttribute("sessionCTime", DateUtil.subDays(new Date(), 0.1));
        Context.sessionMap.get("sessionId3").setAttribute("sessionCTime", DateUtil.subDays(new Date(), 0.1));
        Context.sessionMap.get("xxxxxxx").setAttribute("sessionCTime", DateUtil.subDays(new Date(), 0.1));
        
        action.getOnlineUserInfo(response);
        
        // test logout
        assertEquals(2, this.getOnlineUsersByToken("token_1").size());

        manager.logout("TSS1", "sessionId1");
        assertEquals(2, this.getOnlineUsersByToken("token_1").size());
        commonDao.deleteAll(commonDao.getEntities("from DBOnlineUser where sessionId = ?1", "sessionId1"));
        
        manager.logout("TSS", "sessionId3");
        assertEquals(1, this.getOnlineUsersByToken("token_1").size());
        commonDao.deleteAll(commonDao.getEntities("from DBOnlineUser where sessionId = ?1", "sessionId3"));
        
        // test api call 
        XHttpServletRequest request = Context.getRequestContext().getRequest();
        request.addParameter("uName", "JK");
        request.addParameter("uToken", "JK0123456789");
        manager.register("token_1", "TSS", "_sessionId2", new Long(2), "Jon.King2");
        
        List<?> ids = commonDao.getEntities("select sessionId from DBOnlineUser");
        action.deleteOnlineUser(response, EasyUtils.list2Str(ids));
        
        try {
        	new DBOnlineUserService().queryExists(1L, "xxx", true, false, false);
        } 
        catch(Exception e) { }
    }

    private List<?> getOnlineUsersByToken(String token) {
    	return commonDao.getEntities("from DBOnlineUser o where o.token = ?1 ", token);
    }
}
