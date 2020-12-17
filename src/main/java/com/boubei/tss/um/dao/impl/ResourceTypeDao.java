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

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.um.dao.IResourceTypeDao;
import com.boubei.tss.um.entity.ResourceType;
import com.boubei.tss.um.entity.ResourceTypeRoot;

@Repository("ResourceTypeDao")
public class ResourceTypeDao extends BaseDao<ResourceType> implements IResourceTypeDao {
 
	public ResourceTypeDao() {
		super(ResourceType.class);
	}
 
	public ResourceTypeRoot getResourceTypeRoot(String applicationId,String resourceTypeId){
		String hql = " from ResourceTypeRoot o where upper(o.applicationId) = ?1 and o.resourceTypeId = ?2";
        List<?> list = getEntities(hql, applicationId.toUpperCase(), resourceTypeId );
        if (list.isEmpty()) {
			throw new BusinessException(EX.parse(EX.U_09, applicationId, resourceTypeId));
		}
        return (ResourceTypeRoot)list.get(0);
	}
 
    public ResourceType getResourceType(String applicationId, String resourceTypeId) {
    	String hql = " from ResourceType o where upper(o.applicationId) = ?1 and o.resourceTypeId = ?2";
		List<?> list = getEntities(hql, applicationId.toUpperCase(), resourceTypeId);
		if (list.isEmpty()) {
			throw new BusinessException(EX.parse(EX.U_10, applicationId, resourceTypeId));
		}
        return (ResourceType) list.get(0);
    }
 
    public String getPermissionTable(String applicationId, String resourceTypeId) {
        return getResourceType(applicationId, resourceTypeId).getPermissionTable();
    }
 
    public String getResourceTable(String applicationId, String resourceTypeId) {
        return getResourceType(applicationId, resourceTypeId).getResourceTable();
    }
 
    public List<?> getOperationIds(String applicationId, String resourceTypeId) {
        String hql = "select t.operationId from Operation t where upper(t.applicationId) = ?1 and t.resourceTypeId = ?2 order by t.seqNo";
        return getEntities(hql, applicationId.toUpperCase(), resourceTypeId);
    }
 
    public List<?> getOperations(String applicationId, String resourceTypeId) {
        String hql = " from Operation t where upper(t.applicationId) = ?1 and t.resourceTypeId = ?2 order by t.seqNo";
        return getEntities(hql, applicationId.toUpperCase(), resourceTypeId);
    }
}