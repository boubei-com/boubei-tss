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

import javax.persistence.Entity;
import javax.persistence.Table;

import com.boubei.tss.um.permission.AbstractResource;

/** 
 * 导航栏资源视图
 */
@Entity
@Table(name = "view_navigator_resource")
public class NavigatorResource extends AbstractResource {
    
    public String getResourceType() {
        return PortalConstants.NAVIGATOR_RESOURCE_TYPE;
    }
}

