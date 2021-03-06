/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.entity.permission;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 用户角色关系表(包括继承的组的角色)
 * ALTER TABLE `xxx`.`um_roleusermapping`  DROP PRIMARY KEY;
 */
@Entity
@Table(name = "um_roleusermapping")
public class RoleUserMapping implements Serializable{
	
	private static final long serialVersionUID = -2132184041472900185L;

	@EmbeddedId  
	private RoleUserMappingId id;
 
	public RoleUserMappingId getId() {
		return id;
	}
 
	public void setId(RoleUserMappingId id) {
		this.id = id;
	}
	
	public String toString() {
	    return id.toString();
	}
}

	