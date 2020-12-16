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

public interface IService {
	
	@QueryCached(limit=-1)
	@Cached
	Object f0(Object flag);
	
	@QueryCached
	@Cached
	Object f1();
	
	@Cached
	Object f2();

}
