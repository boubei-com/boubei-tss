/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report.timer;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.timer.JobDef;

public class ReportJobTest extends AbstractTest4DM {
	
	@Autowired private ReportService service;
	
	JobDef jobDef;
	
	protected void init() {
    	super.init();
    	
    	jobDef = new JobDef();
		jobDef.setCode("ReportJob-1");
		jobDef.setName("ReportJob-1");
		jobDef.setJobClassName(ReportJob.class.getName());
		jobDef.setTimeStrategy("0 0 01 * * ?");
		jobDef.setCustomizeInfo("");
		jobDef.setDisabled(0);
		jobDef.setRemark("test job");
		commonDao.createObject(jobDef);
        
        if(paramService.getParam(PX.EMAIL_MACRO) == null) {
        	Param paramL = ParamManager.addComboParam(ParamConstants.DEFAULT_PARENT_ID, PX.EMAIL_MACRO, "常用收件人组");
    		ParamManager.addParamItem(paramL.getId(), "jinhetss@163.com", "JK", ParamConstants.COMBO_PARAM_MODE);
        }
    }

	@Test
	public void testReportJob() {
		
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(Report.DEFAULT_PARENT_ID);
        report1.setName("report-job-test-012");
        report1.setScript(" select t.id, t.name, null as udf1 from dm_report t, view_report_resource v " +
        		" where t.id > ? <#if param2??> and t.type <> ${param2} <#else> and t.type = 1 </#if>" +
        		" <#if param3??> and t.createTime > ? </#if>");
        report1.setDisplayUri("template.html");
        
        String paramsConfig = 
        		"[ {'label':'报表ID', 'type':'Number', 'nullable':'false', 'jsonUrl':'../xxx/list', 'multiple':'true'}," +
        		  "{'label':'报表类型', 'type':'String'}," +
        		  "{'label':'创建时间', 'type':'date'}]"	;
        report1.setParam(paramsConfig);
        report1.setDatasource(DMConstants.LOCAL_CONN_POOL);
        
        service.createReport(report1);
        
        for(int i=0; i <= 40; i++) {
        	Report rg = new Report();
        	rg.setType(Report.TYPE0);
        	rg.setParentId(Report.DEFAULT_PARENT_ID);
        	rg.setName("rGroup" + i);
        	service.createReport(rg);
        }
        
        ReportJob job = new ReportJob();
        Assert.assertTrue(job.needSuccessLog());
        
        String jobConfig = "\n" +
        				   report1.getId() + ":报表一:jinhetss@163.com,BL00618,-1@tssRole,-2@tssGroup,${JK}:param1=0,param2=0,param3=today-0\n" + 
        		           report1.getId() + ":报表二:BL00618,jinhetss@163.com:param1=0,param3=today-0\n" +
        		           report1.getId() + ":报表三:BL00618,jinhetss@163.com:param1=0,param3=today+0\n" +
        		           report1.getId() + ":报表三:BL00618,jinhetss@163.com:param1=0,param2=1\n" +
        		           report1.getId() + ":报表四:BL00618,jinhetss@163.com\n" +
        		           report1.getId() + ":报表六:BL00619:param1=0,param3\n" +
        		           " \n" +
        		           report1.getId() + ":报表五\n";
		try{
        	job.excuteJob(jobConfig);
		} catch(Exception e) {
        	log.error(e.getCause());
        }
		
		ReportJob.MAX_ROWS = 10;
		job = new ReportJob();
		job.excuteJob(report1.getId() + ":报表一:jinhetss@163.com:param1=0,param2=0,param3=today-0\n", jobDef.getId());
		
		job.excuteJob(report1.getId() + ":报表一:jinhetss@163.com:param1=0,param2=0,param3=today+1\n", jobDef.getId());
	}

}
