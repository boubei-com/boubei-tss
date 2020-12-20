/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cache.aop;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4TSS;
import com.boubei.tss.EX;
import com.boubei.tss.cache.AbstractPool;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;

import org.junit.Assert;

/**
 * 测试10分钟Cache
 * 
缓存池【服务数据缓存（短期），77.0】的当前快照：
----------------【SHORT_free】池中数据项列表，共【2】个 --------------
  key: com.boubei.tss.cache.aop.IService.f1(), value: 82, hit: 21
  key: com.boubei.tss.cache.aop.IService.f2(), value: 0, hit: 99
----------------------------------- END ---------------------------
----------------【SHORT_using】池中数据项列表，共【0】个 --------------
----------------------------------- END ---------------------------
 */

public class ShortCacheTest extends AbstractTest4TSS {
	
	@Autowired IService service;
 
    @Test
    public void test() {     
        
        for(int i = 0; i < 100; i++) {
        	new Thread() {
        		public void run() {
					service.f1();
    			}
        	}.start();
        	
        	try { Thread.sleep(5); } catch (InterruptedException e) { }
        }
        
        try { Thread.sleep(15*1000); } catch (InterruptedException e) { }
        
        for(int i = 0; i < 100; i++) {
        	service.f2();
        }
        
        Pool shortCache = CacheHelper.getShortCache();
		System.out.println( shortCache );
    }
    
    @Test
    public void test2() {
    	QueryCacheInterceptor qi = new QueryCacheInterceptor();
    	AbstractPool qCache = (AbstractPool) CacheHelper.getShortCache();
    	qCache.putObject("QC_1", new Object());
    	
    	try {
    		qi.checkBusy(qCache, -1);
    		Assert.fail();
    	} catch(Exception e) {
    		Assert.assertEquals(EX.CACHE_1 + 1 + ">" + -1, e.getMessage());
    	}
    	
		qi.checkBusy(qCache, 10);
		
		try {
    		qi.checkBusy(qCache, -1);
    		Assert.fail();
    	} catch(Exception e) { }
		
		try {
    		qi.checkBusy(qCache, -1);
    	} catch(Exception e) { }
    }
}
