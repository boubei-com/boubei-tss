/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web;

import javax.servlet.http.Cookie;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.framework.web.wrapper.XHttpServletRequestWrapper;

import junit.framework.Assert;

public class XHttpServletRequestWrapperTest {
	
	@Test
	public void test() {
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("h0", "h0");
		
		Cookie cookie = new Cookie("c1", "%22abcd");
		request.setCookies(cookie);
		
		XHttpServletRequest r = XHttpServletRequestWrapper.wrapRequest(request);
		
		r.addParameter("p1", "x");
		Assert.assertEquals("x", r.getParameterValues("p1")[0]);
		Assert.assertEquals("x", r.getParameter("p1"));
		
		r.setHeader("h1", "y");
		Assert.assertEquals("y", r.getHeader("h1"));
		Assert.assertEquals("y", r.getHeaders("h1").nextElement());
		Assert.assertEquals("h0", r.getHeaders("h0").nextElement());
		
		r.getCookies();
	}

}
