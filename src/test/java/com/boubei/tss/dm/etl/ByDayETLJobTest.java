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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cms.job.PublishJob;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportDao;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.modules.timer.JobAction;
import com.boubei.tss.modules.timer.JobDef;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.MathUtil;

public class ByDayETLJobTest extends AbstractTest4DM {
	
	@Autowired ReportService reportService;
	@Autowired RecordService recordService;
	@Autowired ReportDao reportDao;
	
	@Autowired TaskAction taskAction;
	@Autowired JobAction jobAction;
	
	JobDef job;
	Long record1Id;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		int index = 0;
        while(index++ < 108) {
        	 User temp = new User();
        	 temp.setLoginName("U-" + index);
        	 temp.setUserName(temp.getLoginName());
        	 temp.setBirthday( DateUtil.subDays(new Date(), MathUtil.randomInt(100)%5) );
        	 reportDao.createObject(temp);
        }
        
        job = new JobDef();
		job.setCode("JOB-1");
		job.setName("JOB-1");
		job.setJobClassName(ByDayETLJob.class.getName());
		job.setTimeStrategy("0 0 01 * * ?");
		job.setCustomizeInfo("");
		job.setDisabled(0);
		job.setDescription("模块使用说明");
		job.setRemark("test job");
		reportDao.createObject(job);
		
		CacheHelper.getShorterCache().flush();
		Assert.assertEquals(" done", jobAction.exucteJob( job.getId().toString() ) );
		Assert.assertEquals(" done", jobAction.exucteJob( job.getCode() ) );

