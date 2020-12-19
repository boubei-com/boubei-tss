/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ext;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ImportReport implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	ReportService reportService = (ReportService) Global.getBean("ReportService");

	public String processUploadFile(HttpServletRequest request,
			String filepath, String oldfileName) throws Exception {
		
		File targetFile = new File(filepath);
		String json = FileHelper.readFile(targetFile);
            
		String dataSource = request.getParameter("dataSource");
        Long groupId;
        try { 
        	groupId = Long.parseLong(request.getParameter("groupId"));
        } catch(Exception e) {
        	groupId = Report.DEFAULT_PARENT_ID;
        }
        
        int count = createReports(json, dataSource, groupId);
        
		return "parent.alert('成功导入" +count+ "个报表.');parent.loadInitData();";
	}

	/**
	 * 参考Param模块的【复制】操作;
	 * 
	 * 如果同名 且 同ID 的已存在，则覆盖； 通常是一个环境复制到另外一个环境
	 */
	public int createReports(String json, String dataSource, Long groupId) throws Exception {
		
        int count = 0;
    	Map<Long, Long> idMapping = new HashMap<Long, Long>();
    	
    	List<?> list = new ObjectMapper().readValue(json, List.class);
        for (int i = 0; i < list.size(); i++) {
        	Object obj = list.get(i);  // Map
            Report report = new ObjectMapper().readValue(EasyUtils.obj2Json(obj), Report.class);
            Long oldId = report.getId();
            
            if( !report.isGroup() ) {
            	count ++;
            	report.setDatasource(dataSource);
            }
            
            String hql = "from Report where name = ?1 and (id = ?2 or code = ?3)";
            List<?> exists = Global.getCommonService().getList(hql, report.getName(), oldId, EasyUtils.obj2String(report.getCode()));
            if( exists.isEmpty() ) {
            	Integer status = report.getDisabled();
            	report.setId(null);
            	Long parentId = idMapping.get( report.getParentId() );
            	report.setParentId( (Long) EasyUtils.checkNull(parentId, groupId) );
                reportService.createReport(report);
                
                report.setDisabled(status); // 因默认创建分组都是停用状态，但导入分组不需要，保留原来状态
                reportService.updateReport(report);
            }
            else {
            	Report old = (Report) exists.get(0);
            	BeanUtil.copy( old, report, new String[]{"id", "lockVersion","createTime", "creatorName", "decode", "seqNo", "levelNo", "group"} );
            	reportService.updateReport(old);
            }
            
            idMapping.put(oldId, report.getId());
        }
		return count;
	}
}
