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
com.boubei.tss.framework.exception.BusinessException: 当前您查询的数据服务响应缓慢，前面还有3个人在等待，请稍后再查询。
com.boubei.tss.framework.exception.BusinessException: 当前您查询的数据服务响应缓慢，前面还有3个人在等待，请稍后再查询。

缓存池【服务数据缓存（短期），82.0】的当前快照：
----------------【SHORT_free】池中数据项列表，共【1】个 --------------
  key: QC_com.boubei.tss.cache.aop.IService.f1(), value: 9, hit: 9
 */
public class QueryCacheInterceptor2Test extends AbstractTest4TSS {
	
	@Autowired IService service;
 
    @Test
    public void test() {     
    	
    	try {
    		ParamManager.addSimpleParam(0L, PX.MAX_QUERY_REQUEST, "MQR", "10");
    	} catch(Exception e) { }
        
        final List<Object> results = new ArrayList<Object>();
        for(int i = 0; i < 5; i++) {
        	new Thread() {
        		public void run() {
					Object ret = service.f1();
					
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
 
}
