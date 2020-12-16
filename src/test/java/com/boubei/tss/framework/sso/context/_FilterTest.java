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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterConfig;

import com.boubei.tss.framework.web.filter.Filter3Context;
import com.boubei.tss.framework.web.filter.Filter4AutoLogin;
import com.boubei.tss.framework.web.filter.Filter8APITokenCheck;

/**
 * 单个的Mock对象，利用静态导入EasyMock，通过createMock(interfaceName.class)
 * 多个Mock对象，通过ImocksControl管理。
 */
public class _FilterTest {

    private Filter filter;

    private IMocksControl mocksControl;

    private HttpServletRequest request;

    private HttpServletResponse response;
    
    MockFilterConfig filterConfig;

    @Before
    public void setUp() throws Exception {
        mocksControl = EasyMock.createControl();
        
        request = mocksControl.createMock(HttpServletRequest.class);
        response = mocksControl.createMock(HttpServletResponse.class);
        filterConfig = new MockFilterConfig();
        filterConfig.addInitParameter("ignorePaths", "/remote/OnlineUserService,js,htm,html,jpg,png,gif,ico,css,xml,swf");
        
        EasyMock.expect(request.getContextPath()).andReturn("/tss");
        EasyMock.expect(request.getRequestURI()).andReturn("/tss/login.html");
        EasyMock.expect(request.getServletPath()).andReturn("/login.html");
    }

    @After
    public void tearDown() throws Exception {
        Context.destroy();
    }
 
    @Test
    public final void testFilter3Context() throws IOException, ServletException {
        mocksControl.replay();
        
        filter = new Filter3Context();
        filter.init(filterConfig);
        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {

            }
        });
    }
    
    @Test
    public final void testFilter4AutoLogin() throws IOException, ServletException {
        mocksControl.replay();
        
        filter = new Filter4AutoLogin();
        filter.init(filterConfig);
        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {

            }
        });
    }
    
    @Test
    public final void testFilter8APITokenCheck() throws IOException, ServletException {
        mocksControl.replay();
        
        filter = new Filter8APITokenCheck();
        filter.init(filterConfig);
    }
}
