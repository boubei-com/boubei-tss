/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.timer;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.PX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

public class SchedulerBeanTest extends AbstractTest4F {
	
	@Autowired JobAction jobAction;
	
	@Test
	public void testSchedulerBean() {
		JobDef job1 = new JobDef();
		job1.setCode("JOB-1");
		job1.setName("JOB-1");
		job1.setJobClassName(DemoJob.class.getName());
		job1.setTimeStrategy("10,20,30,40,55 * * * * ?");
		job1.setCustomizeInfo("");
		job1.setDisabled(0);
		commonDao.createObject(job1);
		
		JobDef job2 = new JobDef();
		BeanUtil.copy(job2, job1);
		job2.setCode("JOB-2");
		job2.setName("JOB-2");
		job2.setDisabled(1);
		job2.setId(null);
		job2.getPK();
		commonDao.createObject(job2);
		
		JobDef job3 = new JobDef();
		job3.setCode("JOB-3");
		job3.setName("JOB-3");
		job3.setJobClassName(ErrorJob.class.getName());
		job3.setTimeStrategy("12,22,32,42,52 * * * * ?");
		job3.setCustomizeInfo("X");
		commonDao.createObject(job3);
		
		JobDef job4 = new JobDef();
		job4.setCode("JOB-4");
		job4.setName("JOB-4");
		job4.setJobClassName(Demo2Job.class.getName());
		job4.setTimeStrategy("11,21,31,41,51 * * * * ?");
		job4.setCustomizeInfo("X");
		commonDao.createObject(job4);
		
		JobDef job5 = new JobDef();
		job5.setCode("JOB-5");
		job5.setName("JOB-5");
		job5.setJobClassName(Demo2Job.class.getName());
		job5.setTimeStrategy("wrong_time_strategy");
		job5.setCustomizeInfo("X");
		commonDao.createObject(job5);
		
		Global.schedulerBean.refresh(true);
 
		// 修改、新增、删除定时配置
		job1.setCustomizeInfo("X");
		commonDao.update(job1);
		Global.schedulerBean.refresh(false);
		
		job1.setDisabled(1);
		commonDao.update(job1);
		Global.schedulerBean.refresh(false);
		
		job1.setDisabled(0);
		commonDao.update(job1);
		Global.schedulerBean.refresh(false);
		
		commonDao.delete(JobDef.class, job2.getId());
		Global.schedulerBean.refresh(false);
		
		// DemoJob配了每分钟里10,20,30,40,55执行
		try { Thread.sleep(1000 * 22); } catch (InterruptedException e) { }
		
		JobAction jobAction = new JobAction();
		jobAction.refresh();
		jobAction.listJobs("");
		
		_TestUtil.printLogs(logService);
	}
	
	@Test
	public void testParseConfig() {
		String jobConfig = getJobConfig(DemoJob.class.getName());
		
    	String[] array = EasyUtils.split(jobConfig, "|");
		Assert.assertTrue( array.length == 3 );
		Assert.assertEquals( array[0].trim(), DemoJob.class.getName() );
		Assert.assertEquals( array[1].trim(), "10,20,30,40,55 * * * * ?");
    	
    	array = EasyUtils.split(array[2].trim(), "\n");
    	Assert.assertTrue( array.length == 2 );
		
		Assert.assertTrue( EasyUtils.split(array[1].trim(), ":").length == 4);
		
		Config.setProperty(PX.ENABLE_JOB, "false");
		new SchedulerBean().refresh(true);
		Config.setProperty(PX.ENABLE_JOB, "true");
	}

	private String getJobConfig(String jobClazz) {
		String jobConfig = jobClazz + " | 10,20,30,40,55 * * * * ? | " +
				"1:报表一:lovejava@163.com,lovejava@163.com:param1=0,param2=0\n" + 
	            "2:报表二:lovejava@163.com,lovejava@163.com:param1=0"; 
		return jobConfig;
	}
}
