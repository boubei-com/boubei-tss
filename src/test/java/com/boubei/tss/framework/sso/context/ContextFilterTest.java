/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.web.XHttpServletRequest;
import com.boubei.tss.framework.web.filter.Filter3Context;

/**
 * <p>
 * 上下文信息处理Filter测试类
 * </p>
 * 
 * 单个的Mock对象，利用静态导入EasyMock，通过createMock(interfaceName.class)
 * 多个Mock对象，通过ImocksControl管理。
 */
public class ContextFilterTest {

    private Filter3Context filter;

    private IMocksControl mocksControl;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private HttpSession session;

    @Before
    public void setUp() throws Exception {
        mocksControl = EasyMock.createControl();
        
        request = mocksControl.createMock(HttpServletRequest.class);
        response = mocksControl.createMock(HttpServletResponse.class);
        session = mocksControl.createMock(HttpSession.class); // EasyMock.createMock(HttpSession.class);
        
        EasyMock.expect(request.getSession()).andReturn(session).times(0, 8);
        EasyMock.expect(request.getHeader(RequestContext.USER_REAL_IP)).andReturn("127.0.0.1").times(0, 8);
        
        EasyMock.expect(request.getContextPath()).andReturn("/tss");
        EasyMock.expect(request.getRequestURI()).andReturn("/tss/login.do");
        EasyMock.expect(request.getServletPath()).andReturn("/login.do");
        
        EasyMock.expect(session.getId()).andReturn("1").times(0, 3);
        
        filter = new Filter3Context();
    }

    @After
    public void tearDown() throws Exception {
        Context.destroy();
    }

    /**
     * Test method for
     * {@link com.boubei.tss.core.web.filter.Filter3Context#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}.
     */
    @Test
    public final void testDoFilterNotLogin() throws IOException, ServletException {
        EasyMock.expect(session.getAttribute(RequestContext.USER_TOKEN)).andReturn(null).times(0, 3); // 无token
        EasyMock.expect(session.getAttribute(RequestContext.IDENTITY_CARD)).andReturn(null).atLeastOnce(); // 无card

        mocksControl.replay();

        filter.doFilter(request, response, new FilterChain() {
            // not Login
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;

                assertTrue(Context.getRequestContext().getRequest() instanceof HttpServletRequest);
                assertEquals(httpServletRequest.getSession(), Context.getRequestContext().getSession());
                assertNull(Context.getRequestContext().getIdentityCard());

            }
        });
        assertNull(Context.getRequestContext());
    }

    /**
     * Test method for
     * {@link com.boubei.tss.core.web.filter.Filter3Context#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}.
     */
    @Test
    public final void testDoFilter4Login() throws IOException, ServletException {
        EasyMock.expect(session.getAttribute(RequestContext.USER_TOKEN)).andReturn("token").times(0, 3);
        final IdentityCard identityCard = new IdentityCard("token", Anonymous.one);
        EasyMock.expect(session.getAttribute(RequestContext.IDENTITY_CARD)).andReturn(identityCard).times(0, 18);
        
        Enumeration<String> e = new Enumeration<String>() {
			public String nextElement() {
				return null;
			}
			public boolean hasMoreElements() {
				return false;
			}
		};
        EasyMock.expect(request.getHeaders(RequestContext.USER_REAL_IP)).andReturn(e).times(0, 8);	
        EasyMock.expect(request.getParameterMap()).andReturn(new HashMap<String, String[]>()).times(0, 8);	

        EasyMock.expect(request.getParameter("userName")).andReturn("J.K").times(0, 3);
        
        session.setAttribute(RequestContext.IDENTITY_CARD, identityCard);

        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            // Login
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;

                XHttpServletRequest requestWrap = Context.getRequestContext().getRequest();
				assertTrue(requestWrap instanceof HttpServletRequest);
                assertEquals(httpServletRequest.getSession(), Context.getRequestContext().getSession());
                assertEquals(identityCard, Context.getRequestContext().getIdentityCard());
                
                assertEquals("127.0.0.1", requestWrap.getHeader(RequestContext.USER_REAL_IP));
                assertEquals("J.K", requestWrap.getParameter("userName"));
                
                assertEquals(false, requestWrap.getHeaders(RequestContext.USER_REAL_IP).hasMoreElements());
                assertEquals(false, requestWrap.getParameterNames().hasMoreElements());
                assertNull( requestWrap.getParameterValues("userName") );
                requestWrap.addCookie(new Cookie("test", "test"));
            }

        });
        assertNull(Context.getRequestContext());
    }
}
