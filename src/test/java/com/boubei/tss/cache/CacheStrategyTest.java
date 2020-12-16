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

import com.boubei.tss.util.BeanUtil;

public class CacheStrategyTest {
	
	Pool pool; 
	
	@Before
	public void setUp() {
		pool = JCache.getInstance().getPool("LONG");
	}
	
	@After
	public void tearDown() {
		pool = null;
	}
	
	@Test
	public void testCacheStrategy() {
		CacheStrategy strategy = pool.getCacheStrategy();
		
		CacheStrategy strategy2 = new CacheStrategy();
		BeanUtil.copy(strategy2, strategy);
		
		Assert.assertTrue(strategy2.equals(strategy));
		
		Assert.assertNotNull(strategy.getPoolInstance());
		
		Assert.assertTrue(5 == strategy.getAccessMethod());
		Assert.assertEquals("LONG", strategy.getCode());
		Assert.assertEquals("服务数据缓存（长期）", strategy.getName());
		Assert.assertEquals("com.boubei.tss.cache.DefaultCacheCustomizer", strategy.getCustomizerClass());
		Assert.assertEquals("com.boubei.tss.cache.ObjectPool", strategy.getPoolClass());
		Assert.assertEquals("com.boubei.tss.cache.MapContainer", strategy.getPoolContainerClass());
		
		Assert.assertTrue(1000000 == strategy.getCyclelife());
		Assert.assertTrue(0  == strategy.getInitNum());
		Assert.assertTrue(50 == strategy.getPoolSize());
		Assert.assertTrue(1000 == strategy.getInterruptTime());
		
		Assert.assertNull(strategy.getParamFile());
		Assert.assertNull(strategy.getRemark());
		Assert.assertEquals(CacheStrategy.TRUE, strategy.getVisible());
		Assert.assertEquals(CacheStrategy.FALSE, strategy.getDisabled());
		
		strategy2.setPoolSize(12);
		strategy2.setInitNum(3);
		strategy2.setName("数据缓存（长期）");
		strategy2.setInterruptTime(2000l);
		strategy2.setCyclelife(3000l);
		strategy2.setPoolContainerClass("com.boubei.tss.cache.MapContainer");
		strategy2.setPoolClass("com.boubei.tss.cache.ReusablePool");
		strategy2.setCustomizerClass("com.boubei.tss.cache.ScannerTaskPoolCustomizer");
		
		strategy.fireEventIfChanged(strategy2);
		
		strategy2.setDisabled( CacheStrategy.TRUE );
		strategy.fireEventIfChanged(strategy2);
		
		strategy2.setDisabled( CacheStrategy.FALSE );
		strategy.fireEventIfChanged(strategy2);
		
		strategy2.setParamFile("<x/>");
		strategy.fireEventIfChanged(strategy2);
		
		strategy2.setPoolContainerClass(null);
		strategy2.setCyclelife(null);
		strategy2.setInterruptTime(null);
		strategy2.setName(null);
		strategy2.setPoolSize(null);
		strategy2.setInitNum(null);
		Assert.assertEquals(CacheStrategy.DEFAULT_CONTAINER, strategy2.getPoolContainerClass());
		Assert.assertEquals(new Long(0), strategy2.getCyclelife());
		Assert.assertEquals(strategy2.getCode(), strategy2.getName());
		Assert.assertEquals(new Long(0), strategy2.getInterruptTime());
		Assert.assertEquals(new Integer(12), strategy2.getPoolSize());
		Assert.assertEquals(new Integer(0), strategy2.getInitNum());
		
		Assert.assertFalse( strategy2.equals(pool) );
		
		strategy2.setCyclelife(0L);
		Assert.assertEquals(60*60*1000L, strategy2.getMaxWait());
		strategy2.setCyclelife(3000L);
		Assert.assertEquals(3000L * 2, strategy2.getMaxWait());
		
		strategy2.setPoolSize(0);
		Assert.assertTrue(0.2f == strategy2.calPoolLoadPercent(20));
		strategy2.setPoolSize(40);
		Assert.assertTrue(0.5f == strategy2.calPoolLoadPercent(20));
		
		try {
			ContainerFactory.getInstance().create(ScannerTask.class.getName(), "c1");
			Assert.fail("should throw exception but didn't.");
		} catch(Exception e) {
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}
}
