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
import static org.junit.Assert.assertTrue;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss._TestUtil;
 
public class RequestContextTest {

    private IMocksControl mocksControl;

    private HttpServletRequest request;
    private HttpSession session;

    private RequestContext context;
 
    @Before
    public void setUp() throws Exception {
        mocksControl =  EasyMock.createControl();
        request = mocksControl.createMock(HttpServletRequest.class);
        
        session = mocksControl.createMock(HttpSession.class);
        EasyMock.expect(request.getSession()).andReturn(session).times(0, 18);
        EasyMock.expect(session.getAttribute(RequestContext.IDENTITY_CARD)).andReturn(null).times(0, 18);
        
        context = new RequestContext(request);
    }
 
    @Test
    public void testGetClientIpFromHeader() {
    	_TestUtil.mockRequest(request, session);
    	EasyMock.expect(request.getContextPath()).andReturn("/tss").times(0, 3);
        EasyMock.expect(session.getId()).andReturn(null).times(0, 3);
        
        mocksControl.replay(); 
        assertEquals("127.0.0.1", context.getClientIp());
        Assert.assertNull(context.getSessionId());
        Assert.assertNull(context.getIdentityCard());
    }
 
    @Test
    public void testCanAnonymous4Header() {
        EasyMock.expect(request.getHeader(RequestContext.ANONYMOUS_REQUEST)).andReturn("true").atLeastOnce();
        EasyMock.expect(request.getCharacterEncoding()).andReturn("UTF-8").atLeastOnce();
 
        mocksControl.replay(); 
        assertTrue(context.canAnonymous());
    }
 
    @Test
    public void testCanAnonymous4Parameter() {
        EasyMock.expect(request.getHeader(RequestContext.ANONYMOUS_REQUEST)).andReturn("").atLeastOnce();
        EasyMock.expect(request.getParameter(RequestContext.ANONYMOUS_REQUEST)).andReturn("true").atLeastOnce();
        EasyMock.expect(request.getCharacterEncoding()).andReturn("UTF-8").atLeastOnce();
 
        mocksControl.replay(); 
        assertTrue(context.canAnonymous());
    }
 
    @Test
    public void testGetUserToken4Cookie() {
        EasyMock.expect(request.getHeader(RequestContext.USER_TOKEN)).andReturn(null).atLeastOnce();
        EasyMock.expect(request.getParameter(RequestContext.USER_TOKEN)).andReturn("").atLeastOnce();
        
        String token = "1234567890";
        Cookie[] cookies = new Cookie[2];
        cookies[0] = new Cookie("test", "test");
        cookies[1] = new Cookie(RequestContext.USER_TOKEN, token);
        EasyMock.expect(request.getCookies()).andReturn(cookies).atLeastOnce();
 
        mocksControl.replay(); 
        assertEquals(token, context.getUserToken());
    }
    
    @Test
    public void testCheckAPICall() {
        EasyMock.expect(request.getAttribute(RequestContext.API_CALL)).andReturn("true").atLeastOnce();
 
        mocksControl.replay(); 
        assertTrue(context.isApiCall());
    }
}
