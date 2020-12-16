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

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.exception.BusinessException;

public class DefaultExceptionConvertorTest {
	
	@Test
	public void test() {
		DefaultExceptionConvertor c = new DefaultExceptionConvertor();
		
		Exception ex = new ConstraintViolationException("ConstraintViolationException delete error", null, "delete");
		Exception rlt = c.convert(ex );
		_TestUtil.assertExMsg(EX.ERR_HAS_FKEY, rlt);
		
		ex = new ConstraintViolationException("ConstraintViolationException insert error", null, "insert");
		rlt = c.convert(ex );
		_TestUtil.assertExMsg(EX.ERR_UNIQUE, rlt);
		
		ex = new javax.persistence.OptimisticLockException("org.hibernate.StaleObjectStateException: " +
				"Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect): " +
				"[com.boubei.tss.um.entity.User#194]");
		rlt = c.convert(ex );
		_TestUtil.assertExMsg(EX.ERR_LOCK_VERSION, rlt);
		
		ex = new BusinessException("Cannot delete or update a parent row: a foreign key constraint fails "
				+ "(`wms`.`wms_inv`, CONSTRAINT `FK57871D0F7C36DC88` FOREIGN KEY (`location_id`) REFERENCES `wms_location` (`id`)) "
				+ "--- SQL: delete from wms_location where id=27, connectionpool");
		rlt = c.convert(ex );
		_TestUtil.assertExMsg(EX.ERR_HAS_FKEY, rlt);
		
		ex = new ConstraintViolationException("Cannot add or update a child row: a foreign key constraint fails "
				+ "(`wms`.`wms_order`, CONSTRAINT `FK928A3CECD31CA4AC` FOREIGN KEY (`owner_id`) REFERENCES `wms_owner` (`id`))", null, "insert");
		rlt = c.convert(ex );
		_TestUtil.assertExMsg(EX.ERR_FK_NOT_EXIST, rlt);
	}

}
