/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JCacheTest {
	
	JCache cache;
			
	@Before
	public void setUp() {
		cache = JCache.getInstance();
	}
	
	/**
	 * 测试简单池
	 */
	@Test
	public void testJCache() {
		Assert.assertTrue( cache.listCachePools().size() >= 11 );
		
		Assert.assertNotNull(cache.getThreadPool());
		Assert.assertNotNull(cache.getConnectionPool());
		Assert.assertNull(cache.getPool(null));
		Assert.assertNull(cache.getPool("NotExsit"));
		
		JCache.pools.remove("connectionpool2");
		Assert.assertNull(cache.getPool("connectionpool2"));
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
	}
	
	@Test
	public void testAbstractPool() {
		Pool pool = cache.getTaskPool();
		pool.release(true);
		pool.init();
		
		try {
			Thread.sleep(3000); // 等待初始化完成
		} catch (InterruptedException e) {
		}
		
		Assert.assertTrue( pool.listItems().size() == pool.listKeys().size() );
		
		Assert.assertTrue( pool.getHitRate() >= 0d );
		Assert.assertTrue( pool.getRequests() >= 0 );
		
		Assert.assertNull(pool.putObject(null, new Object()));
	}
}
