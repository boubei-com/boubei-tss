/* ==================================================================   
 * Created [2015-9-12] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cache.extension;

import java.util.Set;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.param.ParamService;
import com.boubei.tss.util.EasyUtils;

public class CacheHelper {
	
	public static Pool getShorterCache() {
		return JCache.getInstance().getPool(CacheLife.SHORTER.toString());
	}
	
	public static Pool getShortCache() {
		return JCache.getInstance().getPool(CacheLife.SHORT.toString());
	}
	
	public static Pool getLongCache() {
		return JCache.getInstance().getPool(CacheLife.LONG.toString());
	}
	
	public static Pool getLongerCache() {
		return JCache.getInstance().getPool(CacheLife.LONGER.toString());
	}
	
	public static Pool getNoDeadCache() {
		return JCache.getInstance().getPool(CacheLife.NODEAD.toString());
	}
	
	/**
	 * 按照关键字刷新指定缓存池中的对象
	 */
	public static void flushCache(String poolName, String likeKey) {
		if( EasyUtils.isNullOrEmpty(likeKey) ) return;
		
		Pool pool = JCache.getInstance().getPool(poolName);
		Set<Object> keys = pool.listKeys();
		for(Object _key : keys) {
			if( _key.toString().indexOf(likeKey) >= 0) {
				pool.destroyByKey(_key);
			}
		}
	}
	
	/**
	 * 清理掉最近 howLong 分钟内，点击此处小于 lessHits 的缓存
	 */
	public static void flushCache(Pool pool, String likeKey, int lessHits, long now, long howLong) {
		for(Cacheable obj : pool.listItems()) {
			Object key = obj.getKey();
			if( key.toString().indexOf(likeKey) > 0 && obj.getHit() < lessHits &&  now - obj.getAccessed() > 1000*60*howLong ) {
				pool.destroyByKey(key);
			}
		}
	}
	
	public static Param getCacheParamGroup(ParamService paramService) {
		Param paramGroup = paramService.getParam(PX.CACHE_PARAM);
		if(paramGroup == null) {
			paramGroup = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, EX.CACHE_CONFIG);
			paramGroup.setCode(PX.CACHE_PARAM);
	        paramService.saveParam(paramGroup);
		}
		return paramGroup;
	}
}
