/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.ArrayLoginCustomizer;
import com.boubei.tss.framework.sso.DemoOperator;
import com.boubei.tss.framework.sso.DemoUserIdentifier;
import com.boubei.tss.framework.sso.DoErrorLoginCustomizer;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.ILoginCustomizer;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
import com.boubei.tss.framework.sso.PWDOperator;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.sso.online.OnlineUserManagerFactory;
import com.boubei.tss.framework.web.filter.Filter4AutoLogin;
 
public class AutoLoginFilterTest {

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    private MockHttpSession session;

    private FilterChain chain;

    private Filter4AutoLogin filter;

    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        session = new MockHttpSession();
        
        request.setSession(session);
        request.addHeader(RequestContext.USER_REAL_IP, "192.168.0.12");
        request.setCharacterEncoding("UTF-8");
        
        Context.initApplicationContext(MockApplicationContext.getDefaultApplicationContext());
       
        filter = new Filter4AutoLogin();
        FilterConfig filterConfig = new MockFilterConfig();
		filter.init(filterConfig );
        
        chain = new MockFilterChain();
        
        String className = Config.getAttribute(SSOConstants.LOGIN_COSTOMIZER);
        LoginCustomizerFactory.customizer = new ArrayLoginCustomizer(className.split(","));
    }
 
    @After
    public void tearDown() throws Exception {
        request = null;
        response = null;
        session = null;
        LoginCustomizerFactory.customizer = null;
    }
 
    @Test
    public void testDoFilter4Pass() throws IOException, ServletException {
        String token = "token";
        session.setAttribute(RequestContext.USER_TOKEN, token);
        
        Cookie[] cookies = new Cookie[] { new Cookie(RequestContext.USER_TOKEN, token) };
        request.setCookies(cookies);
        
        PWDOperator testPasswordOperator = new PWDOperator(new Long(1));
        IdentityCard card = new IdentityCard(token, testPasswordOperator);
        session.setAttribute(RequestContext.IDENTITY_CARD, card);
        
        Context.initRequestContext(request);
        Context.initIdentityInfo(card);
        Context.setResponse(response);
        
        filter.doFilter(request, response, chain);
        assertNotNull(Context.getRequestContext().getIdentityCard());
        assertEquals(testPasswordOperator.getLoginName(), Environment.getUserCode());
        assertEquals(MockFilterChain.RESPONSE_BODY_STRING, response.getContentAsString());
    }
 
    @Test
    public void testDoFilter4AnonymousLogin() throws IOException, ServletException {
        request.addHeader(RequestContext.ANONYMOUS_REQUEST, "true"); // 允许匿名
        Context.initRequestContext(request);
        filter.doFilter(request, response, chain);

        assertNotNull(Context.getRequestContext().getIdentityCard());
        assertEquals(Anonymous.one.getLoginName(), Environment.getUserCode());
        assertEquals(MockFilterChain.RESPONSE_BODY_STRING, response.getContentAsString());
    }
 
    @Test
    public void testDoFilter4AnonymousException() throws IOException, ServletException {
        Context.initRequestContext(request);
        try {
            filter.doFilter(request, response, chain);
            fail("应该发生异常而没有发生");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
 
    @Test
    public void testDoFilter4OnlineException2Anonymous() throws IOException, ServletException {
        request.addHeader(RequestContext.ANONYMOUS_REQUEST, "true");
        
        String token = "token";
        Cookie[] cookies = new Cookie[] { new Cookie(RequestContext.USER_TOKEN, token) };
        request.setCookies(cookies);
        
        Context.initRequestContext(request);
        filter.doFilter(request, response, chain);
        
        assertEquals(Anonymous.one.getLoginName(), Environment.getUserCode());
        assertEquals(MockFilterChain.RESPONSE_BODY_STRING, response.getContentAsString());
    }
 
    @Test
    public void testDoFilter4OnlineException2AnonymousException() throws IOException, ServletException {
        String token = "token";
        Cookie[] cookies = new Cookie[] { new Cookie(RequestContext.USER_TOKEN, token) };
        request.setCookies(cookies);
        Context.initRequestContext(request);
        try {
            filter.doFilter(request, response, chain);
            fail("应该发生异常而没有发生");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
 
    @Test
    public void testDoFilter4OnlineOk() throws IOException, ServletException {
        request.addHeader(RequestContext.ANONYMOUS_REQUEST, "true");
        
        IOperator operator = new DemoOperator(new Long(2));
        String token = TokenUtil.createToken("sessionId", operator.getId());
        Cookie[] cookies = new Cookie[] { new Cookie(RequestContext.USER_TOKEN, token) };
        request.setCookies(cookies);
        
        Context.initRequestContext(request);
        
        MockOnlineUserManagerFactory.init();
        MockIdentityGetterFactory.init();
        
        OnlineUserManagerFactory.getManager().register(token, "appCode", "sessionId", new Long(3), null);
        
        filter.doFilter(request, response, chain);
        
        assertNotNull(Context.getToken());
        assertEquals(new Long(operator.getId().longValue() + 1), Environment.getUserId());
        assertEquals(MockFilterChain.RESPONSE_BODY_STRING, response.getContentAsString());
    }
 
    @Test
    public void testDoFilter4LoginException() {
        String token = "token";
        Cookie[] cookies = new Cookie[] { new Cookie(RequestContext.USER_TOKEN, token) };
        request.setCookies(cookies);
        request.addHeader("identifier", MockUserIdentifier.class.getName());
        Context.initRequestContext(request);
        try {
            filter.doFilter(request, response, chain);
            fail("应该发生异常而没有发生");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
 
    @Test
    public void testDoFilter4LoginOk() throws IOException, ServletException {
        request.addHeader("identifier", DemoUserIdentifier.class.getName());
        Context.initRequestContext(request);
        Context.setResponse(response);
        
        filter.doFilter(request, response, chain);

        assertNotNull(Context.getRequestContext().getIdentityCard());
        assertEquals(Anonymous.one.getLoginName(), Environment.getUserCode());
        assertEquals(MockFilterChain.RESPONSE_BODY_STRING, response.getContentAsString());
    }
    
    @Test
    public void testDoFilter4LoginCustomizeError() throws IOException, ServletException {
    	LoginCustomizerFactory.destroy();
    	
    	ILoginCustomizer x = LoginCustomizerFactory.customizer;

        LoginCustomizerFactory.customizer = new ArrayLoginCustomizer(DoErrorLoginCustomizer.class.getName().split(","));
    	
        request.addHeader("identifier", DemoUserIdentifier.class.getName());
        request.setServletPath("/tss/index.portal");
        
        Context.initRequestContext(request);
        
        try {
            filter.doFilter(request, response, chain);
            fail("应该发生异常而没有发生");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        try {
            filter.doFilter(request, response, chain);
            fail("应该发生异常而没有发生");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        LoginCustomizerFactory.customizer = x;
    }
}
