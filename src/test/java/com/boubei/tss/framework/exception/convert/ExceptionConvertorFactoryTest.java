/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.exception.convert;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.exception.BusinessException;

public class ExceptionConvertorFactoryTest {
	
	@Test
	public void testCreateConvter() {
		IExceptionConvertor convertor = ExceptionConvertorFactory.getConvertor();
		Assert.assertTrue(convertor instanceof IExceptionConvertor);
		
		ExceptionConvertorFactory.convertor = null;
		Config.setProperty(ExceptionConvertorFactory.EXCEPTION_CONVERTOR, "com.boubei.tss.framework.exception.convert.DefaultExceptionConvertor");
		convertor = ExceptionConvertorFactory.getConvertor();
		Assert.assertTrue(convertor instanceof IExceptionConvertor);
		
		ExceptionConvertorFactory.convertor = null;
		Config.setProperty(ExceptionConvertorFactory.EXCEPTION_CONVERTOR, 
				"com.boubei.tss.framework.exception.convert.DefaultExceptionConvertor,,com.boubei.tss.framework.exception.convert.DefaultExceptionConvertor");
		convertor = ExceptionConvertorFactory.getConvertor();
		Assert.assertTrue(convertor instanceof ExceptionConvertorChain);
		
		Assert.assertTrue(convertor.convert( new BusinessException("e1: ConstraintViolationException column 'code' cannot be null") ).getMessage().indexOf(EX.ERR_NOT_NULL) >= 0 );
		
		_TestUtil.assertExMsg(EX.ERR_UNIQUE, convertor.convert( new BusinessException("e1: ConstraintViolationException insert") ) );
		_TestUtil.assertExMsg("e1: ConstraintViolationException update", convertor.convert( new BusinessException("e1: ConstraintViolationException update") ) );
		_TestUtil.assertExMsg(EX.ERR_HAS_FKEY, convertor.convert( new BusinessException("e1: ConstraintViolationException delete") ) );
		_TestUtil.assertExMsg(EX.ERR_LOCK_VERSION, convertor.convert( new BusinessException("e1: Row was updated or deleted by another transaction") ) );
	}

}
