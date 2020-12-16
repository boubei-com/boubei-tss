/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock;

import org.junit.Test;

import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.util.BeanUtil;

/**
 * 因IEntity接口里定义了  Serializable getId();
 * 导致使用反射机制处理ID值时异常，没有"写方法" 
 *
 */
public class EntityIDTest {
	
	@Test
	public void test() {
		_Group e1 = new _Group();
		e1.setId(1L);
		e1.setCode("RD");
		
		_Group e2 = new _Group();
		BeanUtil.copy(e2, e1);
		
		System.out.println(e2.getId());
		System.out.println(e2.getCode());
		
		XO e3 = new XO();
		e3.setId(1L);
		e3.setCode("RD");
		
		XO e4 = new XO();
		BeanUtil.copy(e4, e3);
		
		System.out.println(e4.getId());
		System.out.println(e4.getCode());
	}
	
	public static class XO {
		private Long id;
	    private String code;
	    
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
	    
	}

}
