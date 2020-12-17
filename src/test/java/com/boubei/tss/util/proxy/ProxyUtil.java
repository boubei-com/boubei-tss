/* ==================================================================   
 * Created [2007-1-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018  
 * ================================================================== 
 */
package com.boubei.tss.util.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 动态代理相关的一些帮助方法。
 */
public class ProxyUtil {
	
	/**
	 * 获取对象所有的接口，包括其父类的接口，以及父类的父类的。。。
	 * 
	 * @param clazz
	 * @return
	 */
	public static Class<?>[] getInterfaces(Class<?> clazz) {
		List<Class<?>> interfaces = new ArrayList<Class<?>>();

		Class<?> superClazz = clazz;
		while (superClazz != Object.class) {
			interfaces.addAll(Arrays.asList(superClazz.getInterfaces()));
			superClazz = superClazz.getSuperclass();
		}

		Class<?>[] classes = new Class[interfaces.size()];
		for (int i = 0; i < interfaces.size(); i++) {
			classes[i] = (Class<?>) interfaces.get(i);
		}
		return classes;
	}
}
