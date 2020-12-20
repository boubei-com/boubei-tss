/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report.log;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.EasyUtils;

@Entity
@Table(name = "dm_access_log")
public class AccessLog implements IEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "access_log_sequence")
    @GenericGenerator(name = "access_log_sequence", strategy = "native")
    private Long id; 

    @Column(nullable = false)
    private String className; // 类名
    
    @Column(length = 100, nullable = false)
    private String methodName; // 方法名
    
    @Column(nullable = false)
    private String methodCnName; // 方法中文名
    
    @Column(length = 1000)
    private String params; // 参数
    
    private Date accessTime;  // 访问时间
    private Long runningTime; // 运行时长
    
    private Long userId;      // 登录用户
    private String ip;        // 访问者IP地址
    private String origin;    // 浏览器、微信、APP等
    
    public AccessLog() {
    }
    
    public AccessLog(Long start, String params) {
        this.setAccessTime( new Date(start) );
        this.setRunningTime( System.currentTimeMillis() - start );
        this.setParams(params);
        
        // 记录访问人，没有则记为匿名访问
        Long userId = (Long) EasyUtils.checkNull(Environment.getUserId(), UMConstants.ANONYMOUS_USER_ID);
		this.setUserId(userId);
		this.setIp( Environment.getClientIp() );
		
		this.setOrigin( Environment.getOrigin() );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodCnName() {
        return methodCnName;
    }

    public void setMethodCnName(String methodCnName) {
        this.methodCnName = methodCnName;
    }

    public Date getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(Date accessTime) {
        this.accessTime = accessTime;
    }

    public Long getRunningTime() {
        return runningTime;
    }

    public void setRunningTime(Long runningTime) {
        this.runningTime = runningTime;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public Serializable getPK() {
		return this.id;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

}
