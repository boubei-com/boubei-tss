/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.RequestContext;

/**
 * 单个的Mock对象，利用静态导入EasyMock，通过createMock(interfaceName.class)
 * 多个Mock对象，通过ImocksControl管理。
 */
public class Filter7AccessingCheckTest {

    private Filter7AccessingCheck filter;

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
        EasyMock.expect(request.getHeader(RequestContext.USER_REAL_IP)).andReturn("127.0.0.1").times(0, 3);
        EasyMock.expect(request.getContextPath()).andReturn("/tss").times(0, 3);
        
		EasyMock.expect(
				session.getAttribute(SSOConstants.USER_RIGHTS_L))
				.andReturn(Arrays.asList(new Long[] { -100L, 2L, 3L })).times(0, 3);
		EasyMock.expect(
				session.getAttribute(SSOConstants.USER_ROLES_L))
				.andReturn(Arrays.asList(new String[] { "角色一", "角色二", "角色三" })).times(0, 3);
        
        filter = new Filter7AccessingCheck();
    }
 
    @Test
    public final void testDenied() throws IOException, ServletException {
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/login.do");
		EasyMock.expect(request.getServletPath()).andReturn("/login.do");
		EasyMock.expect(request.getSession(false)).andReturn(session).times(0, 8);
		
		response.sendRedirect("/tss/_404.html");
		EasyMock.expectLastCall();
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testCheckOK() throws IOException, ServletException {
    	EasyMock.expect(request.getRequestURI()).andReturn("/tss/login.do").times(0, 3);
    	EasyMock.expect(request.getServletPath()).andReturn("/login.do").times(0, 3);
    	EasyMock.expect(request.getSession(false)).andReturn(session).times(0, 8);
    	EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("J.K").times(0, 3);
    	
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testCheckSessionIsNull() throws IOException, ServletException {
    	EasyMock.expect(request.getRequestURI()).andReturn("/tss/login.html").times(0, 3);
    	EasyMock.expect(request.getServletPath()).andReturn("/login.html").times(0, 3);;
    	EasyMock.expect(request.getSession(false)).andReturn( null ).times(0, 8);
    	
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testCheckSessionIsNull2() throws IOException, ServletException {
    	EasyMock.expect(request.getRequestURI()).andReturn("/tss/modules/_param/param.htm").times(0, 3);
    	EasyMock.expect(request.getServletPath()).andReturn("/modules/_param/param.htm").times(0, 3);;
    	EasyMock.expect(request.getSession(false)).andReturn( null ).times(0, 8);
    	
    	response.sendRedirect("/tss/_404.html");
		EasyMock.expectLastCall();
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public void testAccessingChecker() {
    	 AccessingChecker checker = AccessingChecker.getInstance();
    	 Assert.assertNotNull( checker.get404URL() );
    	 
    	 Assert.assertFalse( checker.checkPermission(null, "/login.do") ); 
    	 
    	 Assert.assertTrue( checker.checkPermission(null, "/modules/dm/datasource.html") );
    	 
    	 List<Object> rights = new ArrayList<Object>();
    	 rights.add("anonymous");
    	 Assert.assertTrue( checker.checkPermission(rights, "/modules/dm/datasource.html") );
    	 
    	 rights.add(-1);
    	 rights.add("Admin");
		 Assert.assertTrue( checker.checkPermission(rights, "/modules/dm/datasource.html") );
    	 
    	 AccessingChecker.rightsMap.clear();
    	 Assert.assertTrue( checker.checkPermission(null, "/login.do") );
    	 
    	 AccessingChecker.rightsMap = null;
    	 Assert.assertTrue( checker.checkPermission(null, "/login.do") );
    }
}
