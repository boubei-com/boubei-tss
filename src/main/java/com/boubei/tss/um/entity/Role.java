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
import javax.persistence.UniqueConstraint;

import com.boubei.tss.framework.persistence.entityaop.IDecodable;
import com.boubei.tss.framework.persistence.entityaop.OperateInfo;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;
import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.permission.IResource;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 角色域对象
 */
@Entity
@Table(name = "um_role", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_NAME_ROLE", columnNames = { "parentId", "name", "isGroup" })
})
@SequenceGenerator(name = "role_sequence", sequenceName = "role_sequence", initialValue = 1000, allocationSize = 10)
@JsonIgnoreProperties(value={"pk", "attributes4XForm", "attributes", "parentClass", "createTime", "creatorName",
		"updatorId", "updateTime", "updatorName", "lockVersion", "decode", "seqNo", "levelNo", "active", "resourceType"})
public class Role extends OperateInfo implements IDecodable, IXForm, IResource {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "role_sequence")
	private Long    id;       // 角色主键ID
	private Long    parentId; // 父节点ID 
	
	@Column(length = 20, nullable = false)  
	private String  name;        // 名称:角色名称
	private String  description; // 描述:对角色的描述
	private Integer isGroup = ParamConstants.FALSE;  // 是否角色组 (1-角色组，0-角色)
	private Date    startDate; // 开始时间 
	private Date    endDate;   // 结束时间 
    
    private Integer seqNo;   // 角色序号 
    private String  decode;  // 层码
    private Integer levelNo; // 层次值
    
    private Integer disabled = ParamConstants.FALSE; // 角色状态
 
	public Long getId() {
		return id;
	}
 
	public void setId(Long id) {
		this.id = id;
	}
 
	public String getDescription() {
		return EasyUtils.obj2String(description);
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
 
	public Integer getIsGroup() {
		return isGroup;
	}
 
	public void setIsGroup(Integer isGroup) {
		this.isGroup = isGroup;
	}
 
	public Long getParentId() {
		return parentId;
	}
 
	public void setParentId(Long parentRoleId) {
		this.parentId = parentRoleId;
	}
 
	public String getName() {
		return name;
	}
 
	public void setName(String roleName) {
		this.name = roleName;
	}
 
	public Integer getSeqNo() {
		return seqNo;
	}
 
	public void setSeqNo(Integer roleOrder) {
		this.seqNo = roleOrder;
	}
 
	public Integer getDisabled() {
		return disabled;
	}
 
	public void setDisabled(Integer roleState) {
		this.disabled = roleState;
	}
 
	public Date getStartDate() {
		return startDate;
	}
 
	public void setStartDate(Date startDate) {
		this.startDate = DateUtil.noHMS(startDate);
	}
 
	public TreeAttributesMap getAttributes() {
		TreeAttributesMap map = new TreeAttributesMap(id, name);

		map.put("roleGroupId", parentId);
		map.put("isGroup", isGroup);
		map.put("disabled", disabled);
		map.put("resourceTypeId", getResourceType());
		map.put("description", this.description);

		if (ParamConstants.FALSE.equals(isGroup)) { // 角色
			map.put("icon", UMConstants.ROLE_TREENODE_ICON + disabled + ".gif");
		} 
		else { // 角色组
			map.put("icon", UMConstants.GROUP_ROLE_TREENODE_ICON + disabled + ".gif");
		}
		map.put("_open", String.valueOf( (this.description+"").indexOf("open") >= 0 || EasyUtils.obj2Int(this.levelNo) <= 2) );
		 
		return map;
	}
 
	public Map<String, Object> getAttributes4XForm() {
		Map<String, Object> map = new HashMap<String, Object>();
		BeanUtil.addBeanProperties2Map(this, map);
 
		map.put("startDate", DateUtil.format(startDate));
		map.put("endDate",   DateUtil.format(endDate));
		return map;
	}
 
	public String getDecode() {
		return decode;
	}
 
	public void setDecode(String decode) {
		this.decode = decode;
	}
 
	public Integer getLevelNo() {
		return levelNo;
	}
 
	public void setLevelNo(Integer levelNo) {
		this.levelNo = levelNo;
	}

	public Class<?> getParentClass() {
		return getClass();
	}
    
	public String getResourceType() {
		return UMConstants.ROLE_RESOURCE_TYPE_ID;
	}
    
    public String toString(){
        return "(id:" + this.id + ", name:" + this.name + ")"; 
    }
    
	public Serializable getPK() {
		return this.id;
	}
}
