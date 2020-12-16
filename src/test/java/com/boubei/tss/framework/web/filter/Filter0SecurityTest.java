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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.RequestContext;

/**
 * 单个的Mock对象，利用静态导入EasyMock，通过createMock(interfaceName.class)
 * 多个Mock对象，通过ImocksControl管理。
 */
public class Filter0SecurityTest {

    private Filter0Security filter;

    private IMocksControl mocksControl;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private HttpSession session;

    @Before
    public void setUp() throws Exception {
        mocksControl = EasyMock.createControl();
        
        request = mocksControl.createMock(HttpServletRequest.class);
        response = mocksControl.createMock(HttpServletResponse.class);
        session = mocksControl.createMock(HttpSession.class);
        
        EasyMock.expect(request.getSession()).andReturn(session).times(0, 8);
        EasyMock.expect(request.getSession(false)).andReturn(session).times(0, 8);
        EasyMock.expect(request.getContextPath()).andReturn("/tss").times(0, 3);
        
		EasyMock.expect(request.getServerName()).andReturn("www.boubei.com");
		EasyMock.expect(request.getParameter("uName")).andReturn(null).anyTimes();
		EasyMock.expect(request.getParameter("uSign")).andReturn(null).anyTimes();
		EasyMock.expect(request.getParameter("uToken")).andReturn(null).anyTimes();
		EasyMock.expect(request.getAttribute("apiCall")).andReturn(null).anyTimes();
		
		EasyMock.expect(request.getParameterMap()).andReturn(new HashMap<String, String[]>()).anyTimes();
		EasyMock.expect(request.getCookies()).andReturn(new Cookie[]{}).anyTimes();
		EasyMock.expect(request.getQueryString()).andReturn(null).anyTimes();
		EasyMock.expect(request.getHeader("http-client")).andReturn(null).anyTimes();
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ID)).andReturn(-1L).atLeastOnce();
        
        filter = new Filter0Security();
    }
 
    @Test
    public final void testPass() throws IOException, ServletException {
//    	EasyMock.expect(request.getParameter("uName")).andReturn("ABCDAFAFASFASD");
    	EasyMock.expect(session.getAttribute(RequestContext.USER_TOKEN)).andReturn("hahaha");
    	
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://www.boubei.com/tss/index.html");
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/login.html");
		EasyMock.expect(request.getServletPath()).andReturn("/login.html");
		
		response.sendRedirect("/404.html");
		EasyMock.expectLastCall();
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testPass2() throws IOException, ServletException {
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://www.boubei.com/tss/index.html");
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/index.html").anyTimes();
		EasyMock.expect(request.getServletPath()).andReturn("/index.html");
		EasyMock.expect(session.getAttribute(RequestContext.USER_TOKEN)).andReturn("hahaha");
		EasyMock.expect(session.getAttribute("admin_su")).andReturn(null);
		
		/* 没有返回值void方法的mock方式 */
		response.sendRedirect("/tss/modules/um/_password.htm?flag=0&origin=/tss/index.html");
		EasyMock.expectLastCall();
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testPass22() throws IOException, ServletException {
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://www.boubei.com/tss/index.html");
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/index.html").anyTimes();
		EasyMock.expect(request.getServletPath()).andReturn("/index.html");
		EasyMock.expect(session.getAttribute(RequestContext.USER_TOKEN)).andReturn("hahaha");
		EasyMock.expect(session.getAttribute("admin_su")).andReturn(1);
		
		/* 没有返回值void方法的mock方式 */
		response.sendRedirect("/tss/modules/um/_password.htm?flag=0&origin=/tss/index.html");
		EasyMock.expectLastCall();
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testPass21() throws IOException, ServletException {
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://www.boubei.com/tss/index.html");
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/_password.htm").anyTimes();
		EasyMock.expect(request.getServletPath()).andReturn("/_password.htm"); // _password.htm 被默认放行
		EasyMock.expect(session.getAttribute(RequestContext.USER_TOKEN)).andReturn("hahaha");
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testPass3() throws IOException, ServletException {
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://www.boubei.com/tss/index.html").anyTimes();
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/data/json/122");
		EasyMock.expect(request.getServletPath()).andReturn("/data/json/122");
		EasyMock.expect(request.getHeader("REQUEST-TYPE")).andReturn("xmlhttp");
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testPass4() throws IOException, ServletException {
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://www.boubei.com/tss/index.html").anyTimes();
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/data/export/122/1/1000000");
		EasyMock.expect(request.getServletPath()).andReturn("/data/export/122/1/1000000");
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testPass5() throws IOException, ServletException {
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://www.boubei.com/tss/index.html");
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/reset.xxx");
		EasyMock.expect(request.getServletPath()).andReturn("/reset.xxx");
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    @Test
    public final void testDenyByCheckSession() throws IOException, ServletException {
    	EasyMock.expect(request.getHeader("referer")).andReturn("http://127.0.0.1/tss/index.html").anyTimes();
		EasyMock.expect(request.getRequestURI()).andReturn("/tss/data/json/122");
		EasyMock.expect(request.getServletPath()).andReturn("/data/json/122");
		EasyMock.expect(request.getHeader("REQUEST-TYPE")).andReturn("http");
		EasyMock.expect(session.getAttribute(RequestContext.USER_TOKEN)).andReturn(null);
		
		_TestUtil.mockRequest(request, session);
		
		PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(System.out));
		EasyMock.expect(response.getWriter()).andReturn( printWriter ).atLeastOnce();
		
		/* 没有返回值void方法的mock方式 */
		response.setContentType("application/json;charset=UTF-8");
		response.sendRedirect("/tss/404.html");
		EasyMock.expectLastCall().anyTimes();
		
		EasyMock.expect(session.getAttribute(SSOConstants.USER_ACCOUNT)).andReturn("Jane").times(0, 3);
		
        mocksControl.replay(); // 让 mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    /**
     * 跨域白名单检查1：{\"code\": \"TSS-404\", \"errorMsg\": \"资源不存在或限制访问\"}
     */
    @Test
    public final void testDenyTSS404() throws IOException, ServletException {
    	
		EasyMock.expect(request.getHeader("referer")).andReturn("http://www.12306.com/tss/index.html").anyTimes();
		EasyMock.expect(request.getServletPath()).andReturn("/data/json/122");
		EasyMock.expect(response.getWriter()).andReturn( new PrintWriter(new OutputStreamWriter(System.out)) );;
		
		/* 没有返回值void方法的mock方式 */
		response.setContentType("application/json;charset=UTF-8");
		response.sendRedirect("/tss/404.html");
		EasyMock.expectLastCall();
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
    
    /**
     * 跨域白名单检查2
     */
    @Test
    public final void testDenyHTML404() throws IOException, ServletException {
		EasyMock.expect(request.getHeader("referer")).andReturn("http://www.12306.com/tss/index.html").anyTimes();
		EasyMock.expect(request.getServletPath()).andReturn("/xxx.html");
		EasyMock.expect(response.getWriter()).andReturn( new PrintWriter(new OutputStreamWriter(System.out)) );;
		
		/* 没有返回值void方法的mock方式 */
		response.setContentType("application/json;charset=UTF-8");
		response.sendRedirect("/tss/404.html");
		EasyMock.expectLastCall();
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
        
        
        HttpServletRequest req = new MockHttpServletRequest();
		filter.log404Context(req , "/cache/grid", session);
    }
    
    @Test
    public final void testIgnoreRefer() throws IOException, ServletException {
    	// report_portlet.html 页面忽略盗链跨域检查
    	EasyMock.expect(request.getServletPath()).andReturn("/tss/modules/dm/report_portlet.html");
		EasyMock.expect(request.getHeader("referer")).andReturn("http://www.12306.com/tss/index.html").atLeastOnce();
		
		/* 没有返回值void方法的mock方式 */
		response.sendRedirect("/tss/404.html");
		EasyMock.expectLastCall();
		
        mocksControl.replay(); // 让mock 准备重放记录的数据

        filter.doFilter(request, response, new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            }
        });
    }
}
