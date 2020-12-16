/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.util.proxy;

import org.junit.Test;


/**
 * 菲波那契(Fibonacci)数列的几种解法 
 */
public class FibonacciTest {
 
    @Test
    public void testProxy() {
        Fibonacci f = (Fibonacci) ProxyProfiler.frofiler(new FibonacciImpl());
        
        System.out.println(f.recursive(32)); //拦截递归调用得方法只能拦截住第一次
        System.out.println(f.fibArray(1000000));       
        System.out.println(f.fib(1000000));
    }
    
    public interface Fibonacci {
        /** 递归 */
        long recursive(int index);
        
        /** 数组 */
        long fibArray(int index);
        
        /** 循环 */
        long fib(int index);
    }

    public static class FibonacciImpl implements Fibonacci {
        public long recursive(int index) {
            if (index <= 2) {
                return 1;
            } else {
                return this.recursive(index - 1) + recursive(index - 2);
            }
        }

        public long fibArray(int index) {
            long[] array = new long[index];
            if (index == 1) {
                array[0] = 1;
            }
            if (index >= 2) {
                array[0] = 1;
                array[1] = 1;
            }
            if (index > 2) {
                for (int i = 2; i < array.length; i++) {
                    array[i] = array[i - 2] + array[i - 1];
                }
            }
            return array[index - 1];
        }
        
        public long fib(int index) {
            long sum = 1, x = 1;
            if (index > 2) {
                for (int i = 0; i < index - 2; i++) {
                    sum += x;
                    x = sum - x;
                }
            }
            return sum;
        }
    }
}

