/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "cloud_module_def")
@SequenceGenerator(name = "module_def_seq", sequenceName = "module_def_seq", initialValue = 1, allocationSize = 10)
@JsonIgnoreProperties(value = { "pk" })
public class ModuleDef extends ARecordTable {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "module_def_seq")
	private Long id;

	@Column(nullable = false)
	private String module; // 模块名称
	private String code;
	private String kind;
	private String status; // creating|opened|closed
	private Integer seqno = 0; // 排列次序
	
	private Long module_group; // 模块域专用分组

	@Column(nullable = false)
	private String roles;       // 收费角色：需购买策略账号进行转授分配
	private String roles_free;  // 免费角色：域管理员可自主授予，但会用于模块菜单过滤（比如E8和eff的客户使用不同的菜单）
	private String inner_base_role; // 企业内部默认角色
	private String reports;
	private String records; // 模块包含的数据表，为空则忽略
	private String menus;   // 模块包含的菜单、按钮

	@Column(length = 500)
	private String resource; // 资源目录，多个用逗号分隔

	private Double price;     // 单价（账号/月）
	private String price_def; // 优惠定义
	
	private Integer try_days = 31; // 试用天数, 如果=0，表示不支持试用
	private String account_limit;  // 1,10 代表1个起购，最多不超过10个 1 同1,1
	private String month_limit;

	/*
	 * 模块自定义初始化接口地址，在企业域用户选择此模块时自动调用，以完成模块初始化
	 */
	private String product_class; // 自定义实现类

	private Double cashback_ratio; // 返现比率 10% 请填写 10, 反给邀请者的
	private String experience_gold; // 体验金 第一个购买赠送； 支持格式 100 10%；eg：100即送10元，10%即按实付的百分之10反

	@Column(length = 800)
	private String description; // 模块描述

	@Column(length = 800)
	private String remark; // 模块备注
	
	public List<Long> resourcelist(String resourceType) {
		String resource = (String) BeanUtil.getPropertyValue(this, resourceType);
		List<Long> ids = new ArrayList<Long>();
		String[] array = EasyUtils.obj2String(resource).split(",");
		for (String t : array) {
			try {
				ids.add(EasyUtils.obj2Long(t)); // 非数字的忽略
			} catch (Exception e) {
			}
		}

		ids.remove(0L);
		return ids;
	}

	public Serializable getPK() {
		return this.getId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	public String getInner_base_role() {
		return inner_base_role;
	}

	public void setInner_base_role(String inner_base_role) {
		this.inner_base_role = inner_base_role;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getReports() {
		return reports;
	}

	public void setReports(String reports) {
		this.reports = reports;
	}

	public String getRecords() {
		return records;
	}

	public void setRecords(String records) {
		this.records = records;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getSeqno() {
		return seqno;
	}

	public void setSeqno(Integer seqno) {
		this.seqno = seqno;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public String getPrice_def() {
		return price_def;
	}

	public void setPrice_def(String price_def) {
		this.price_def = price_def;
	}

	public Integer getTry_days() {
		return try_days;
	}

	public void setTry_days(Integer try_days) {
		this.try_days = try_days;
	}

	public String getProduct_class() {
		return product_class;
	}

	public void setProduct_class(String product_class) {
		this.product_class = product_class;
	}

	public Double getCashback_ratio() {
		return cashback_ratio;
	}

	public void setCashback_ratio(Double cashback_ratio) {
		this.cashback_ratio = cashback_ratio;
	}

	public String getAccount_limit() {
		return account_limit;
	}

	public void setAccount_limit(String account_limit) {
		this.account_limit = account_limit;
	}

	public String getMonth_limit() {
		return month_limit;
	}

	public void setMonth_limit(String month_limit) {
		this.month_limit = month_limit;
	}

	public String getExperience_gold() {
		return experience_gold;
	}

	public void setExperience_gold(String experience_gold) {
		this.experience_gold = experience_gold;
	}

	public Double cal_experience_gold(Double payMoney) {
		if (EasyUtils.isNullOrEmpty(experience_gold) || payMoney == null || payMoney == 0)
			return null;

		if (experience_gold.endsWith("%"))
			return payMoney * EasyUtils.obj2Double(experience_gold.replace("%", "")) / 100;

		return EasyUtils.obj2Double(experience_gold);
	}

	public String getMenus() {
		return menus;
	}

	public void setMenus(String menus) {
		this.menus = menus;
	}

	public Long getModule_group() {
		return module_group;
	}

	public void setModule_group(Long module_group) {
		this.module_group = module_group;
	}

	public String getRoles_free() {
		return roles_free;
	}

	public void setRoles_free(String roles_free) {
		this.roles_free = roles_free;
	}
 
}
