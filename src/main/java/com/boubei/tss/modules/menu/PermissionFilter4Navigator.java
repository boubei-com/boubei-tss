/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.menu;

import java.util.Iterator;
import java.util.List;

import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.permission.filter.IPermissionFilter;
import com.boubei.tss.um.permission.filter.PermissionTag;

/**
 * 菜单权限过滤拦截器。
 */
public class PermissionFilter4Navigator implements IPermissionFilter {

	@Override
	public void doFilter(Object args[], Object returnValue, PermissionTag tag, PermissionHelper helper) {
        List<?> resources = (List<?>) returnValue;
        
        String application = UMConstants.TSS_APPLICATION_ID;
        String navigatorResourceType = PortalConstants.NAVIGATOR_RESOURCE_TYPE;
        
        List<Long> menuPermissions = helper.getResourceIdsByOperation(application, navigatorResourceType, PortalConstants.NAVIGATOR_VIEW_OPERRATION);
        
        for(Iterator<?> it = resources.iterator(); it.hasNext();) { 
            Navigator menu = (Navigator) it.next();
            
            // 菜单本身授权过滤。
            if( !menuPermissions.contains(menu.getId()) ){
                it.remove();
                continue;
            }
        }
	}

}
