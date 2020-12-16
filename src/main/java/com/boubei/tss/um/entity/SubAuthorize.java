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
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.persistence.entityaop.OperateInfo;
import com.boubei.tss.framework.web.display.tree.ITreeNode;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;
import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * 权限转授策略(Sub Authorize Strategy)域对象。 策略可以授予用户、用户组、也可以授予角色，或者三者兼有。
 */
@Entity
@Table(name = "um_sub_authorize")
@SequenceGenerator(name = "SubAuthorize_sequence", sequenceName = "SubAuthorize_sequence", initialValue = 1000, allocationSize = 10)
public class SubAuthorize extends OperateInfo implements IEntity, ITreeNode, IXForm {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "SubAuthorize_sequence")
	private Long id; // 策略ID：策略主键ID

	private Long buyerId;  // 策略购买人
	private Long ownerId;  // 策略所有人
	private Long ownerOrg; // 策略所属组织（经营组织：分公司、仓库站点等）···

	@Column(nullable = false)
	private String name; // 名称:策略名称，如果是购买模块所获策略，则name=模块ID_模块名称_购买人ID_购买序号
	
	private Long moduleId;
	
	private Date startDate; // 开始时间
	private Date endDate; // 结束时间

	@Column(length = 1000)
	private String description;// 描述:对策略的描述

	private Integer disabled = ParamConstants.FALSE;// 策略状态 1-停用, 0-启用

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getModuleId() {
		return moduleId;
	}

	public void setModuleId(Long moduleId) {
		this.moduleId = moduleId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getDisabled() {
		return disabled;
	}

	public void setDisabled(Integer state) {
		this.disabled = state;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Map<String, Object> getAttributes4XForm() {
		Map<String, Object> map = new HashMap<String, Object>();
		BeanUtil.addBeanProperties2Map(this, map);

		map.put("startDate", DateUtil.format(startDate));
		map.put("endDate", DateUtil.format(endDate));
		return map;
	}

	public TreeAttributesMap getAttributes() {
		TreeAttributesMap map = new TreeAttributesMap(id, name);
		map.put("disabled", disabled);
		map.put("icon", UMConstants.STRATEGY_TREENODE_ICON + disabled + ".gif");

		return map;
	}

	public Serializable getPK() {
		return this.id;
	}

	public Long getOwnerId() {
		return (Long) EasyUtils.checkNull(ownerId, creatorId);
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getBuyerId() {
		return buyerId;
	}

	public void setBuyerId(Long buyerId) {
		this.buyerId = buyerId;
	}

	public Long _buyer() {
		return (Long) EasyUtils.checkNull(buyerId, creatorId);
	}

	public Long getOwnerOrg() {
		return ownerOrg;
	}

	public void setOwnerOrg(Long ownerOrg) {
		this.ownerOrg = ownerOrg;
	}
}
