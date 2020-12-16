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
import org.junit.Test;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;

public class CacheHelperTest {

	@Test
	public void test() {
		Assert.assertNotNull(CacheHelper.getLongCache());
		Assert.assertNotNull(CacheHelper.getLongerCache());
		Assert.assertNotNull(CacheHelper.getNoDeadCache());
		Assert.assertNotNull(CacheHelper.getShortCache());
		
		Pool shorterCache = CacheHelper.getShorterCache();
		Assert.assertNotNull(shorterCache);
		
		shorterCache.putObject("xyz(1)", "123456");
		CacheHelper.flushCache(CacheLife.SHORTER.toString(), "yz(1)");
		CacheHelper.flushCache(CacheLife.SHORTER.toString(), "");
		
		Assert.assertNull( CacheHelper.getLongerCache().getCustomizer().create() );
		Assert.assertTrue( CacheHelper.getLongerCache().getCustomizer().isValid( null ) );
		
		Pool cache = CacheHelper.getNoDeadCache();
		cache.putObject("Axxxx-", new Object());
		cache.putObject("Bxxxx-", new Object());
		CacheHelper.flushCache(cache, "xxxx", 0, System.currentTimeMillis() + 1000*60*1, 3);
		CacheHelper.flushCache(cache, "xxxx", 2, System.currentTimeMillis() + 1000*60*10, 3);
	}

}
