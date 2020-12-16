/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.etl;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.ReportDao;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.util.DateUtil;

public class StopTaskTest extends AbstractTest4DM {
	
	@Autowired ReportService reportService;
	@Autowired RecordService recordService;
	@Autowired ReportDao reportDao;
 
	@Test
	public void testWsID() {
		String sql = "select 1 as id from dual union select id FROM dm_etl_task WHERE id > ?";
		
		Task task = new Task();
		task.setName("wsDay");
		task.setType("wsDay");
		task.setApplier("JK");
		task.setApplyDay(new Date());
		task.setDescription("test");
		task.setJobId(1L);
		task.setSourceDS(getDefaultSource());
		task.setSourceScript(sql);
		task.setStartID(0L);
		task.setTargetDS(getDefaultSource());
		task.setTargetScript( " " );
		task.setStatus(Task.STATUS_OFF);
		commonDao.createObject(task);
        
		ByIDETLJob job = new ByIDETLJob();
		job.excuteTask(task);
	}
	
	@Test
	public void testWsDay() {
		String sql = "select 1 as id from dual union select id from dm_etl_task where createTime >= ? and createTime < ?";
		Task task = new Task();
		task.setName("wsDay");
		task.setType("wsDay");
		task.setApplier("JK");
		task.setApplyDay(new Date());
		task.setDescription("test");
		task.setJobId(1L);
		task.setSourceDS(getDefaultSource());
		task.setSourceScript(sql);
		task.setStartDay( DateUtil.subDays(new Date(), 4) );
		task.setTargetDS(getDefaultSource());
		task.setTargetScript( " " );
		task.setStatus(Task.STATUS_OFF);
		
		commonDao.createObject(task);
        
		ByDayETLJob job = new ByDayETLJob();
		job.excuteTask(task);
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		List<?> logs = reportDao.getEntities("from TaskLog");
		for(Object t : logs) {
			log.debug(t);
		}
	}
}
