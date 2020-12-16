/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.identity.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.boubei.tss.framework.exception.UserIdentificationException;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.IUserIdentifier;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.appserver.AppServer;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.sso.identifier.AnonymousUserIdentifier;
import com.boubei.tss.framework.sso.identity.MockAppServerStorer;
import com.boubei.tss.framework.sso.identity.MockApplicationContext;
import com.boubei.tss.framework.sso.identity.MockOnlineUserManagerFactory;
import com.boubei.tss.framework.sso.online.OnlineUserManagerFactory;
 
public class AnonymousUserIdentifierTest {
    
    private IUserIdentifier identifier;

    private MockHttpServletRequest request;
 
    @Before
    public void setUp() throws Exception {
        MockOnlineUserManagerFactory.init();
        
        AppServer server = new AppServer();
        server.setCode("Core");
        server.setName("Core");
        MockAppServerStorer storer = new MockAppServerStorer();
        storer.putAppServer("Core", server);
        
        Context.initApplicationContext(new MockApplicationContext(storer, "Core"));
        
        request = new MockHttpServletRequest();
        request.setCharacterEncoding("UTF-8");
        request.setSession(new MockHttpSession());
        
        Context.initRequestContext(request);
        
        identifier = new AnonymousUserIdentifier();
    }
 
    public void tearDown() throws Exception {
        identifier = null;
        request = null;
    }

    @Test
    public void testIdentify4Ok() {
        request.addHeader(RequestContext.ANONYMOUS_REQUEST, "true"); // 允许匿名访问
        IdentityCard card = null;
        try {
            card = identifier.identify();
        } catch (UserIdentificationException e) {
            fail("UserIdentificationException");
        }
        assertNotNull(card);
        assertEquals(Anonymous.one.getId(), card.getId());
        assertEquals(Anonymous.one.getUserName(), card.getUserName());
        assertTrue(OnlineUserManagerFactory.getManager().isOnline(card.getToken()));
        
        IOperator anonymousOperator = card.getOperator();
        assertTrue(anonymousOperator instanceof Anonymous);
        assertTrue( ((Anonymous)anonymousOperator).isAnonymous() );
        assertTrue( ((Anonymous)anonymousOperator).getAttributesMap().isEmpty() );
    }

    @Test
    public final void testIdentify4Fail() {
        IdentityCard card = null;
        try {
            card = identifier.identify();
            fail("应该发生异常，但没有发生");
        } catch (UserIdentificationException e) {
            assertTrue(true);
        }
        assertNull(card);
    }
}
