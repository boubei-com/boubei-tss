/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.permission;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.boubei.tss.um.permission.AbstractPermission;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 
 * 数据录入表权限
 */
@Entity
@Table(name = "dm_permission_record")
@JsonIgnoreProperties(value={"pk"})
public class RecordPermission extends AbstractPermission {

}