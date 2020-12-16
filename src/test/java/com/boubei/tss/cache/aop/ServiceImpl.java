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

import org.springframework.stereotype.Service;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.MathUtil;

@Service("xyzService")
public class ServiceImpl implements IService {
	
	public Object f0(Object flag) {
		return f1();
	}
	
	public Object f1() {
		
		try { Thread.sleep(100 + MathUtil.randomInt(100)); } catch (InterruptedException e) { }
		
		return Environment.threadID();
	}
	
	public Object f2() {
		return MathUtil.randomInt(10) * 0;
	}

}
