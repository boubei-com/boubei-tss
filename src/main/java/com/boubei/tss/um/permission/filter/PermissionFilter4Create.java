/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.permission.filter;

import java.util.List;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.permission.IResource;
import com.boubei.tss.um.permission.PermissionHelper;

/**
 * 资源新增时候的权限检查： 判断用户在父节点下是否有新增权限。
 * 
 * IResource create***( IResource obj )
 */
public class PermissionFilter4Create implements IPermissionFilter {
	
	@Override
	public void doFilter(Object args[], Object returnValue, PermissionTag tag, PermissionHelper helper) {
        String application = tag.application();
		String resourceType = tag.resourceType();
		String createOperation = tag.operation();
 
		IResource resource = (IResource) args[0];
		
		List<Long> permitedResourceIds = helper.getResourceIdsByOperation(application, resourceType, createOperation);
        if( !permitedResourceIds.contains(resource.getParentId()) ) {
            throw new BusinessException( EX.parse(EX.U_14, resource.getParentId()) + Environment.getOwnRoleNames() + Environment.getUserCode());
        }
	}

}
