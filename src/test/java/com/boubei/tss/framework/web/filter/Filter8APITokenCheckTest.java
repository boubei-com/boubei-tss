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
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.EX;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.InfoEncoder;
 
public class Filter8APITokenCheckTest extends AbstractTest4UM {

    private Filter8APITokenCheck filter;
 
    private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		
	    request = new MockHttpServletRequest();
	    response = new MockHttpServletResponse();

	    filter = new Filter8APITokenCheck();
	}
	
	@Test
    public void testDoPost() {
		
		request.addParameter("uName", "Admin");
		String uToken = "E5E0A2593A3AE4C038081D5F113CEC78";
	    
        try {
			request.addParameter("uToken", uToken);
    	    filter.doFilter(request, response, new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                }
            });
    	    
    	    request.removeParameter("uToken");
        	filter.doFilter(request, response, new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                }
            });
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
        	filter.destroy();
        }
        
        // 第二次访问，不用再重新校验uName 和 uToken 及 mockLogin
        try {
        	Cookie[] cookies = new Cookie[3];
    		cookies[0] = new Cookie("TSS", "TSS");
    		cookies[1] = new Cookie("JSESSIONID", request.getSession().getId());
    		cookies[2] = new Cookie("token", Context.getRequestContext().getAgoToken() );
    		request.setCookies( cookies );
    		request.getSession().setAttribute("token", Context.getRequestContext().getAgoToken() );
    		Context.initRequestContext(request);
    		
        	request.removeParameter("uToken");
        	request.addParameter("uToken", "ErrorToken");
    	    filter.doFilter(request, response, new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                }
            });
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
        	filter.destroy();
        }
        
        // test hardest
        SecurityUtil.LEVEL_7 = 2;
        request.removeParameter("uToken");
        
        //  验签1 md5(uToken + 时间戳) success
    	String timestamp = DateUtil.formatCare2Second(new Date());
    	request.addParameter("timestamp", timestamp);
    	request.addParameter("uSign", InfoEncoder.string2MD5(uToken + timestamp));
    
    	try{
		    filter.doFilter(request, response, new FilterChain() {
	            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
	            }
	        });
		} catch (Exception e) {
	    	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
	    }
    	
    	//  验签2 md5(uToken + body.json + 时间戳) success
    	String params = "aaabbbskuname货品Xwh仓库1";
    	
    	JSONObject obj = new JSONObject();
    	obj.put("skuname", "货品X");
    	obj.put("aaa", "bbb");
    	obj.put("wh", "仓库1");
		request.setContent(obj.toString().getBytes());
    	request.removeParameter("uSign");
    	request.addParameter("uSign", InfoEncoder.string2MD5(uToken + params + timestamp));
    
    	try{
		    filter.doFilter(request, response, new FilterChain() {
	            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
	            }
	        });
		} catch (Exception e) {
	    	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
	    }
    	
    	//  验签 失败
    	request.removeParameter("uSign");
    	request.addParameter("uSign", "wrongSign");
    	try {
    	    filter.doFilter(request, response, new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                }
            });
    	    Assert.fail("should throw exception but didn't.");
            
        } catch (Exception e) {
        	Assert.assertEquals(EX.DM_11B, e.getMessage());
        } 
    	
    	 //  验签 签名为空
	    timestamp = "2018-01-01 12:12:12";
	    request.removeParameter("uSign");
	    request.removeParameter("sign");
        try {
    	    filter.doFilter(request, response, new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                }
            });
    	    Assert.fail("should throw exception but didn't.");
            
        } catch (Exception e) {
        	Assert.assertEquals(EX.DM_11C, e.getMessage());
        } 
        
        //  验签 时间戳无效
	    timestamp = "2018-01-01 12:12:12";
	    request.removeParameter("timestamp");
    	request.addParameter("timestamp", timestamp);
    	request.addParameter("uSign", InfoEncoder.string2MD5(uToken + timestamp));
        try {
    	    filter.doFilter(request, response, new FilterChain() {
                public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                }
            });
    	    Assert.fail("should throw exception but didn't.");
            
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.DM_11A, timestamp), e.getMessage());
        } 
        
        SecurityUtil.LEVEL_7 = 7;
    }
}
