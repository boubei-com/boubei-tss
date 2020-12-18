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

import com.boubei.tss.um.permission.AbstractPermission;

/** 
 * 导航栏资源权限表
 */
@Entity
@Table(name = "portal_permission_navigator")
public class NavigatorPermission extends AbstractPermission {
    
}

