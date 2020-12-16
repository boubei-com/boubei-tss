/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.dml;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.cache.JCache;

public class SqlConfigTest {

	@Test
	public void test() {
		
		JCache.pools.clear();
		JCache.cache = null;
		
		try {
			SqlConfig.getScript("noExists"); 
		} catch(Exception e) {
			Assert.assertEquals("没有找到编码为【noExists】的SQL", e.getMessage());
		}
		
		Assert.assertTrue( SqlConfig.sqlNestFmParams.keySet().contains("${saveAccessLog}") );
		Assert.assertTrue( SqlConfig.sqlNestFmParams.keySet().contains("${test1}") );
		
		Assert.assertNotNull(SqlConfig.getScript("test1"));
		try {
			Assert.assertNotNull(SqlConfig.getScript("test2"));
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("你对数据服务【test2】没有访问权限", e.getMessage());
		}
		
		Assert.assertNotNull(SqlConfig.getScript("saveAccessLog"));
		
	}
}
