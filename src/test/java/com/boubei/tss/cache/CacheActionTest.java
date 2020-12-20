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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.cache.extension.CacheAction;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamManager;

import org.junit.Assert;

public class CacheActionTest extends AbstractTest4F {
	
	@Autowired private CacheAction action;
 
    @Test
	public void testCacheAction() {
		action.getAllCacheStrategy4Tree(response);
		
		action.getPoolsGrid(response);
		
		// ReusablePool: taskpool test
		String poolCode = "taskpool";
		action.listCacheItems(response, poolCode);
		CacheHelper.getNoDeadCache().putObject("xxx", new Object[] { 1, 2 });
		
		action.releaseCache(response, poolCode);
		action.initPool(response, poolCode);
		
		action.viewCachedItem(response, poolCode, "Task_6");
		action.viewCachedItem(response, CacheHelper.getNoDeadCache().getCacheStrategy().getCode(), "xxx");
		action.viewCachedItem(response, poolCode, "not-exist-key");
		
		action.removeCachedItem(response, poolCode, "Task_6");
		action.removeCachedItem(response, poolCode, "not-exist-key-2");
		
		action.viewCachedItem(response, poolCode, "Task_6");
		action.removeCachedItem(response, poolCode, "Task_6");
		
		// common pool: 
		poolCode = "LONGER";
		AbstractPool longerPool = (AbstractPool) CacheHelper.getLongerCache();
		longerPool.putObject("test-1", "test-1-Value");
		
		longerPool.checkOut();
		action.listCacheItems(response, poolCode);
		
		action.viewCachedItem(response, poolCode, "test-1");
		action.viewCachedItem(response, poolCode, "test-1");
		
		action.releaseCache(response, poolCode);
		action.initPool(response, poolCode);
		
		action.viewCachedItem(response, poolCode, "test-1");
		action.viewCachedItem(response, poolCode, "not-exist-key");
		
		action.removeCachedItem(response, poolCode, "test-1");
		action.removeCachedItem(response, poolCode, "test-1");
		action.removeCachedItem(response, poolCode, "not-exist-key-2");
		
		action.viewCachedItem(response, poolCode, "test-1");
		
		action.listCacheItems(response, "threadpool");
	}
 
    @Test
	public void testModifyCacheConfig() {
    	JCache jCache = JCache.getInstance();
    	
//    	Assert.assertEquals("", jCache.getPool("connectionpool3"));
		action.modifyCacheConfig(response, "connectionpool3", "{\"poolSize\":\"20\", \"initNum\":\"10\"}");
		action.modifyCacheConfig(response, "connectionpool3", "{\"poolSize\":\"8\", \"initNum\":\"6\"}");
		action.modifyCacheConfig(response, "connectionpool3", "{\"poolSize\":\"8\", \"cyclelife\":\"10000\"}");
		action.modifyCacheConfig(response, "connectionpool3", "{\"poolSize\":\"8\", \"cyclelife\":\"10000\", \"poolContainerClass\":\"com.boubei.tss.cache.MapContainer\"}");
		
		Pool pool = jCache.getConnectionPool();
		for(int i = 0; i < 8; i++) {
			pool.checkOut(0);
		}
		
		try {
			pool.checkOut(0);
		} catch(Exception e) {
			Assert.assertTrue("缓存池【服务数据缓存（短期）】已满，且各缓存项都处于使用状态，需要等待。可考虑重新设置缓存策略！", true);
		}
		
		action.modifyCacheConfig(response, "connectionpool3", "{\"poolSize\":\"10\", \"initNum\":\"6\"}");
		pool.checkOut(0);
		pool.checkOut(0);
		
		action.modifyCacheConfig(response, "connectionpool3", "{\"poolSize\":\"8\", \"cyclelife\":\"10000\", \"poolContainerClass\":\"com.boubei.tss.cache.MapContainer\"}");
		
		CacheAction action2 = new CacheAction();
		action2.paramService = action.paramService;
		
		action2.listCacheItems(response, "connectionpool3");
		action.modifyCacheConfig(response, "connectionpool3", "{\"poolSize\":\"10\"}");
		
		testPCacheListner();
	}
    
    
    String poolConfig1 = "{" +
			"\"customizerClass\":\"com.boubei.tss.framework.persistence.connpool.ConnPoolCustomizer\"," +
			"\"poolClass\":\"com.boubei.tss.cache.ReusablePool\"," +
			"\"code\":\"pool_1_\"," +
			"\"name\":\"DB连接池-1\"," +
			"\"cyclelife\":\"180000\"," +
			"\"paramFile\":\"H2.properties\"," +
			"\"interruptTime\":\"1000\"," +
			"\"poolSize\":\"10\"" +
			"}";
	
	String poolConfig2 = "{" +
			"\"customizerClass\":\"com.boubei.tss.framework.persistence.connpool.ConnPoolCustomizer\"," +
			"\"poolClass\":\"com.boubei.tss.cache.ReusablePool\"," +
			"\"code\":\"pool_2_\"," +
			"\"name\":\"DB连接池-2\"," +
			"\"cyclelife\":\"180000\"," +
			"\"paramFile\":\"org.h2.Driver,jdbc:h2:mem:h2db;DB_CLOSE_DELAY=-1,sa,123\"," +
			"\"interruptTime\":\"1000\"," +
			"\"poolSize\":\"10\"" +
			"}";
	
	private void testPCacheListner() {
		Param cacheGroup = CacheHelper.getCacheParamGroup(paramService);
		Long parentId = cacheGroup.getId();
		
		Param param1 = ParamManager.addSimpleParam(parentId, "pool_1_", "pool_1_", poolConfig1);
		Param param2 = ParamManager.addSimpleParam(parentId, "pool_2_", "pool_2_", poolConfig2);
		
		paramService.startOrStop(parentId, 1);
		
		Pool pool1 = JCache.pools.get("pool_1_");
		Pool pool2 = JCache.pools.get("pool_2_");
	
		Assert.assertNotNull(pool1);
		Assert.assertNotNull(pool2);
		
		Cacheable connItem = pool1.checkOut(0);
		Assert.assertNotNull(connItem);
		Assert.assertNotNull(connItem.getValue());
		
		connItem = pool2.checkOut(0);
		Assert.assertNotNull(connItem);
		Assert.assertNotNull(connItem.getValue());
		
		paramService.saveParam(param1);
		paramService.delete(param2.getId());
	}
    
}
