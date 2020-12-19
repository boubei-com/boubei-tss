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

import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ImportRecord implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	RecordService recordService = (RecordService) Global.getBean("RecordService");

	public String processUploadFile(HttpServletRequest request,
			String filepath, String oldfileName) throws Exception {
		
		File targetFile = new File(filepath);
		String json = FileHelper.readFile(targetFile);
            
		String dataSource = request.getParameter("dataSource");
        Long groupId;
        try { 
        	groupId = Long.parseLong(request.getParameter("groupId"));
        } catch(Exception e) {
        	groupId = Record.DEFAULT_PARENT_ID;
        }
        
        int count = createRecords(json, dataSource, groupId);
        
		return "parent.alert('成功导入" +count+ "个数据表定义.');parent.loadInitData();";
	}

	/**
	 * 参考Param模块的【复制】操作;
	 * 
	 * 如果同名 且 同ID 的已存在，则覆盖； 通常是一个环境复制到另外一个环境
	 */
	public int createRecords(String json, String dataSource, Long groupId) throws Exception {
		
        int count = 0;
    	Map<Long, Long> idMapping = new HashMap<Long, Long>();
    	
    	List<?> list = new ObjectMapper().readValue(json, List.class);
        for (int i = 0; i < list.size(); i++) {
        	Object obj = list.get(i);  // Map
            Record record = new ObjectMapper().readValue(EasyUtils.obj2Json(obj), Record.class);
            Long oldId = record.getId();
            
            if( Record.TYPE1 == record.getType() ) {
            	count ++;
            	record.setDatasource(dataSource);
            	
            	String table = record.getTable();
                record.setTable( table.substring(table.indexOf(".") + 1) ); // 去掉表空间|schema
            }
            
            String hql = "from Record where name = ?1 and (id = ?2 or table = ?3)";
            List<?> exists = Global.getCommonService().getList(hql, record.getName(), oldId, EasyUtils.obj2String(record.getTable()));
            if( exists.isEmpty() ) {
            	Integer status = record.getDisabled();
            	record.setId(null);
            	Long parentId = idMapping.get( record.getParentId() );
				record.setParentId( (Long) EasyUtils.checkNull(parentId, groupId) );
				
                recordService.createRecord(record);
                
                record.setDisabled(status); // 因默认创建分组都是停用状态，但导入分组不需要，保留原来状态
                recordService.updateRecord(record);
            }
            else {
            	Record old = (Record) exists.get(0);
            	BeanUtil.copy( old, record, new String[]{"id", "lockVersion","createTime", "creatorName", "decode", "seqNo", "levelNo", "group"} );
            	recordService.updateRecord(old);
            }
            
            idMapping.put(oldId, record.getId());
        }
		return count;
	}
}
