/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.log;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import junit.framework.Assert;


public class LogTest {

	@Test
	public void test() {
		
		StringBuffer operationCode = new StringBuffer();
		for(int i = 0; i< 120; i++) {
			operationCode.append(i);
		}
		
		Log log = new Log(operationCode.toString(), new Object());
		Assert.assertTrue( log.getOperationCode().length() == 100 );
		Assert.assertTrue("Object[]".equals(log.getContent()));
				
		log = new Log("Test", null);
		Assert.assertTrue("".equals(log.getContent()));
		
		LogQueryCondition lq = new LogQueryCondition();
		lq.setOperationCode("Test");
		Assert.assertEquals("%Test%", lq.getOperationCode());
		
		lq.setOperationCode("%Test");
		Assert.assertEquals("%Test", lq.getOperationCode());
		lq.setOperationCode("Test%");
		Assert.assertEquals("Test%", lq.getOperationCode());
		
		lq.setOperateTable(null);
		Assert.assertNull(lq.getOperateTable());
		
		lq.setOperateTable("record-");
		Assert.assertEquals("record-", lq.getOperateTable());

		Assert.assertEquals(LogOutputTask.class.getName(), new LogOutputTaskPoolCustomizer().getTaskClass());
		
		LogOutputTask logOutputTask = new LogOutputTask();
		logOutputTask.excute();
		
		List<Object> records = new ArrayList<Object>();
		records.add( new Log() );
		records.add( new Log("xx", new Object()) );
		logOutputTask.fill(records );
		logOutputTask.excute();
		
		List<Log> list = new ArrayList<Log>();
		list.add(log);
		list.add(log);
		
		System.out.println( list );
		
		log.setContent("begin: {f1=停用,aa, f2=null, f3=null, f4=null, domain=null, creator=Admin, version=0, createtime=2019-03-17 16:23:36.0}  " +
				" after: {f1=停用,aa, f2=null, f3=null, f4=2, domain=null, creator=Admin, version=1, createtime=2019-03-17 16:23:36.0}");
		log.formatContent() ;
		System.out.println( log.getContent() );
	}
	
}
