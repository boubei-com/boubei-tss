/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence.connpool;

import java.sql.Connection;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.cache.CacheStrategy;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.ReusablePool;

public class ConnpoollTest {
	
	@Test
	public void test() {
		ReusablePool connpool = (ReusablePool) JCache.getInstance().getConnectionPool();
		
		Cacheable connItem = connpool.checkOut(0);
		
		connpool.destroyObject(connItem);
		connpool.destroyObject(null);
		
		Assert.assertTrue(connpool.getCustomizer().isValid(connItem) == false);
		
		connpool.reload(connItem);
		
		Assert.assertTrue(connpool.getCustomizer().isValid(connItem) == true);
		
		// test disable connection pool
		try { Thread.sleep(1000); } catch (InterruptedException e1) { }
		connpool.flush();
		
		CacheStrategy cs = connpool.getCacheStrategy();
		cs.disabled = CacheStrategy.TRUE;
		connpool.setCacheStrategy(cs);
		try {
			connpool.checkOut(1000);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("数据库存在异常，连接池已被停用", true);
		}
		cs.disabled = CacheStrategy.FALSE;
		connpool.setCacheStrategy(cs);
		
		// test IConnectionProvider
		
		String dbUrl = "org.h2.Driver,jdbc:h2:mem:h2db;DB_CLOSE_DELAY=-1,sa,123";
		Connection conn = _Connection.getInstanse(dbUrl).getConnection();
		Assert.assertNotNull(conn);
		
		try {
			dbUrl = "com.mysql.jdbc.Driver,jdbc:mysql://10.8.9.10:3306/tss,root,123456";
			_Connection.getInstanse(dbUrl).getConnection();
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("创建数据库连接时候出错", true);
		}
	}
}
