/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.um.dao.IApplicationDao;
import com.boubei.tss.um.entity.Application;

@Repository("ApplicationDao")
public class ApplicationDao extends BaseDao<Application> implements IApplicationDao {
	
    public ApplicationDao() {
		super(Application.class);
	}
	
	public Application getApplication(String applicationId){
		List<?> list = getEntities("from Application o where o.applicationId = ?1", applicationId);
		return list.size() > 0 ? (Application)list.get(0) : null;
	}
 
    // 删除Operation、ResourceTypeRoot、ResourceType、Application表
	public void clearDirtyData(String applicationId) {
        deleteAll(getEntities("from Operation        where applicationId = ?1", applicationId));
        deleteAll(getEntities("from ResourceTypeRoot where applicationId = ?1", applicationId));
        deleteAll(getEntities("from ResourceType     where applicationId = ?1", applicationId));
        deleteAll(getEntities("from Application      where applicationId = ?1", applicationId));
		flush();
	}
 
}