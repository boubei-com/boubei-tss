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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4TSS;
import com.boubei.tss.PX;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamManager;

import junit.framework.Assert;

/**
71 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】first time executing...
72 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 1
72 QueryCache waiting...
73 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 2
73 QueryCache waiting...
74 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 3
74 QueryCache waiting...
75 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 4
75 QueryCache waiting...
76 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 5
76 QueryCache waiting...
77 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 6
77 QueryCache waiting...
78 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 7
78 QueryCache waiting...
79 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 8
79 QueryCache waiting...
80 QueryCache【QC_com.boubei.tss.cache.aop.IService.f1()】= 9
com.boubei.tss.framework.exception.BusinessException: 当前应用服务器资源紧张，请稍后再查询。10>9

缓存池【服务数据缓存（短期），82.0】的当前快照：
----------------【SHORT_free】池中数据项列表，共【1】个 --------------
  key: QC_com.boubei.tss.cache.aop.IService.f1(), value: 9, hit: 9
 */
public class QueryCacheInterceptorTest extends AbstractTest4TSS {
	
	@Autowired IService service;
	
	@Before
	public void setUp() {
    	try {
    		ParamManager.addSimpleParam(0L, PX.MAX_QUERY_REQUEST, "MQR", "9");
    	} catch(Exception e) { }
	}
 
    @Test
    public void test() {     
        final List<Object> results = new ArrayList<Object>();
        for(int i = 0; i < 11; i++) {
        	new Thread() {
        		public void run() {
					Object ret = service.f0( 0 ); // f0 里 Thread.sleep(200);
					
					// 查看打出来的是不是同一个对象，是的话说明cache拦截器在queryCache拦截器后执行，正常。
					System.out.println("------" + Environment.threadID() + "------" + ret); 
					for(Object obj : results) {
						Assert.assertEquals(obj, ret);
					}
					results.add(ret);
    			}
        	}.start();
        	
        	try { Thread.sleep(15); } catch (InterruptedException e) { }
        }

        System.out.println(CacheHelper.getShortCache());
        
        try { Thread.sleep(5000); } catch (InterruptedException e) { }
    }
 
    /**
     * 当前应用服务器资源紧张，请稍后再查询。11>10
     */
    @Test
    public void test2() {     
    	Logger.getLogger("com.boubei").setLevel(Level.INFO);
    	
    	QueryCacheInterceptor.MAX_QUERY_REQUEST = 10;
        for(int i = 0; i < 13; i++) {
        	new Thread() {
        		public void run() {
					Object ret = service.f0( Environment.threadID() ); // f0 里 Thread.sleep(200);
					
					// 查看打出来的是不是同一个对象，是的话说明cache拦截器在queryCache拦截器后执行，正常。
					System.out.println("------" + Environment.threadID() + "------" + ret); 
    			}
        	}.start();
        }
        
        try { Thread.sleep(5000); } catch (InterruptedException e) { }
        
        Logger.getLogger("com.boubei").setLevel(Level.DEBUG);
    }
    
    @Test
    public void test3() {     
    	Logger.getLogger("com.boubei").setLevel(Level.INFO);
    	
    	QueryCacheInterceptor.MAX_QUERY_TIME = 150;
        for(int i = 0; i < 13; i++) {
        	new Thread() {
        		public void run() {
					Object ret = service.f0( Environment.threadID() ); // f0 里 Thread.sleep(200);
					
					// 查看打出来的是不是同一个对象，是的话说明cache拦截器在queryCache拦截器后执行，正常。
					System.out.println("------" + Environment.threadID() + "------" + ret); 
    			}
        	}.start();
        }
        
        try { Thread.sleep(5000); } catch (InterruptedException e) { }
        
        Logger.getLogger("com.boubei").setLevel(Level.DEBUG);
    }
}
