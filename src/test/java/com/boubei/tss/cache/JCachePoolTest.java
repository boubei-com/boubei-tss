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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.cache.extension.threadpool.ThreadPoolCustomizer;
import com.boubei.tss.cache.extension.workqueue.AbstractTask;
import com.boubei.tss.cache.extension.workqueue.OutputRecordsManager;

import junit.framework.Assert;

public class JCachePoolTest {
	
	JCache cache;
			
	@Before
	public void setUp() {
		JCache.cache = null;
		cache = JCache.getInstance();
	}
	
	/**
	 * 测试简单池
	 */
	@Test
	public void testSimplePool() {
		SimplePool spool = (SimplePool) cache.getPool("SimplePool");
		assertNotNull(spool);
		
		assertEquals(0, spool.size());
		spool.release(true);
		
		assertEquals(0, spool.getFree().size());
		assertEquals(0, spool.getUsing().size());
	}
	
	/**
	 * 测试线程池
	 */
	@Test
	public void testThreadPool() {
		Pool tpool = (Pool) cache.getThreadPool();
		assertNotNull(tpool);
		
		Cacheable threadItem = tpool.checkOut(0);
		assertNotNull(threadItem);
		
		tpool.reload(threadItem);
		tpool.destroyByKey(threadItem.getKey());
		tpool.destroyObject(threadItem);
		
		ThreadPoolCustomizer tpc =new ThreadPoolCustomizer();
		Assert.assertFalse(tpc.isValid(null));
		tpc.destroy(null);
	}
	
	/**
	 * 测试任务池
	 */
	@Test
	public void testTaskPool() {
		AbstractPool taskpool = (AbstractPool) cache.getTaskPool();
		taskpool.flush();
		taskpool.init();
		
		Assert.assertNull(taskpool.getObject(null));
		
		Cacheable taskItem = taskpool.checkOut(0);
		assertNotNull(taskItem.getAccessed());
		assertTrue(taskItem.getPreAccessed() > 0);
		assertTrue(taskItem.getPreAccessed() <= taskItem.getAccessed());
		assertEquals(1, taskItem.getHit());
		assertEquals(false, taskItem.isExpired());
		
		Object key = taskItem.getKey();
		assertNotNull(key);
		assertNotNull(taskItem.getValue());
		
		taskpool.checkIn(taskItem);
		
		// 测试任务接收和执行
		for(int i = 0; i < 20; i++) {
			LogRecorder.getInstanse().output(i);
		}
		try {
			Thread.sleep(3000); // 休息三秒，等待剩余任务被强制执行
		} catch (InterruptedException e) {
		}
		
		// 测试checkOut等待
		for(int i = 0; i < 98; i++) {
			taskpool.checkOut(0);
		}
		
		try {
			taskpool.checkOut(0);
			taskpool.checkOut(0);
		} catch(Exception e) {
			Assert.assertTrue("缓存池【端口扫描任务池】已满，且各缓存项都处于使用状态，需要等待。可考虑重新设置缓存策略！", true);
		}
		
		taskpool.purge();
		try {
			Thread.sleep(6000); // 休息6秒,等待cleaner运行
		} catch (InterruptedException e) {
		}
		
		Assert.assertNull( taskpool.remove() );
		
		taskpool.checkIn(taskItem);
		TimeWrapper tw = new TimeWrapper("x", 1);
		taskpool.checkIn( tw );
		
		tw.getHitLong();
		Assert.assertEquals(" Forever ", tw.getDeath());
		
		taskItem = taskpool.getObject(key);
		taskpool.destroyByKey(key);
		taskItem = taskpool.getObject(key); // 触发reload
		taskpool.putObject(key, taskItem.getValue());
		
		assertTrue(taskpool.getHitRate() > 0);
		taskpool.flush();
		taskpool.init();
		
		Container free = taskpool.getFree();
		free.getByAccessMethod(Container.ACCESS_LIFO);
		free.getByAccessMethod(10001);
		
		taskpool.flush();
		free.put("aaa", new TimeWrapper("aaa", "bbb") );
		free.getByAccessMethod( Container.ACCESS_LIFO );
		free.getByAccessMethod( 9998 );
		
		taskpool.checkOut(0);
	}
	
	static class LogRecorder extends OutputRecordsManager{
		
		private static LogRecorder instance;
	    
	    private LogRecorder(){
	    }
	    
	    public static LogRecorder getInstanse(){
	        if(instance == null) {
	            instance = new LogRecorder();
	        }
	        return instance;
	    }
	    
	    protected int getMaxTime() { 
	    	return 2000;
	    }
 
	    protected void excuteTask(List<Object> logs) {
	    	AbstractTask task = new AbstractTask() {

	    		public void excute() {
					for (Object temp : records) {
				    	System.out.println(temp);
				    }
				}
	    		
	    	};
	        task.fill(logs);

	        tpool.excute(task);
	    }
	}
}
