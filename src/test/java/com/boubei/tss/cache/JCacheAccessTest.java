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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.cache.extension.CacheHelper;

/**
 * 测试缓存池的存取方式，FIFO、LIFO、LRU、LFU、Radom等。
 * 并顺带测试测试容量、事件、生命周期等相关。
 */
public class JCacheAccessTest {
	
	Pool pool; 
	
	@Before
	public void setUp() {
		pool = CacheHelper.getNoDeadCache();
	}
	
	@After
	public void tearDown() {
		pool = null;
	}
	
	/**
	 * 测试存取方式LRU：最不常使用的先出
	 */
	@Test
	public void testPoolAccessLFU() {
		pool.flush();
		pool.init();
		pool.getCacheStrategy().setAccessMethod(Container.ACCESS_LFU);
		
		Assert.assertEquals(0, pool.size());
		
		for(int i = 0; i < 10; i++) {
			pool.putObject(i, i);
		}
		Assert.assertEquals(10, pool.size());
		
		for(int i = 0; i < 9; i++) {
			Cacheable item = pool.getObject(i);
			Assert.assertEquals(i, item.getValue());
		}
		System.out.println(pool);
		
		// 测试获取不存在的key
		Object notExistKey = 11;
		Cacheable item = pool.getObject(notExistKey);
		Assert.assertNull(item);
		
		// 测试命中率（上面10次getObject，9次命中，最后一次没有命中，命中率90%）
		Assert.assertTrue( 90.0f == pool.getHitRate());
		
		// 测试移除对象（按存取方式：FIFO、FILO、LRU、LFU、Radom等，当前配的存取方式是 LFU）
		item = pool.remove();
		Assert.assertNotNull(item);
		Assert.assertEquals(9, item.getValue()); // 最后一个元素命中率最低
		pool.destroyObject(item); // 销毁，只有这样size才会减一
		
		// 测试池阀值，maxSize=30 （9被remove掉，当前配的存取方式是 LFU）
		for(int i = 9; i <= 30; i++) {
			pool.putObject(i, i);
		}
		
		Assert.assertEquals(30, pool.size());
		Assert.assertNull(pool.getObject(9));
		Assert.assertNotNull(pool.getObject(30));
		
		// 清空缓存池
		pool.flush();
		Assert.assertEquals(0, pool.size());
	}
	
	@Test
	public void testPoolAccessLRU() {
		pool = JCache.getInstance().getPool("SHORT-2");
		pool.getCacheStrategy().setAccessMethod(Container.ACCESS_LRU);
		
		for(int i = 0; i < 10; i++) {
			pool.putObject(i, i);
 
			Cacheable item = pool.getObject(i);
			Assert.assertEquals(i, item.getValue());
			
			try {
				Thread.sleep(30); // 休息一会，以拉开最后访问时间
			} catch (InterruptedException e) {
			}
		}
		
		// 测试移除对象（按存取方式：FIFO、FILO、LRU、LFU、Radom等，当前配的存取方式是 LFU）
		Cacheable item = pool.remove();
		Assert.assertNotNull(item);
		Assert.assertEquals(0, item.getValue()); // 第一个元素最老
		pool.destroyObject(item); // 销毁，只有这样size才会减一
		
		// 测试池阀值，maxSize=10 （9被remove掉，当前配的存取方式是 LFU）
		pool.putObject(10, 10);
		pool.putObject(11, 11);
		Assert.assertEquals(10, pool.size());
		Assert.assertNull   (pool.getObject(1) );
		Assert.assertNotNull(pool.getObject(10));
		
		// 等待池中对象全部过期，cleaner线程每 Math.max(1000 * 5, cyclelife / 5) 执行一次清理
		try {
			Thread.sleep(11 * 1000);
		} catch (InterruptedException e) {
		}
		Assert.assertEquals(0, pool.size());
	}
	
	/**
	 * 测试check-out、check-in
	 */
	@Test
	public void testPoolCyclelife() {
		pool = JCache.getInstance().getPool("SHORT-3");
		pool.getCacheStrategy().setAccessMethod(Container.ACCESS_RANDOM);
		
		Assert.assertEquals(0, pool.size());
		
		for(int i = 0; i < 3; i++) {
			pool.putObject(i, i);
		}
		
		Cacheable item0 = pool.checkOut(0);
		Cacheable item1 = pool.checkOut(0);
		Cacheable item2 = pool.checkOut(0);
		
		Assert.assertNotNull(item0);
		Assert.assertNotNull(item1);
		Assert.assertNotNull(item2);
		
		try {
			pool.checkOut(0);
			Assert.fail("should throw exception but didn't.");
		} catch(Exception e) {
			Assert.assertTrue("缓存池【服务数据缓存（短期）】已满，且各缓存项都处于使用状态，需要等待。可考虑重新设置缓存策略！", true);
		}
		
		pool.checkIn(item0);
		
		Cacheable item4 = pool.checkOut(0);
		Assert.assertNotNull(item4);
		
		// 另外启一线程，执行checkOut操作，如果没有值，则等候三秒，等主线程的 item2被checkIn后，就能获取到了。
		new Thread() {
			public void run() {
				pool = JCache.getInstance().getPool("SHORT");
				Cacheable item5 = pool.checkOut(3000);
				Assert.assertNotNull(item5);
			}
		}.start();
		
		pool.checkIn(item2);
		
		pool.checkIn(null);
		
		pool.releaseAsync(true);
		
		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
		}
	}
}
