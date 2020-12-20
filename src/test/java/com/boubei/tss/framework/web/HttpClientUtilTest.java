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

import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Cookie;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.boubei.tss.framework.sso.DemoOperator;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.appserver.AppServer;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;

import org.junit.Assert;

public class HttpClientUtilTest {
	
	protected MockHttpServletResponse response;
    protected MockHttpServletRequest request;
 
	@Test
	public void test() {
		Context.destroy();
		try {
			transmitReturnCookies(null, null);
		} catch(Exception e) { }
        
		request = new MockHttpServletRequest();
		MockHttpSession session = new MockHttpSession();
		request.setSession(session);
		
		Context.initRequestContext(request);
		try {
			transmitReturnCookies(null, null);
		} catch(Exception e) { }
		
		Context.setResponse(response = new MockHttpServletResponse());
		
        request.addParameter(RequestContext.PROXY_REAL_PATH, "/param/json?code=sysTitle");
        
        String token = TokenUtil.createToken(new Random().toString(), 12L); 
        IdentityCard card = new IdentityCard(token, new DemoOperator(12L));
        Context.initIdentityInfo(card);
        
		AppServer targetAppServer = new AppServer();
		targetAppServer.setCode("TSS");
		targetAppServer.setBaseURL("http://localhost:8111/tss");
		targetAppServer.setName("TSS");
		targetAppServer.setSessionIdName("JSESSIONID");
        
		Cookie[] cookies = new Cookie[3];
		cookies[0] = new Cookie("/", "TSS", "TSS");
		cookies[1] = new Cookie("/", "JSESSIONID", "1234567890");
		cookies[2] = new Cookie("/", "XXX", "XXX");
		
		transmitReturnCookies(cookies , targetAppServer );
		
		Assert.assertEquals(2, Context.getRequestContext().getRequest().getCookies().length);
		
		HttpClientUtil.getHttpClient(targetAppServer);
 
		try {
			HttpClientUtil.getHttpMethod(targetAppServer);
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
     * 处理二次转发请求（request2）转发成功后 返回的Cookie信息，将这些cookie设置到初始的请求和响应里
     * @param cookies 
     *            注：是org.apache.commons.httpclient.Cookie
     * @param targetAppServer
     */
    public static void transmitReturnCookies(org.apache.commons.httpclient.Cookie[] cookies, AppServer targetAppServer) {
        RequestContext requestContext = Context.getRequestContext();
        if (requestContext == null) return;
        
        XHttpServletRequest request = requestContext.getRequest();
        HttpServletResponse response = Context.getResponse();
        if (response == null || request == null) return;
        
        // 转发返回Cookies
        for (int i = 0; i < cookies.length; i++) {
            String cookieName = cookies[i].getName();
            
            //如果当前应用本身的cookie，则无需转发
            if (cookieName.equals(Context.getApplicationContext().getCurrentAppCode())) continue; 
            
            if (cookieName.equals(targetAppServer.getSessionIdName())) {
                cookieName = targetAppServer.getCode();
            }
            
            String cpath = request.getContextPath();
            javax.servlet.http.Cookie cookie = HttpClientUtil.createCookie(cookieName, cookies[i].getValue(), cpath);
            cookie.setMaxAge(-1);
            cookie.setSecure(request.isSecure());
            
            if (response.isCommitted()) {
                response.addCookie(cookie);
            }
            
            // 同时也添加到request中，以用于二次、三次的远程接口调用
            request.addCookie(cookie); 
        }
    }
}
