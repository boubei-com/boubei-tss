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

import javax.persistence.Embeddable;

@Embeddable
public class _GroupRoleId implements Serializable {
 
    private static final long serialVersionUID = 2080938669288193204L;
    
    private Long groupId;   
    private Long roleId;  
    
    public _GroupRoleId() {
        
    }
    
    public _GroupRoleId(Long groupId, Long roleId) {
        this.groupId = groupId;
        this.roleId = roleId;
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof _GroupRoleId))
            return false;
        
        _GroupRoleId temp = (_GroupRoleId) obj;
        return this.groupId.equals(temp.getGroupId())
                && this.roleId.equals(temp.getRoleId());
    }
    
    public Long getGroupId() {
        return groupId;
    }
 
    public Long getRoleId() {
        return roleId;
    }
    

    public int hashCode() {
        return (this.roleId + "_" + this.groupId).hashCode();
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
 
}
