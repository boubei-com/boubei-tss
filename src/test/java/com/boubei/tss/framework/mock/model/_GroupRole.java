/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock.model;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;

@Entity
@Table(name = "test_group_role")
public class _GroupRole implements IEntity {

    @EmbeddedId  
    private _GroupRoleId id;

    public _GroupRoleId getId() {
        return id;
    }

    public void setId(_GroupRoleId id) {
        this.id = id;
    }

	public Serializable getPK() {
		return this.id;
	}
}
