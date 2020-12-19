/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report.permission;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.boubei.tss.um.permission.AbstractPermission;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 
 *报表资源权限表
 */
@Entity
@Table(name = "dm_permission_report")
@JsonIgnoreProperties(value={"pk"})
public class ReportPermission extends AbstractPermission {

}