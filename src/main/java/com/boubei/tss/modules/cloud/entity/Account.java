package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;

/**
 * 域账户
 */
@Entity
@Table(name = "cloud_account")
@SequenceGenerator(name = "account_seq", sequenceName = "account_seq", initialValue = 1, allocationSize = 10)
public class Account implements IEntity {

	public static final String STATUS0 = "激活";
	public static final String STATUS1 = "冻结";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "account_seq")
	private Long id;

	@ManyToOne
	private User belong_user;

	private Double balance;        // 余额
	private Double balance_freeze; // 冻结余额
	private Double balance_safe;   // 安全额度
	private Integer points;  	   // 积分总额 100:1 元
	private Integer points_freeze; // 积分冻结余额

	private String status = STATUS0; // 激活|冻结
	private String remark;           // 备注
	private Date createDate = new Date();
	
	private String domain;

	public void deduct(Double money) {
		Double m = MathUtil.addDoubles(getBalance_freeze(), balance);

		m = MathUtil.addDoubles(m, -money);

		if (balance_freeze == null || m > balance_freeze) {
			balance = MathUtil.addDoubles(balance, -money);
		} else {
			balance = 0D;
			balance_freeze = m;
		}
	}

	// 加freeze_money
	public void addBalanceFreeze(Double money) {
		balance_freeze = MathUtil.addDoubles(getBalance_freeze(), money);
	}

	// 默认加money
	public void add(Double money) {
		balance = MathUtil.addDoubles(balance, money);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getBalance() {
		return balance;
	}

	public void setBalance(Double balance) {
		this.balance = balance;
	}

	public Double getBalance_freeze() {
		return (Double) EasyUtils.checkNull(balance_freeze, 0D);
	}

	public void setBalance_freeze(Double balance_freeze) {
		this.balance_freeze = balance_freeze;
	}

	public Double getBalance_safe() {
	    return (Double) EasyUtils.checkNull(balance_safe, 0D);
	}

	public void setBalance_safe(Double balance_safe) {
		this.balance_safe = balance_safe;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Serializable getPK() {
		return this.getId();
	}

	public User getBelong_user() {
		return belong_user;
	}

	public void setBelong_user(User belong_user) {
		this.belong_user = belong_user;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public Integer getPoints_freeze() {
		return points_freeze;
	}

	public void setPoints_freeze(Integer points_freeze) {
		this.points_freeze = points_freeze;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}
