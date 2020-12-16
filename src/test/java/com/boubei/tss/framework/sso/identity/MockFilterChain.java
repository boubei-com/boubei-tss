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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 返回相关数据
 * </p>
 */
public class MockFilterChain implements FilterChain {

    public static final String TEST_COOKIE_NAME = "testCookieName";
    public static final String TEST_COOKIE_VALUE = "testCookieValue";

    public static final String TEST_HEADER_NAME = "testHeaderName";
    public static final String TEST_HEADER_VALUE = "testHeaderValue";

    public static final String RESPONSE_BODY_STRING = "It works";
 
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        
        HttpServletResponse res = (HttpServletResponse) response;
        res.setHeader(TEST_HEADER_NAME, TEST_HEADER_VALUE);
        res.addCookie(new Cookie(TEST_COOKIE_NAME, TEST_COOKIE_VALUE));
        res.addCookie(new Cookie(TEST_COOKIE_NAME, TEST_COOKIE_VALUE));
        
        res.getWriter().print(RESPONSE_BODY_STRING);
        
        System.out.println("servlet running");
    }

}
