/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um;

import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4TSS;
import com.boubei.tss.um.permission.ResourcePermission;
import com.boubei.tss.um.service.IGroupService;

public abstract class AbstractTest4UM extends AbstractTest4TSS { 
 
	@Autowired protected ResourcePermission resourcePermission;
	@Autowired protected IGroupService groupService;
 
    /**
     * 初始化UM、CMS、Portal相关应用、资源类型、权限选型信息
     */
    protected void init() {
        super.init();
        
        // 补全SQL初始化出来的系统级用户组
        Long[] groupIds = new Long[] {-1L, -2L, -3L, -7L, -8L, -9L};
        for(Long groupId : groupIds) {
        	resourcePermission.addResource(groupId, UMConstants.GROUP_RESOURCE_TYPE_ID);
        }
    }
  
}
