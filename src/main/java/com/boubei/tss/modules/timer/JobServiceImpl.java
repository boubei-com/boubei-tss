package com.boubei.tss.modules.timer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.etl.AbstractETLJob;
import com.boubei.tss.dm.etl.Task;
import com.boubei.tss.dm.etl.TaskLog;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.modules.log.IBusinessLogger;
import com.boubei.tss.util.BeanUtil;

@Service("JobService")
public class JobServiceImpl implements JobService {
	
	@Autowired ICommonDao commonDao;
	@Autowired IBusinessLogger businessLogger;
	
	public String excuteJob(String jobKey, Object tag) {
		List<?> list = commonDao.getEntities("from JobDef where ?1 in (id, code) ", jobKey);
		if( list.isEmpty() ) {
			throw new BusinessException( EX.parse(EX.XX_NOT_FOUND, jobKey, "Job") );
		}
		
		JobDef jobDef = (JobDef) list.get(0);
		String jobClass = jobDef.getJobClassName();
		
		AbstractJob job = (AbstractJob) BeanUtil.newInstanceByName(jobClass);
		String resultMsg = job.excuting(jobDef.getCode(), jobDef.getCustomizeInfo(), jobDef.getId() );
		
		return resultMsg;
	}

	public String excuteTask(String idOrName, Object tag) {
		List<?> list = commonDao.getEntities("from Task where ?1 in (id, name) ", idOrName);
		if( list.isEmpty() ) {
			throw new BusinessException( EX.parse(EX.XX_NOT_FOUND, idOrName, "ETL") );
		}
		
		Task task = (Task) list.get(0);
		JobDef jobDef = (JobDef) commonDao.getEntity(JobDef.class, task.getJobId());
		String jobClass = jobDef.getJobClassName();
		
		Object job = BeanUtil.newInstanceByName(jobClass);
		String simpleName = job.getClass().getSimpleName();
		
		// byID、byDay必须严格匹配自己的定时器，不能串
		if( job instanceof AbstractETLJob && simpleName.toLowerCase().startsWith(task.getType().toLowerCase()) ) {
			TaskLog log = ((AbstractETLJob)job).excuteTask(task);
			if( log == null ) {
				return EX.DM_30;
			}
			else if( "yes".equals( log.getException() ) ) {
				throw new BusinessException(task.getJobName() + EX._ERROR_TAG + log.getDetail() );
			}
			return "Success! result: " + log.getDetail() +", maxID="+ log.getMaxID() +", lastday="+ log.getDataDay();
		}
		else {
			throw new BusinessException( EX.parse(EX.DM_28, task.getJobName(), task.getType(), simpleName) );
		}
	}

}
