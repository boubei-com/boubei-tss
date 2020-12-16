/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.servlet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.record.file.OrignUploadFile;
import com.boubei.tss.framework.SystemInfo;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.URLUtil;

public class Servlet4UploadTest extends AbstractTest4DM {

	@Autowired SystemInfo si;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
	    request = new MockHttpServletRequest();
	    response = new MockHttpServletResponse();
	    
	    request.addParameter("afterUploadClass", 
	    		new String[] {"com.boubei.tss.framework.web.servlet.MyAfterUpload"});
	}
	
	@Test
    public void testDoPost() {
		request.addParameter("useOrignName", new String[] {"true"});
        Servlet4Upload servlet = new Servlet4Upload();
        try {
        	request.setServletPath("/auth/file/upload");
        	servlet.doPost(new XRequest(request), response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
        	servlet.destroy();
        }
        
        try {
        	request = new MockHttpServletRequest();
        	request.setServletPath("/tss/remote/upload");
        	servlet.doPost(new XRequest(request), response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
        	servlet.destroy();
        }
    }
	
	@Test
    public void testDoUpload1() {
		request.addParameter("useSpecifiedName", new String[] {"xxxx"});
        Servlet4Upload servlet = new Servlet4Upload();
        try {
        	IMocksControl mocksControl = EasyMock.createControl();
        	Part part = mocksControl.createMock(Part.class);
            
            EasyMock.expect(part.getHeader("content-disposition")).andReturn("attachment;filename=\"1234.txt\"");
            
            URL url = URLUtil.getResourceFileUrl("application.properties");
            String log4jPath = url.getPath(); 
            EasyMock.expect(part.getInputStream()).andReturn(new FileInputStream(log4jPath));
            EasyMock.expect(part.getSize()).andReturn(1023000L);
            
            EasyMock.replay(part); // 让mock 准备重放记录的数据
            
			servlet.doUpload(request, part);
        } 
        catch (Exception e) {
        	e.printStackTrace();
        	Assert.fail("Test servlet error:" + e.getMessage());
        } finally {
        	servlet.destroy();
        }
    }
	
	@Test
    public void testDoUpload2() {
        Servlet4Upload servlet = new Servlet4Upload();
        try {
        	IMocksControl mocksControl = EasyMock.createControl();
        	Part part = mocksControl.createMock(Part.class);
            
            EasyMock.expect(part.getHeader("content-disposition")).andReturn("attachment;filename=\"1234.csv\"");
            
            URL url = URLUtil.getResourceFileUrl("application.properties");
            String log4jPath = url.getPath(); 
            EasyMock.expect(part.getInputStream()).andReturn(new FileInputStream(log4jPath));
            EasyMock.expect(part.getSize()).andReturn(1023000L);
            
            EasyMock.replay(part); // 让mock 准备重放记录的数据
            
			servlet.doUpload(request, part);
			
			List<?> list = commonDao.getEntities("from OrignUploadFile");
			OrignUploadFile ogf = (OrignUploadFile) list.get(0);
			
		    response = new MockHttpServletResponse();
			si.downloadAttach(request, response, ogf.getId());
			
			login(Anonymous._CODE);
		    response = new MockHttpServletResponse();
			si.downloadAttach(request, response, ogf.getId());
			
			EasyUtils.obj2Json(ogf);
        } 
        catch (Exception e) {
        	e.printStackTrace();
        	Assert.fail("Test servlet error:" + e.getMessage());
        } finally {
        	servlet.destroy();
        }
    }
	
	class XRequest implements HttpServletRequest {
		
		MockHttpServletRequest request;
		
		public XRequest(MockHttpServletRequest request) {
			this.request = request;
		}
		public AsyncContext getAsyncContext() {
			return null;
		}
		public Object getAttribute(String arg0) {
			return null;
		}
		public Enumeration<String> getAttributeNames() {
			return null;
		}
		public String getCharacterEncoding() {
			return null;
		}
		public int getContentLength() {
			return 0;
		}
		public String getContentType() {
			return null;
		}
		public DispatcherType getDispatcherType() {
			return null;
		}
		public ServletInputStream getInputStream() throws IOException {
			return null;
		}
		public String getLocalAddr() {
			return null;
		}
		public String getLocalName() {
			return null;
		}
		public int getLocalPort() {
			return 0;
		}
		public Locale getLocale() {
			return null;
		}
		public Enumeration<Locale> getLocales() {
			return null;
		}
		public String getParameter(String arg0) {
			return this.request.getParameter(arg0);
		}
		public Map<String, String[]> getParameterMap() {
			return request.getParameterMap();
		}
		public Enumeration<String> getParameterNames() {
			return request.getParameterNames();
		}
		public String[] getParameterValues(String arg0) {
			return request.getParameterValues(arg0);
		}
		public String getProtocol() {
			return null;
		}
		public BufferedReader getReader() throws IOException {
			return null;
		}
		public String getRealPath(String arg0) {
			return null;
		}
		public String getRemoteAddr() {
			return null;
		}
		public String getRemoteHost() {
			return null;
		}
		public int getRemotePort() {
			return 0;
		}
		public RequestDispatcher getRequestDispatcher(String arg0) {
			return null;
		}
		public String getScheme() {
			return null;
		}
		public String getServerName() {
			return null;
		}
		public int getServerPort() {
			return 0;
		}
		public ServletContext getServletContext() {
			return this.request.getServletContext();
		}
		public boolean isAsyncStarted() {
			return false;
		}
		public boolean isAsyncSupported() {
			return false;
		}
		public boolean isSecure() {
			return false;
		}
		public void removeAttribute(String arg0) {
			
		}
		public void setAttribute(String arg0, Object arg1) {
			
		}
		public void setCharacterEncoding(String arg0)
				throws UnsupportedEncodingException {
			
		}
		public AsyncContext startAsync() throws IllegalStateException {
			return null;
		}
		public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1)
				throws IllegalStateException {
			return null;
		}
		public boolean authenticate(HttpServletResponse arg0)
				throws IOException, ServletException {
			return false;
		}
		public String getAuthType() {
			return null;
		}
		public String getContextPath() {
			return null;
		}
		public Cookie[] getCookies() {
			return null;
		}
		public long getDateHeader(String arg0) {
			return 0;
		}
		public String getHeader(String arg0) {
			return null;
		}
		public Enumeration<String> getHeaderNames() {
			return null;
		}
		public Enumeration<String> getHeaders(String arg0) {
			return null;
		}
		public int getIntHeader(String arg0) {
			return 0;
		}
		public String getMethod() {
			return null;
		}
		public Part getPart(String arg0) throws IOException, ServletException {
			return new _Part("file", 1024L, "image/png");
		}
		public Collection<Part> getParts() throws IOException, ServletException {
			List<Part> list = new ArrayList<>();
			list.add( new _Part("xxx", 1L, null) );
			list.add( new _Part("file", 1024L, "image/png") );
			return list;
		}
		public String getPathInfo() {
			return null;
		}
		public String getPathTranslated() {
			return null;
		}
		public String getQueryString() {
			return null;
		}
		public String getRemoteUser() {
			return null;
		}
		public String getRequestURI() {
			return null;
		}
		public StringBuffer getRequestURL() {
			return null;
		}
		public String getRequestedSessionId() {
			return null;
		}
		public String getServletPath() {
			return this.request.getServletPath();
		}
		public HttpSession getSession() {
			return null;
		}
		public HttpSession getSession(boolean arg0) {
			return null;
		}
		public Principal getUserPrincipal() {
			return null;
		}
		public boolean isRequestedSessionIdFromCookie() {
			return false;
		}
		public boolean isRequestedSessionIdFromURL() {
			return false;
		}
		public boolean isRequestedSessionIdFromUrl() {
			return false;
		}
		public boolean isRequestedSessionIdValid() {
			return false;
		}
		public boolean isUserInRole(String arg0) {
			return false;
		}
		public void login(String arg0, String arg1) throws ServletException {
			
		}
		public void logout() throws ServletException {
			
		}
	}
	
	class _Part implements Part {
		
		String name;
		long size;
		String contentType;
		
		public _Part(String name, Long size, String contentType) {
			this.name = name;
			this.size = size;
			this.contentType = contentType;
		}
		
		public void write(String arg0) throws IOException {
		}
		
		public long getSize() {
			return size;
		}
		
		public String getName() {
			return name;
		}
		
		public InputStream getInputStream() throws IOException {
			return null;
		}
		
		public Collection<String> getHeaders(String arg0) {
			return null;
		}
		
		public Collection<String> getHeaderNames() {
			return null;
		}
		
		public String getHeader(String arg0) {
			return "";
		}
		
		public String getContentType() {
			return this.contentType;
		}
		
		public void delete() throws IOException {
		}
	};
}
