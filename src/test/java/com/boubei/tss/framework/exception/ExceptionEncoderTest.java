/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.exception;

import java.net.SocketException;
import java.sql.SQLException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.framework.sso.context.Context;

public class ExceptionEncoderTest {
	
	@Test
	public void test1() {
		MockHttpServletResponse response = new MockHttpServletResponse();
        Context.initRequestContext(new MockHttpServletRequest());
		
		ExceptionEncoder.encodeException(response, new BusinessException("Duplicate entry"));
		ExceptionEncoder.encodeException(response, new BusinessException("cannot be null", "505"));
		ExceptionEncoder.encodeException(response, new RuntimeException("cannot be null"));
		ExceptionEncoder.encodeException(response, new BusinessServletException(new RuntimeException("cannot be null"), true));
		
		Exception be = new BusinessException("test BusinessException encoder", true);
		ExceptionEncoder.encodeException(response, be);
		ExceptionEncoder.logException("xxx", be);
		ExceptionEncoder.printErrorMessage(be);
		
		be = new BusinessException("test BusinessException encoder2", false);
		ExceptionEncoder.encodeException(response, be);
		ExceptionEncoder.printErrorMessage(be);
		
		be = new BusinessServletException(be, false);
		ExceptionEncoder.encodeException(response, be);
		ExceptionEncoder.logException("xxx", be);
		
		be = new BusinessServletException(be, true);
		ExceptionEncoder.encodeException(response, be);
		
		be = new BusinessServletException("xxx");
		ExceptionEncoder.encodeException(response, be);
		
		be = new BusinessServletException("xxx", be);
		ExceptionEncoder.encodeException(response, be);
		
		be = new BusinessException("don't print", false);
		ExceptionEncoder.encodeException(response, be);
		
		be = new BusinessException("don't print", new SocketException());
		ExceptionEncoder.encodeException(response, be);
		
		be = new BusinessServletException(new RuntimeException("bes", be));
		new BusinessServletException("xxx", be).errorCode();
		
		ExceptionEncoder.encodeException(response, new RuntimeException());
		ExceptionEncoder.logException("xxx", new RuntimeException());
		
		response.setCommitted(true);
		response.setWriterAccessAllowed(false);
		ExceptionEncoder.encodeException(response, new RuntimeException("eee"));
		
		ExceptionEncoder.encodeException(response, new SQLException("Incorrect string value: '\\\\xF0\\\\x9F\\\\x98\\\\x81' for column 'description'"));
	}
	
	@Test
	public void test2() {
		Context.destroy();
		ExceptionEncoder.encodeException(null, null);
	}

}
