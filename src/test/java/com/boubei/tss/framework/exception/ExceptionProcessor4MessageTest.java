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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.boubei.tss.framework.web.display.ErrorMessageEncoder;

/**
 * 消息处理过程单元测试
 */
public class ExceptionProcessor4MessageTest {

    /**
     * 处理抛出异常时，消息显示问题
     */
	@Test
    public void testThrowException4Message() {
        BusinessException e1 = new BusinessException("test1", new Exception("test"));
        ErrorMessageEncoder encoder1 = new ErrorMessageEncoder(e1);
        assertEquals("test1", encoder1.getMessage());

        BusinessException e2 = new BusinessException("test2", e1);
        ErrorMessageEncoder encoder2 = new ErrorMessageEncoder(e2);
        assertEquals("test1", encoder2.getMessage());

        BusinessServletException e4 = new BusinessServletException(e2);
        ErrorMessageEncoder encoder4 = new ErrorMessageEncoder(e4);
        assertEquals("test1", encoder4.getMessage());

        BusinessServletException e6 = new BusinessServletException(e4);
        ErrorMessageEncoder encoder6 = new ErrorMessageEncoder(e6);
        assertEquals("test1", encoder6.getMessage());

    }

    /**
     * 处理抛出异常时，消息显示问题
     */
	@Test
    public void testThrowBusinessException4Message() {
        BusinessException e = new BusinessException("test");
        ErrorMessageEncoder encoder = new ErrorMessageEncoder(e);
        assertEquals("test", encoder.getMessage());

        BusinessException e3 = new BusinessException("test3", e);
        ErrorMessageEncoder encoder3 = new ErrorMessageEncoder(e3);
        assertEquals("test", encoder3.getMessage());

        BusinessServletException e5 = new BusinessServletException(e3);
        ErrorMessageEncoder encoder5 = new ErrorMessageEncoder(e5);
        assertEquals("test", encoder5.getMessage());

        BusinessServletException e7 = new BusinessServletException(e5);
        ErrorMessageEncoder encoder7 = new ErrorMessageEncoder(e7);
        assertEquals("test", encoder7.getMessage());
    }

    /**
     * 处理抛出异常时，消息显示问题
     */
	@Test
    public void testThrowUserIdentificationException4Message() {
        UserIdentificationException e = new UserIdentificationException("test");
        ErrorMessageEncoder encoder = new ErrorMessageEncoder(e);
        assertEquals("test", encoder.getMessage());

        BusinessServletException e5 = new BusinessServletException(e);
        ErrorMessageEncoder encoder5 = new ErrorMessageEncoder(e5);
        assertEquals("test", encoder5.getMessage());

        BusinessServletException e7 = new BusinessServletException(e5);
        ErrorMessageEncoder encoder7 = new ErrorMessageEncoder(e7);
        assertEquals("test", encoder7.getMessage());
    }

}
 
