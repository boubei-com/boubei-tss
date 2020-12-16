/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report.log;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.report.ReportAction;

public class AccessLogTest extends AbstractTest4DM {
	
	@Autowired XXService service;
	@Autowired ReportAction action;
	
   protected void init() {
    	super.init();
    	
    	for(int i = 0; i < 30; i++) {
			service.report1(i, new Date(), new Object());
		}
		
		try {
			Thread.sleep(1*1000);
		} catch (InterruptedException e) {
		}
   }
	
	@Test
	public void test1() {
		List<?> logs = permissionHelper.getEntities("from AccessLog");
		Assert.assertTrue(logs.size() >= 0);
		AccessLog firstLog = (AccessLog) logs.get(0);
		firstLog.setOrigin("WX");
		firstLog.getOrigin();
		firstLog.setId((Long) firstLog.getPK());
		Assert.assertEquals(firstLog.getId(), firstLog.getPK());
		
		new AccessLog();
	}
	
	@Test
	public void test2() throws InterruptedException {
		SQLExcutor ex = new SQLExcutor();
		
		String sql = "select id, methodName as 方法名称 from dm_access_log";
		ex.excuteQuery(sql, getDefaultSource());
		
		ex.excuteQuery(sql, new HashMap<Integer, Object>(), getDefaultSource());
		Assert.assertTrue(ex.result.size() > 10);
	}
	
}
