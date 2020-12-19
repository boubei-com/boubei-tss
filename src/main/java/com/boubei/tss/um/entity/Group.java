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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.boubei.tss.util.EasyUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 用户组域对象
 */
@Entity
@Table(name = "um_group", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_NAME_GROUP", columnNames = { "parentId", "name" })
})
@SequenceGenerator(name = "group_sequence", sequenceName = "group_sequence", initialValue = 1000, allocationSize = 10)
@JsonIgnoreProperties(value={"pk", "attributes4XForm", "attributes", "parentClass", "decode", "seqNo", "levelNo", 
		"creatorId", "createTime", "creatorName", "updatorId", "updateTime", "updatorName", "lockVersion", 
		"resourceType", "fromGroupId", "syncConfig"})
public class Group extends OperateInfo implements IDecodable, IXForm, IResource {

	public static final Integer MAIN_GROUP_TYPE      = 1; // 主组类型
	public static final Integer ASSISTANT_GROUP_TYPE = 2; // 辅助组类型
	
	public static final String CUSTOMER_GROUP = "customer";
 
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "group_sequence")
	private Long    id;       // 用户组ID , 用户组主键
	private Long    parentId; // 父节点ID  
	
	@Column(length = 50, nullable = false)  
	private String  name;          // 组名:用户组名称
	private String  description;   // 描述:用户组信息描述
	
	@Column(nullable = false)  
	private Integer groupType;     // 用户组类型(1-主用户组类型,2-辅助组类型)
	
	// 树信息begin
	private String  decode;   // 层码
	private Integer levelNo;  // 层次值
	private Integer seqNo;    // 序号,用户组编号

	private Integer disabled = ParamConstants.FALSE; // 停用/启用标记

	// 和其他用户管理系统的同步时的对应信息
	private String  fromGroupId;   // 外部应用用户组id:要同步的系统中对应的节点的编号，针对db数据源、ldap数据源的同步中使用
	
	@Column(length = 2000)
	private String  syncConfig;    // 同步定义参数描述，xml格式
	
	/**
	 * 组域：用以隔离不同组织（企业）的数据。企业注册时生成，其所有子组/用户/数据都记录domain
	 */
	private String domain;
	private Integer domainRoot = ParamConstants.FALSE; // 是否为域的根节点, 0/1, -1为域分组
	
	public boolean isDomainGroup() {
		List<String> ignores = new ArrayList<>();
		ignores.add("Business");
		ignores.add(UMConstants.COMPANY_DOMAIN);
		return (levelNo == 4 && !ignores.contains(this.domain)) || ParamConstants.TRUE.equals(this.getDomainRoot());
	}
 
	public Long getId() {
		return id;
	}
 
	public void setId(Long id) {
		this.id = id;
	}

	public String getFromGroupId() {
		return fromGroupId;
	}
 
	public void setFromGroupId(String fromGroupId) {
		this.fromGroupId = fromGroupId;
	}
 
	public String getDescription() {
		return description;
	}
 
	public void setDescription(String description) {
		this.description = description;
	}
 
	public String getName() {
		return name;
	}
 
	public void setName(String groupName) {
		this.name = groupName;
	}
 
	public Integer getSeqNo() {
		return seqNo;
	}
 
	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
	}
 
	public Integer getDisabled() {
		return disabled;
	}
 
	public void setDisabled(Integer disabled) {
		this.disabled = disabled;
	}
 
	public Integer getGroupType() {
		return groupType;
	}
 
	public void setGroupType(Integer groupType) {
		this.groupType = groupType;
	}
 
	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
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

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
 
	public Class<?> getParentClass() {
		return getClass();
	}

	public String getResourceType() {
		return UMConstants.GROUP_RESOURCE_TYPE_ID;
	}
   
	public TreeAttributesMap getAttributes() {
		TreeAttributesMap map = new TreeAttributesMap(id, name);
		map.put("code", domain);
		map.put("parentId", parentId);
		map.put("disabled", disabled);
		map.put("syncable", EasyUtils.obj2String(this.syncConfig).indexOf("userSql") > 0);
		map.put("groupType", groupType);
		map.put("domain", domain);
		map.put("icon", UMConstants.GROUP_TREENODE_ICON + disabled + ".gif");
		map.put("_open", String.valueOf( (this.description+"").indexOf("open") >= 0) );
		
		return map;
	}
 
	public Map<String, Object> getAttributes4XForm() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        BeanUtil.addBeanProperties2Map(this, map);
        
		return map;
	}
 
    public String toString(){
        return "(ID:" + this.id + ", Name:" + this.name  + ", Decode:" + this.decode + ")"; 
    }
    
	public Serializable getPK() {
		return this.id;
	}

	public String getSyncConfig() {
		return syncConfig;
	}

	public void setSyncConfig(String syncConfig) {
		this.syncConfig = syncConfig;
	}

	public Integer getDomainRoot() {
		return domainRoot;
	}

	public void setDomainRoot(Integer domainRoot) {
		this.domainRoot = domainRoot;
	}
}
