/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.etl;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.exception.ExceptionEncoder;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.IOperator;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MailUtil;

/**
 * ETL Job基类
 */
public abstract class AbstractETLJob extends AbstractJob {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	public static int PAGE_SIZE = 10000;
	
	protected ICommonService commonService = Global.getCommonService();
	protected ILoginService loginService  = (ILoginService) Global.getBean("LoginService");
	protected ReportService reportService = (ReportService) Global.getBean("ReportService");
	protected RecordService recordService = (RecordService) Global.getBean("RecordService");
	
	String currTaskCreator = UMConstants.ROBOT_USER_NAME;
	
	protected IOperator jobRobot() {
        return new OperatorDTO(UMConstants.ROBOT_USER_ID, currTaskCreator); 
	}

	protected String excuteJob(String jobConfig, Long jobID) {
		String hql = "from Task where type = ?1 and status = 'opened' and jobId = ?2 order by priority desc, id asc ";
		List<?> tasks = commonService.getList(hql, etlType(), jobID);
		
		List<String> msgList = new ArrayList<String>();
		for(Object obj : tasks) {
			Task task = (Task) obj;
			
			// 判断是否为定时自动触发Job执行，是则为每个任务单独设置Context
			if( auto ) {
				currTaskCreator = task.getCreator();
				initContext();
			}
			
			TaskLog log = excuteTask( task );
			msgList.add( task.getName() + ": " + (log == null ? "" : log.getDetail()) );
		}
		
		return EasyUtils.list2Str(msgList) + " done";
	}
	
	protected abstract String etlType();

	public abstract TaskLog excuteTask(Task task);
	
	protected void setException(TaskLog tLog, Task task, Exception e) {
		tLog.setException("yes");
		tLog.setDetail( ExceptionEncoder.getFirstCause(e).getMessage() );
		
		log.error(tLog, e);
		
		// 邮件提醒此Task的管理员
		String receiver = (String) EasyUtils.checkNull(task.getManager(), task.getApplier());
		String[] receivers = loginService.getContactInfos( receiver, false );
		MailUtil.send( task.getName() + "ETL Error", tLog.toString(), receivers, MailUtil.DEFAULT_MS);
	}
	
	/**
	 * 执行中定时去轮询Task的状态，如果状态变为closed，则停止执行
	 * @param taskID
	 */
	protected void checkTask(Long taskID) {
		Task task = (Task) commonService.getEntity(Task.class, taskID);
		if(Task.STATUS_OFF.equals(task.getStatus()) ) {
			throw new BusinessException(task.getName() + EX.DM_04 + task.getUpdator());
		}
	}
}