		try {
			jobAction.exucteJob( "notExistJob" );
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.XX_NOT_FOUND, "notExistJob", "Job"), e.getMessage());
        }
		
		String tblDefine = "[ {'label':'类型', 'code':'code', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'name', 'nullable':'false'} ," +
        		"{'label':'日期', 'code':'birthday', 'type':'date'} ]";
		Record record1 = new Record();
		record1.setName("record-ETL-1");
		record1.setType(1);
		record1.setParentId(0L);
		record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record1.setTable("tbl_ETL_1");
		record1.setDefine(tblDefine);
		
		recordService.createRecord(record1);
		record1Id = record1.getId();
	}
	
    protected String getDefaultSource(){
    	return "connectionpool";
    }

	@Test
	public void test1() {
		// create a report
        Report report = new Report();
        report.setType(Report.TYPE1);
        report.setParentId(Report.DEFAULT_PARENT_ID);
        report.setName("report-ETL-1");
        report.setScript(" select id as code, loginName as name, birthday from um_user where 1=1 and birthday >= ? and birthday < ?");
        String paramsConfig = "[ {'label':'日期从', 'type':'date'}, {'label':'日期到', 'type':'date'} ]"	;
        report.setParam(paramsConfig);
        
        reportService.createReport(report);
        Long reportId = report.getId();
		
		Task task = new Task();
		task.setName("ETL-TASK-1");
		task.setType("byDay");
		task.setApplier("JK");
		task.setApplyDay(new Date());
		task.setDescription("test");
		task.setJobId(job.getId());
		task.setJobName(job.getName());
		task.setPriority(100);
		task.setManager("Admin");
		task.setRemark("test");
		task.setRepeatDays(3);
		task.setSourceDS(getDefaultSource());
		task.setSourceScript( reportId.toString() );
		task.setStartDay( DateUtil.subDays(new Date(), 4) );
		task.setStatus("opened");
		task.setTargetDS(getDefaultSource());
		task.setTargetScript(record1Id.toString());
		reportDao.createObject(task);
        
		new ByDayETLJob().excuteJob(null, job.getId());
		
		Task copy = new Task();
		BeanUtil.copy(copy, task);
		copy.setId(null);
		copy.getPK();
		
		JobAction jobAction = new JobAction();
		jobAction.refresh();
		jobAction.listJobs("");
		
		Long taskId = task.getId();
		taskAction.exucteTask(taskId.toString());
		taskAction.disableTask(taskId);
		taskAction.enableTask(taskId);
		
		report.setScript( report.getScript() + " and 1=?" );
		reportDao.update(report);
		String qckey = "com.boubei.tss.modules.timer.JobService.excuteTask(" +taskId+ ", -1)";
		try {
			CacheHelper.getShorterCache().destroyByKey(qckey);
			taskAction.exucteTask(task.getName());
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue( e.getMessage().indexOf("Parameter #3 is not set") > 0 );
        }
		
		try {
			taskAction.exucteTask("noExistETL");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.XX_NOT_FOUND, "noExistETL", "ETL"), e.getMessage());
        }
		
		task.setRepeatDays(0);
		reportDao.update(task);
		CacheHelper.getShorterCache().destroyByKey(qckey);
		Assert.assertEquals(EX.DM_30, taskAction.exucteTask(task.getName()));
		
		// test error 
		JobDef job2 = new JobDef();
		job2.setCode("JOB-1p");
		job2.setName("JOB-1p");
		job2.setJobClassName(PublishJob.class.getName());
		job2.setTimeStrategy("0 0 01 * * ?");
		reportDao.createObject(job2);
		
		task.setJobId( job2.getId() );
		task.setJobName( job2.getName() );
		reportDao.update(task);
		
		try {
			CacheHelper.getShorterCache().destroyByKey(qckey);
			taskAction.exucteTask(taskId.toString());
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.DM_28, job2.getName(), task.getType(), PublishJob.class.getSimpleName()) , e.getMessage());
        }
	}
	
	@Test
	public void test2() {
		Task task = new Task();
		task.setName("ETL-TASK-2");
		task.setType("byDay");
		task.setApplier("JK");
		task.setApplyDay(new Date());
		task.setDescription("test");
		task.setJobId(job.getId());
		task.setJobName(job.getName());
		task.setManager("Admin,jinhetss@163.com");
		task.setRemark("test");
		task.setRepeatDays(3);
		task.setSourceDS(getDefaultSource());
		task.setSourceScript(" select id as code, loginName as name, birthday from um_user where 1=1 and birthday >= ? and birthday < ?");
		task.setStartDay( DateUtil.subDays(new Date(), 4) );
		task.setStatus("opened");
		task.setTargetDS(getDefaultSource());
		task.setTargetScript( "insert into tbl_ETL_1(code,name,birthday,creator,createtime,version) values(?,?,?,'JK',sysdate,0) " );
		task.setPreRepeatSQL("delete from tbl_ETL_1 t where birthday = ?");
		reportDao.createObject(task);
        
		// test insert batch
		new ByDayETLJob().excuteJob(null, job.getId());
		
		// test update batch
		task.setRepeatDays(1);
		task.setPreRepeatSQL(null);
		task.setTargetScript( "update tbl_ETL_1 set code = ?+1000, name = 'J.K' where name = ? and birthday = ? " );
		reportDao.update(task);
		new ByDayETLJob().excuteJob(null, job.getId());
		
		// test precheat: truncate table
		task.setRepeatDays(0);
		task.setStartDay( DateUtil.subDays(new Date(), 7) );
		task.setPreRepeatSQL("truncate table tbl_ETL_1");
		reportDao.update(task);
		new ByDayETLJob().excuteJob(null, job.getId());
		
		task.setPreRepeatSQL("");
		reportDao.update(task);
		new ByDayETLJob().excuteJob(null, job.getId());
		
		// test error case： Invalid value "2" for parameter "parameterIndex" (多个一个参数param2)
		task.setSourceScript(" select id as code, loginName as name, birthday from um_user where 1=1 and birthday = ?");
		reportDao.update(task);
		try {
			new ByDayETLJob().excuteJob(null, job.getId());
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
			log.debug( t );
		}
		TaskLog copy = new TaskLog();
		BeanUtil.copy(copy, logs.get(0));
		copy.setId(null);
		copy.getPK();
		
		log.debug( "-------------" + 
				SQLExcutor.query(getDefaultSource(), "select count(*) as total from tbl_ETL_1").get(0).get("total") );
		log.debug( SQLExcutor.query(getDefaultSource(), "select * from tbl_ETL_1 order by id desc") );
		
		reportDao.deleteAll( reportDao.getEntities("from User")  );
	}
}
