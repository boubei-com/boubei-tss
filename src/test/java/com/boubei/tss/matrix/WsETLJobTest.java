/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.matrix;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.etl.Task;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.ReportDao;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.util.DateUtil;

import junit.framework.Assert;

public class WsETLJobTest extends AbstractTest4DM {
	
	@Autowired ReportService reportService;
	@Autowired RecordService recordService;
	@Autowired ReportDao reportDao;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		int index = 0;
		String tblDefine = "[ {'label':'类型', 'code':'code', 'type':'number', 'nullable':'false'}]";
		
        while(index++ < 12) {
        	Record record1 = new Record();
        	record1.setName("record-ws-etl-"+index);
    		record1.setType(1);
    		record1.setParentId(0L);
    		record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
    		record1.setTable("tbl_ETL_1");
    		record1.setDefine(tblDefine);
    		record1.setBatchImp(1);
    		recordService.createRecord(record1);
        }
	}
	
    protected String getDefaultSource(){
    	return "connectionpool";
    }

	@Test
	public void testWsID() {
		String sql = "SELECT id, operatetable, operationcode, operatetime, operatorip, operatorbrowser, operatorname, content " +
				" FROM component_log WHERE operateTime > '2017-07-01' and id > ?";
		
		Task task = new Task();
		task.setName("wsDay");
		task.setType("wsDay");
		task.setApplier("JK");
		task.setApplyDay(new Date());
		task.setDescription("test");
		task.setJobId(null);
		task.setSourceDS(getDefaultSource());
		task.setSourceScript(sql);
		task.setStartID(0L);
		task.setStatus("opened");
		task.setTargetDS("restful-ws");
		task.setTargetScript( "6" );
        
		try {
			WsIDETLJob job = new WsIDETLJob();
			Assert.assertEquals("wsID", job.etlType());
			
			job.etlByID(task, 0L);
			job.etlByID(task, 10000L);
		} 
		catch(Exception e) {
        	log.error(e.getCause(), e);
        }
	}
	
	@Test
	public void testWsDay() {
		String sql = "select name, createTime ct, updateTime ut, lockVersion, rctable, define, datasource, customizejs, customizepage, customizetj, " +
				" customizeGrid, needlog, needfile, batchimp " +
				" from dm_record where type = 1 and createTime >= ? and createTime < ?";
		
		Task task = new Task();
		task.setName("wsDay");
		task.setType("wsDay");
		task.setApplier("JK");
		task.setApplyDay(new Date());
		task.setDescription("test");
		task.setJobId(null);
		task.setSourceDS(getDefaultSource());
		task.setSourceScript(sql);
		task.setStartDay( DateUtil.subDays(new Date(), 4) );
		task.setStatus("opened");
		task.setTargetDS("restful-ws");
		task.setTargetScript( "7" );
        
		try {
			WsDayETLJob job = new WsDayETLJob();
			Assert.assertEquals("wsDay", job.etlType());
			
			job.etlByDay(task, DateUtil.noHMS(new Date()), null, false);
			job.etlByDay(task, DateUtil.addDays(new Date(), 12), null, false);
		} 
		catch(Exception e) {
        	log.error(e.getCause(), e);
        }
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		List<?> logs = reportDao.getEntities("from TaskLog");
		for(Object t : logs) {
			log.debug(t);
		}
		reportDao.deleteAll( reportDao.getEntities("from Record")  );
	}
}
