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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportDao;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.modules.timer.JobDef;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.MathUtil;

import org.junit.Assert;

public class ByIDETLJobTest extends AbstractTest4DM {
	
	@Autowired ReportService reportService;
	@Autowired RecordService recordService;
	@Autowired ReportDao reportDao;
	
	JobDef job;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		int index = 0;
        while(index++ < 10) {
        	 User temp = new User();
        	 temp.setLoginName("U-" + index);
        	 temp.setUserName(temp.getLoginName());
        	 temp.setBirthday( DateUtil.subDays(new Date(), MathUtil.randomInt(100)%5) );
        	 reportDao.createObject(temp);
        }
        
        job = new JobDef();
		job.setCode("JOB-12");
		job.setName("JOB-12");
		job.setJobClassName(ByIDETLJob.class.getName());
		job.setTimeStrategy("0 0/5 * * * ?");
		job.setCustomizeInfo("");
		job.setDisabled(0);
		reportDao.createObject(job);
		
		// 创建一个表
		SQLExcutor.excute("create table if not exists tbl_ETL_12" + 
                "(" + 
                    "id           NUMBER(19) not null, " + 
                    "name         VARCHAR2(50 CHAR), " + 
                    "birthday     TIMESTAMP" + 
                ");" +
                " alter table tbl_ETL_12 add primary key (id); ", getDefaultSource());
	}
	
    protected String getDefaultSource() {
    	return "connectionpool";
    }

	@Test
	public void test () {
		// create a report
        Report report = new Report();
        report.setType(Report.TYPE1);
        report.setParentId(Report.DEFAULT_PARENT_ID);
        report.setName("report-ETL-12");
        report.setScript(" select id, loginName as name, birthday from um_user where 1=1 and id > ?");
        String paramsConfig = "[ " +
    			"{'label':'从ID', 'type':'Number', 'nullable':'false'}" +
    		"]";
        report.setParam(paramsConfig);
        
        reportService.createReport(report);
        Long reportId = report.getId();
		
		Task task = new Task();
		task.setName("ETL-TASK-12");
		task.setType("byID");
		task.setApplier("JK");
		task.setApplyDay(new Date());
		task.setJobId(job.getId());
		task.setJobName(job.getName());
		task.setManager("Admin");
		task.setSourceDS(getDefaultSource());
		task.setSourceScript( reportId.toString() );
		task.setStartID(0L);
		task.setStatus("opened");
		task.setTargetDS(getDefaultSource());
		task.setTargetScript( "insert into tbl_ETL_12(id,name,birthday) values(?,?,?) " );
		reportDao.createObject(task);
        
		ByIDETLJob etlJob = new ByIDETLJob();
		ByIDETLJob.PAGE_SIZE = 1;
		etlJob.auto = true;
		etlJob.excuteJob(null, job.getId());
		
		Assert.assertEquals(task.getCreator(), etlJob.jobRobot().getLoginName());
 
		
		// test 违反ID唯一性
//		reportDao.deleteAll( reportDao.getEntities("from TaskLog")  );
		task.setSourceScript(" select id, loginName as name, birthday from um_user where 1=1 and id > ? ");
		reportDao.update(task);
		etlJob.excuteJob(null, job.getId());
		
		// test update batch
		reportDao.deleteAll( reportDao.getEntities("from TaskLog")  );
		task.setRepeatDays(1);
		task.setPreRepeatSQL(null);
		task.setSourceScript(" select loginName as name, birthday, id from um_user where 1=1 and id > ? ");
		task.setTargetScript( "update tbl_ETL_12 set name = ?, birthday = ? where id = ?" );
		reportDao.update(task);
		etlJob.excuteJob(null, job.getId());
		
		// test error case：Column "NAMEXXX" not found
		reportDao.deleteAll( reportDao.getEntities("from TaskLog")  );
		
		task.setPreRepeatSQL("select max(id)-100 maxid from tbl_ETL_12");
		task.setTargetScript( "insert into tbl_ETL_12(id,nameXXX,birthday) values(?,?,?) " );
		reportDao.update(task);
		String result = etlJob.excuteJob(null, job.getId());
		Assert.assertTrue(result.indexOf("Column NAMEXXX not found") >= 0);
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		
		List<?> logs = reportDao.getEntities("from TaskLog");
		for(Object t : logs) {
			log.debug( t );
		}
		log.debug( SQLExcutor.query(getDefaultSource(), "select * from tbl_ETL_12 order by id desc") );
		
		reportDao.deleteAll( reportDao.getEntities("from User")  );
	}
}
