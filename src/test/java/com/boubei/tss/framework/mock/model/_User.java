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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.persistence.entityaop.OperateInfo;

@Entity
@Table(name="test_user", uniqueConstraints = { @UniqueConstraint(columnNames = { "userName" }) })
@SequenceGenerator(name = "test_user_sequence", sequenceName = "test_user_sequence", initialValue = 1, allocationSize = 10)
public class _User extends OperateInfo implements IEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "test_user_sequence")
    private Long id;
    
    private String userName;
    private String password;
    private Integer age;
    private String addr;
    private String email;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private _Group group;
    
    public _Group getGroup() {
        return group;
    }

    public void setGroup(_Group group) {
        this.group = group;
    }

    public String toString() {
        return "【id=" + id + "，name=" + userName + "，email=" + email + "】";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    @Transient
    Integer birthYear;

    public Integer getBirthYear() {
        return new Integer(2008 - age);
    }

    public void setBirthYear(Integer age) {
        this.birthYear = new Integer(2008 - age);
    }
    
	public Serializable getPK() {
		return this.id;
	}
}
