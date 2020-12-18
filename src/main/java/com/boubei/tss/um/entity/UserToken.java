/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.boubei.tss.dm.record.ARecordTable;

/**
 * 用户令牌维护，用于其它系统访问TSS。
 * 
 * 开放报表/数据表资源给其它系统访问的步骤
 * 1、创建一个令牌，指定资源ID 和 用户账号
 * 2、把资源的权限（报表：查看，数据表：数据录入）开放给用户所在的角色
 * 3、如果需要做到匿名访问，则1、2步提及的账号和角色分别为匿名用户 和 匿名角色
 * 4、把token复制发送给用户，开始调用
 * 
 */
@Entity
@Table(name = "um_user_token")
public class UserToken extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_token_seq")
	@GenericGenerator(name = "user_token_seq", strategy = "native")
    private Long id; 
    
	@Column(nullable = false)
	private String user; 
    
	@Column(nullable = false)
    private String resource; // id|name|app
	
	@Column(nullable = false)
	private String type;     // report|record|sso|License
	
	@Column(nullable = false)
	private String token;    // eg：md5(id|name:loginName:sysTime)
	
	private Date expireTime;
	
	private String appId;  // 应用ID
	
	private String remark;
	private String description;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
 
	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Date getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}

	public Serializable getPK() {
		return this.getId();
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}
}
