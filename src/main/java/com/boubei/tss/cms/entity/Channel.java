/* ==================================================================   
 * Created [2006-12-28] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.entity.permission.ChannelResource;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.persistence.entityaop.IDecodable;
import com.boubei.tss.framework.persistence.entityaop.OperateInfo;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;
import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.permission.IResource;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * 站点、栏目类
 */
@Entity
@Table(name = "cms_channel", uniqueConstraints = { 
        @UniqueConstraint(name="MULTI_NAME_CHANNEL", columnNames = { "PARENTID", "name" })
    })
@SequenceGenerator(name = "channel_sequence", sequenceName = "channel_sequence", initialValue = 1, allocationSize = 1)
public class Channel extends OperateInfo implements IXForm, IDecodable, IResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "channel_sequence")
	private Long    id;
    private String  name;    // 名称(站点或栏目名称)
    
    @ManyToOne(fetch = FetchType.EAGER)
    private Channel site;  // 栏目所在站点
    
	private String  path;        // 发布路径
	private String  imagePath;   // 图片上传根路径
	private String  docPath;     // 附件上传根路径
	
    private String  overdueDate; // 过期时间
    private String  remark;      // 站点描述
    
    // 状态值信息
    private Integer disabled = ParamConstants.FALSE; // 状态 0:启动 1：停用
    
    // 树结构信息
    private Long    parentId; // 父节点编号 缺省为0，即无父节点
    
    @Column(nullable = false)
    private Integer seqNo;    // 站点或栏目显示顺序
    private String  decode;   // 层码
    private Integer levelNo;  // 层次值
    
    private String domain;

	public Long getId() {
		return id;
	}
 
	public void setId(Long id) {
		this.id = id;
	}
	
	public void setDecode(String decode) {
		this.decode = decode;
	}
 
	public void setLevelNo(Integer levelNo) {
		this.levelNo = levelNo;
	}
 
	public String getDocPath() {
		if(EasyUtils.isNullOrEmpty(docPath)) {
			return CMSConstants.DEFAULT_DOC_PATH;
		}
		return docPath;
	}
 
	public void setDocPath(String docPath) {
		this.docPath = docPath;
	}
 
	public String getImagePath() {
		if(EasyUtils.isNullOrEmpty(imagePath)) {
			return CMSConstants.DEFAULT_IMG_PATH;
		}
		return imagePath;
	}
 
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
 
	public String getName() {
		return name;
	}
 
	public void setName(String name) {
		this.name = name;
	}
 
	public String getOverdueDate() {
		return overdueDate;
	}
 
	public void setOverdueDate(String overdueDate) {
		this.overdueDate = overdueDate;
	}
 
	public Long getParentId() {
		return parentId;
	}
 
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
 
	public String getPath() {
		if( "temp".equals(path) ) {
			return DMUtil.getAttachPath() + "/cms";
		}
		return path;
	}
 
	public void setPath(String path) {
		this.path = path;
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
 
	public void setDisabled(Integer status) {
		this.disabled = status;
	}
 
	public boolean isSiteRoot() {
		return this.id.equals(site.getId());
	}
    
	public Map<String, Object> getAttributes4XForm() {
		Map<String, Object> attributes = new HashMap<String, Object>();
		BeanUtil.addBeanProperties2Map(this, attributes, "site");
		
		if(site != null) {
			attributes.put("site.id", site.getId());
		}
		
		return attributes;
	}

	public TreeAttributesMap getAttributes() {
		TreeAttributesMap map = new TreeAttributesMap(id, name);
		map.put("disabled", this.disabled);
		map.put("siteId", this.site.getId());
		map.put("isSite", isSiteRoot() ? 1 : 0);
		map.put("resourceTypeId", getResourceType());
 
		String iconName = (isSiteRoot() ? "site" : "channel");
		map.put("icon", "images/" + iconName + "_" + disabled + ".gif");
        map.put("_open", String.valueOf( (this.remark+"").indexOf("open") >= 0 || this.levelNo <= 2) );
        
		return map;
	}

	public Integer getLevelNo() {
		return levelNo;
	}
 
	public String getDecode() {
		return decode;
	}
    
    public String getAttanchmentPath(Attachment attanchment){
        if (attanchment.isImage()) {
            return this.getImagePath();
        }
        return this.getDocPath();
    }
    
    public String toString(){
        return "(id:" + this.id + ", name:" + this.name + ", parentId:" + this.parentId + ")" + this.decode; 
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getResourceType() {
        return CMSConstants.RESOURCE_TYPE_CHANNEL;
    }
    
	public Class<?> getParentClass() {
        if(this.parentId.equals(CMSConstants.HEAD_NODE_ID)) {
            return ChannelResource.class;
        }
        return getClass();
	}
 
	public Channel getSite() {
		return site;
	}

	public void setSite(Channel site) {
		this.site = site;
	}
	
	public Serializable getPK() {
		return this.id;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}