/* ==================================================================   
 * Created [2006-8-13] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018  
 * ================================================================== 
*/
package com.boubei.tss.util.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * <p> ProxyProfiler.java </p>
 * 
 * <p>性能测量Dynamic Proxy实现，适合实现接口的对象。 </p>
 * <p>计算对象方法的执行时间。 </p>
 * <p>可指定拦截对象需要被拦截测试的具体方法列。 </p>
 * 
 */
public class ProxyProfiler {
    
    /**
     * 默认拦截所有的方法。
     * @param object  指定拦截对象
     * @return
     */
    public static Object frofiler(final Object object) {
        return ProxyProfiler.frofiler(object, null);
    }
  
    /**
     * 指定拦截对象，同时指定拦截对象需要被拦截测试的具体方法列。
     * @param object  指定拦截对象
     * @param invokeMethods 指定拦截对象需要被拦截测试的具体方法列
     * @return
     */
    public static Object frofiler(final Object object, final String[] invokeMethods) {
        
        Class<?>[] interfaces = ProxyUtil.getInterfaces(object.getClass()); 
        
        return Proxy.newProxyInstance(object.getClass().getClassLoader(), interfaces, 
        		new BaseInvocationHandler(object, invokeMethods) {
		            protected Object before(Object target, Method method, Object[] args){
		                long startTime = System.currentTimeMillis();
		                return new Long(startTime);
		            }
		            protected void after(Object target, Method method, Object[] args, Object beforeReturnVal){
		                long startTime = (Long) beforeReturnVal;
		                long endTime = System.currentTimeMillis();   
		                Object argsStr = args != null && args.length > 0 ? Arrays.asList(args) : "";
		                
		                log.info("方法【" + method.getName() + "(" + argsStr + ")" + "】执行时间为【" + (endTime - startTime) + "】ms.");
		            }
		        }
        );
    }
}



