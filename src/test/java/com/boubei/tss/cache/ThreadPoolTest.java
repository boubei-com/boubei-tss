package com.boubei.tss.cache;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.boubei.tss.cache.extension.threadpool.IThreadPool;
import com.boubei.tss.cache.extension.workqueue.AbstractTask;

/**
 * 线程池使用案例
 * 
 * 注：Task里如设置service调用进行写数据，单元测试框架内会报 no transcation in progress; Tomcat/Jetty正常
 */
public class ThreadPoolTest {

	private class XXTask extends AbstractTask {

		public void excute() {
			for(Object o : records) {
				log.info(o);
			}
			log.info(this + " done");
		}
	}
	
	@Test 
	public void test() {
		IThreadPool threadPool = JCache.getInstance().getThreadPool();
		
		for(int i = 0; i < 10; i++) {
			List<Object> l1 = new ArrayList<>();
			l1.add("a1"); l1.add("b1");
			XXTask task1 = new XXTask();
			task1.fill(l1);
			threadPool.excute(task1);
			
			List<Object> l2 = new ArrayList<>();
			l2.add("a2"); l2.add("b2");
			XXTask task2 = new XXTask();
			task2.fill(l2);
			threadPool.excute(task2);
			
			List<Object> l3 = new ArrayList<>();
			l3.add("a3"); l3.add("b3");
			XXTask task3 = new XXTask();
			task3.fill(l3);
			threadPool.excute(task3);
		}
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
	}
}
