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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * cache的free、using分池策略导致多并发情形下，size计算不准，当缓存对象在两个池之间移动时，size()方法将会统计不到这些对象
 */
public class MultiThreadTest {
	
	private Logger log = Logger.getLogger(this.getClass());
	
	JCache cache;
	
	@Before
	public void setUp() {
		cache = JCache.getInstance();
		
		try {
			Thread.sleep(3000); // 等待初始化完成
		} catch (InterruptedException e) {
		}
	}
	
	@Test
	public void testMultiThreadCheckOutAndIn() {
		final Pool pool = (Pool) cache.getThreadPool();
		
		final CountDownLatch begin = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(100); // 总请求次数
		final ExecutorService exec = Executors.newFixedThreadPool(20); // 线程数

		for (int i = 0; i < 100; i++) {
			Runnable run = new Runnable() {
				public void run() {
					try {
						begin.await();
						
						Cacheable oneThread = pool.checkOut(0);
						Thread.sleep(30);
						pool.checkIn(oneThread);

					} catch (Exception e) {
						log.error("出错了!", e);
						
					} finally {
						end.countDown();
					}
				}
			};
			exec.submit(run);
		}
		
		begin.countDown();
		try {
			end.await();
		} catch (InterruptedException e) {
		}
		exec.shutdown();
		
		/**
		 * 总共请求100次，线程池阀值10，初始化5，所以除了新建5个，其余都直接命中，命中率95%。
		 */
		System.out.println("@ " + pool.getHitRate());
		System.out.println("@ " + pool.getRequests());
//		Assert.assertTrue( pool.getHitRate() > 49 );
//		Assert.assertEquals(50.0f, pool.getHitRate());
	}
}
