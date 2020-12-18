/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;

@Service("ReportService")
public class ReportServiceImpl implements ReportService {
	
	Logger log = Logger.getLogger(this.getClass());
    
    @Autowired ReportDao reportDao;
    
    public Report getReport(Long id, boolean auth) {
        Report report;
        if(auth) {
        	report = reportDao.getVisibleReport(id); // 如没有访问权限，将抛出异常
        } else {
        	report = reportDao.getEntity(id);
        }
        
        if(report == null) {
			throw new BusinessException( EX.parse(EX.DM_18, id) );
        }
        reportDao.evict(report);
        return report;
    }
    
    public Report getReport(Long id) {
        boolean auth;
        if( Environment.isRobot() ) { // 定时JOB
        	auth = false;
        } else {
        	auth = SecurityUtil.isSafeMode();
        }
		return this.getReport(id, auth);
    }
    
    public Long getReportId(String idCodeName) {
    	Long reportId = null;
    	try { // 先假定是报表ID（Long型）
    		reportId = getReportId("id", Long.valueOf(idCodeName), Report.TYPE1);
    	} 
    	catch(Exception e) { }
    	
    	// 按Code 或 名字再查一遍
    	if(reportId == null) {
    		reportId = getReportId("code", idCodeName, Report.TYPE1);
    	}
    	if(reportId == null) {
    		reportId = getReportId("name", idCodeName, Report.TYPE1);
    	}
    	if(reportId == null) {
    		throw new BusinessException( EX.parse(EX.DM_18, idCodeName) );
    	}
    	
    	return reportId;
	}
    
    public List<?> getReportIds(String fname, Object idCodeName, int type) {
		String hql = "select o.id from Report o where o." +fname+ " = ?1 and type = ?2 order by o.id desc";
		return reportDao.getEntities(hql, idCodeName, type); 
    }
    
	public Long getReportId(String fname, Object idCodeName, int type) {
		List<?> list = getReportIds(fname, idCodeName, type); 
		return list.isEmpty() ? null : (Long) list.get(0);
	}
	
	// 加userId以便于缓存
    public List<Report> getReportsByGroup(Long groupId, Long userId) {
        return reportDao.getChildrenById(groupId);
    }
    
    @SuppressWarnings("unchecked")
    public List<Report> getAllReport() {
        return (List<Report>) reportDao.getEntities("from Report o order by o.decode");
    }
    
    @SuppressWarnings("unchecked")
    public List<Report> getAllReportGroups() {
        return (List<Report>) reportDao.getEntities("from Report o where o.type = ?1 order by o.decode", Report.TYPE0);
    }

    public Report createReport(Report report) {
        Long parentId = report.getParentId();
        Report parent = reportDao.getEntity(parentId);
        if( (parent == null || parent.isActive() ) && report.isGroup() ) {
        	report.setDisabled( ParamConstants.TRUE ); // 报表默认为停用，组看父组的状态
        }
        
        checkReportCode(report, 0);
        
		report.setSeqNo(reportDao.getNextSeqNo(parentId));
        reportDao.create(report);

        return report;
    }
    
    // 检查同code的报表是否已经存在，是的话提示改code
    void checkReportCode(Report report, int limit) {
        String code = report.getCode();
		if(code != null && this.getReportIds("code", code, Report.TYPE1).size() > limit) {
        	throw new BusinessException(EX.parse(EX.DM_33, code));
        }
    }
    
    public void updateReport(Report report) {
    	checkReportCode(report, 1);
    	reportDao.refreshEntity(report);
    }
    
    public Report delete(Long id) {
    	 Report report = getReport(id);
         List<Report> children = reportDao.getChildrenById(id, Report.OPERATION_DELETE); // 一并删除子节点
         return reportDao.deleteReport(report, children);
    }

    public void startOrStop(Long reportId, Integer disabled) {
        List<Report> list = ParamConstants.TRUE.equals(disabled) ? 
                reportDao.getChildrenById(reportId, Report.OPERATION_DISABLE) : reportDao.getParentsById(reportId);
        
        for (Report report : list) {
            report.setDisabled(disabled);
            reportDao.updateWithoutFlush(report);
        }
        reportDao.flush();
    }

    public void sort(Long startId, Long targetId, int direction) {
        reportDao.sort(startId, targetId, direction);
    }

    public List<Report> copy(Long reportId, Long groupId) {
        Report report = getReport(reportId);
        
        reportDao.evict(report);
        report.setId(null);
        report.setParentId(groupId);
        report.setSeqNo(reportDao.getNextSeqNo(groupId));
        report.setDisabled(ParamConstants.TRUE); // 新复制出来的节点都为停用状态
        
        report = reportDao.create(report);
        List<Report> list = new ArrayList<Report>();
        list.add(report);
        
        return list;
    }

    public void move(Long id, Long groupId) {
        Report node = reportDao.getEntity(id);
        node.setSeqNo(reportDao.getNextSeqNo(groupId));
        node.setParentId(groupId);
        reportDao.moveEntity(node);
		
        Report group = reportDao.getEntity(groupId);
		if (group != null && !group.isActive() ) {
			List<Report> list  = reportDao.getChildrenById(id);
	        for (Report temp : list) {
	            temp.setDisabled(ParamConstants.TRUE); // 如果目标根节点是停用状态，则所有新复制出来的节点也一律为停用状态
	            reportDao.update(temp);
	        }
		}
    }
    
  	public SQLExcutor queryReport(Long reportId, Map<String, String> requestMap, 
  			int page, int pagesize, Object cacheFlag) {
    	
    	Report report = this.getReport(reportId);
    	SQLExcutor ex = ReportQuery.excute(report, requestMap, page, pagesize);
    	String colDefs = report.getColDefs();
    	if(colDefs != null && ex.count > 0) {
    		String[] arr = colDefs.replaceAll("，", ",").split(",");
    		int size = Math.min(ex.fieldWidths.size(), arr.length);
    		for(int i = 0; i < size; i++) {
    			ex.fieldWidths.set(i, arr[i]);
    		}
    	}
		
		return ex;
    }
   
}