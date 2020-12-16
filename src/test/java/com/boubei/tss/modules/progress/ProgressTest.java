/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.progress;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.MathUtil;

/** 
 * 测试进度条
 */
public class ProgressTest {
    
    String code;
    long delay = 3; //每次取进度信息间隔时间 单位 s
    
    @Before
    public void setUp() {
        ProgressManager manager = new ProgressManager(new Progressable(){
            public String execute(Map<String, Object> params, Progress progress) {
                for(int i = 1; i <= 100; i++) {
                    // 随机的休眠1--5秒，然后打印出当前循环到的序号，直到100只进度100%完成。
                    try { 
                        Thread.sleep(MathUtil.randomInt(5) * 100); 
                    } catch (InterruptedException e) {
                    }
                    System.out.println(i);
                    
                    if( i%10 == 0) {
                        progress.add(10); // 进度加10
                    }
                }
                
                return "done";
            }
        }, 100, new HashMap<String, Object>());
        
        code = manager.execute();
    }
    
    // 以下为不同的线程轮番请求进度信息
    @Test
    public void testProgressInMultiThread() {
        int i = 0;
        Progress progress = (Progress)ProgressPool.getSchedule(code);
        progress.getException();
        ProgressPool.removeSchedule("notExsits");
        
        while (!progress.isCompleted() && !progress.isConceal()) {
            new Thread() {
                public void run() {
                    delay = getProgress(code);
                }
            }.start();
            
            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
            }
            
            if(i++ == 100) {
                progress.setIsConceal(true); //设置中止标志
            }
        }
    }
    
    @Test
    public void testProgressInMultiThread2() {
        int i = 0;
        Progress progress = ProgressPool.getSchedule(code);
        while (!progress.isCompleted() && !progress.isConceal()) {
            new Thread() {
                public void run() {
                	delay = getProgress(code);
                }
            }.start();
            
            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException e) {
            }
            
            if(i++ == 3) {
                progress.setIsConceal(true); //设置中止标志
            }
        }
        
        assertTrue(progress.isConceal());
    }
    
    long getProgress(String code) {
        Progress progress = (Progress)ProgressPool.getSchedule(code);
        if(!progress.isNormal()){
            ProgressPool.removeSchedule(code);
            Throwable t = progress.getException();
            throw new BusinessException(t.getMessage());
        } 
        if(progress.isConceal()){
            System.out.println("取消进度成功");
        }
        
        Object[] info = progress.getProgressInfo();
        System.out.println("已经完成" + info[0] + "%, 本次用时：" + info[1] + "s, 剩余时间" + info[2] + "s");
        
        if(progress.isCompleted()){
            ProgressPool.removeSchedule(code); //执行结束则将将进度对象从池中移除
        }
        return ((Long)info[1]);
    }
}

