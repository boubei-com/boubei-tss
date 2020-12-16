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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.cache.extension.threadpool.IThreadPool;

/**
 * 测试池的性能。<br/>
 * 
 * 任务池的开关在缓存策略配置文件中设置，disabled（0：启用， 1：停用），比较两种状态下的性能。
 */
public class JCachePerformanceTest {
    
    protected Logger log = Logger.getLogger(JCachePerformanceTest.class);
    
    private Pool apool;
    private IThreadPool tpool;
    private int portNum = 100;
    Long cyclelife;
    
    @Before
    public void setUp() {
        JCache cache = JCache.getInstance();
        
        try { // 休眠6s，等待池初始化结束
            Thread.sleep(6000);
            apool = cache.getTaskPool();
            tpool = cache.getThreadPool();
            cyclelife = apool.getCacheStrategy().cyclelife;
        } 
        catch (InterruptedException e) {
            log.error(e);
        }
    }

    @Test
    public void testPoolPerformance() throws Exception {
        Logger.getRootLogger().setLevel(Level.DEBUG);
        
        for (int port = 1 ; port <= portNum ; port++) {
            scanPort(port);
            
            if( port % 10 == 0) {
                Thread.sleep(2*1000); // 歇会再执行后续的端口扫描
            }
            
            if( port % 111 == 0) {
                log.debug(tpool);
                log.debug(apool);
            }
        }
        log.debug("工作队列填充完毕！工填充了 " + portNum + "个任务。");
        
        // 休眠，等待全部端口被扫描完
        while( ScannerTask.finishedNum < portNum) {
            Thread.sleep(10*1000); 
        }
        
        // 休眠，等待池中对象都过期后
        Thread.sleep(cyclelife * 2); 
    }
    
    private void scanPort(int port) {
        Cacheable o = apool.checkOut(3*1000);
        ScannerTask task = (ScannerTask) o.getValue();
        task.port = port;
        
        o.update(task);
        tpool.excute(apool, o);
    }
    
}
