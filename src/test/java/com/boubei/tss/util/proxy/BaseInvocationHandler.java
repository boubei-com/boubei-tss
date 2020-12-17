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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.log4j.Logger;

/** 
 * InvocationHandler基类
 */
public abstract class BaseInvocationHandler implements InvocationHandler{
    protected Logger log = Logger.getLogger(this.getClass());
    
    private Object target;  //拦截的对象
    private String[] invokeMethods; //需要拦截的方法，为空则拦截任何方法
   
    /**
     * @param target  拦截的对象
     * @param invokeMethods  需要拦截的方法
     */
    public BaseInvocationHandler(Object target, String[] invokeMethods){
        this.invokeMethods = invokeMethods;
        this.target = target;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //如果没有指定拦截的方法列表，则拦截接口中所有方法。
        String methodName = method.getName();
		boolean invoke = (invokeMethods == null || Arrays.asList(invokeMethods).contains(methodName));
        
		Object preParamV = null;
		Object returnVal = null;
        try {
            if(invoke){
                //执行拦截前操作
                preParamV = (Long) before(target, method, args);
            }
            // 执行方法，并获取返回值
            returnVal = method.invoke(target, args);
            
        } catch (Exception e) {
//            log.error("unexpected invocation exception: " + e.getMessage());
        } finally {
            if(invoke) {
                //执行拦截后操作
                after(target, method, args, preParamV);
            }
        }
        
        return returnVal;
    }

    /**
     * 定义方法执行前的操作
     * @param target
     * @param method
     * @param args
     * @return
     */
    protected abstract Object before(Object target, Method method, Object[] args);
    
    /**
     * 定义方法执行后的操作
     * @param target
     * @param method
     * @param args
     * @param preParamV
     */
    protected abstract void after(Object target, Method method, Object[] args, Object preParamV);
}

