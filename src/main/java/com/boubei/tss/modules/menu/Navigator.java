/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.menu;

import java.io.Serializable;
import java.util.HashMap;
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

import org.dom4j.Element;

import com.boubei.tss.framework.persistence.entityaop.IDecodable;
import com.boubei.tss.framework.persistence.entityaop.OperateInfo;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;
import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.permission.IResource;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.XMLDocUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 导航栏内容定义器：描述导航栏（菜单）基本信息及内容定义信息
 */
@Entity
@Table(name = "portal_navigator", uniqueConstraints = { 
        @UniqueConstraint(name="MULTI_NAME_MENU", columnNames = { "parentId", "name" })
})
@SequenceGenerator(name = "navigator_sequence", sequenceName = "navigator_sequence", initialValue = 1)
@JsonIgnoreProperties(value={"pk", "attributes4XForm", "attributes", "parentClass", "createTime", "creatorName", 
		"updatorId", "updateTime", "updatorName", "lockVersion", "resourceType"})
public class Navigator extends OperateInfo implements IXForm, IDecodable, IResource {

    /**
     * 菜单组
     */
    public static final Integer TYPE_MENU        = 1;
    
    /**
     * 普通按钮
     */
    public static final Integer TYPE_MENU_ITEM_2 = 2;
    
    /**
     * 普通链接: url = www.google.com
     */
    public static final Integer TYPE_MENU_ITEM_4 = 4;
    
    
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "navigator_sequence")
    private Long   id;
	
	@Column(nullable = false)
	private String  name;       // 菜单（项）名称
	private String  code;       // 菜单（项）code
	private Integer type;       // 类型  参考 TYPE_MENU_X
	private Long   parentId;    // 菜单项对应菜单
    private String url;         // url地址                     
    private String target;      // 目标区域，_blank/_self 等  
    private String icon;
    
    @Column(length = 1000)
    private String description; // 菜单内容的描述信息
	
    private String params;        // 参数    
    
    // 菜单树结构信息
    @Column(nullable = false)
	private Integer seqNo;   // 顺序号
    private String  decode;  // 层码
    private Integer levelNo; // 层次值

    private Integer disabled = ParamConstants.FALSE;  // 是否停用
 
    public TreeAttributesMap getAttributes() {
        TreeAttributesMap map = new TreeAttributesMap(id, name);
        map.put("parentId", parentId);
        map.put("type", type);
        map.put("disabled", disabled);
        map.put("resourceTypeId", getResourceType());
        
        if(this.type.equals(TYPE_MENU)){
            map.put("icon", "images/menu_" + disabled + ".gif");
        } else {
            map.put("icon", "images/menu_item_" + disabled + ".gif");
        }       
        map.put("_open", String.valueOf( (this.description+"").indexOf("open") >= 0 || this.levelNo <= 3) );

        return map;
    }
    
    /**
     * |- 菜单 <br>
     * ......|- 菜单项 <br>
     * ..............|- 菜单项 <br>
     * ..............|- 菜单项 <br>
     * ..............|- 菜单项 <br>
     * ......|- 菜单项 <br>
     * ..............|- 菜单项 <br>
     * ..............|- 菜单项 <br>
     * 
     * @param list
     */    
    public Element compose2Tree(List<Navigator> list){
        Map<Long, Element> map = new HashMap<Long, Element>();
        for ( Navigator entity : list ) {
            map.put(entity.getId(), entity.genMenuNode());
        }
        
        Element root = null;
        for ( Navigator entity : list ) {
        	if(ParamConstants.TRUE.equals(entity.getDisabled())) {
        		continue; // 过滤掉停用的
        	}
        	
            Element node = map.get(entity.getId());
            Element parent = map.get(entity.getParentId());        
            if(parent != null){
                parent.add(node);
            } 
            else {
            	root = node;
            }
        }
        return root;
    }
    
    private Element genMenuNode(){
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("name", name);
        map.put("code", code);
        map.put("type", type);
        
        map.put("url", url);
        map.put("target", target);
        map.put("params", params);
        
        return XMLDocUtil.map2AttributeNode(map, this.type.equals(TYPE_MENU) ? "Menu" : "MenuItem");
    }

    public Map<String, Object> getAttributes4XForm() {
        Map<String, Object> map = new HashMap<String, Object>();
        BeanUtil.addBeanProperties2Map(this, map, "children,content,toContent".split(","));
       
        return map;
    }
    
    public Class<?> getParentClass() {
        if(this.parentId.equals(PortalConstants.ROOT_ID)) {
            return NavigatorResource.class;
        }
        return getClass();
    }

    public String getResourceType() {
        return PortalConstants.NAVIGATOR_RESOURCE_TYPE;
    }
    
    public String toString(){
        return "(id:" + this.id + ", name:" + this.name + ")"; 
    }
 
	public String getDescription() {
		return description;
	}
 
	public Long getParentId() {
		return parentId;
	}
 
	public Long getId() {
		return id;
	}
 
	public String getName() {
		return name;
	}
 
	public Integer getSeqNo() {
		return seqNo;
	}
 
    public String getParams() {
        return params;
    }
 
    public String getTarget() {
        return target;
    }
 
    public Integer getType() {
        return type;
    }   
 
    public String getUrl() {
        return url;
    }
 
    public void setDescription(String description) {
        this.description = description;
    }
 
    public void setId(Long id) {
        this.id = id;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public void setParams(String params) {
        this.params = params;
    }
 
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
 
    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }
 
    public void setTarget(String target) {
        this.target = target;
    }
 
    public void setType(Integer type) {
        this.type = type;
    }
 
    public void setUrl(String url) {
        this.url = url;
    }
 
    public Integer getDisabled() {
        return disabled;
    }
 
    public void setDisabled(Integer disabled) {
        this.disabled = disabled;
    }
 
    public String getDecode() {
        return decode;
    }
 
    public Integer getLevelNo() {
        return levelNo;
    }
 
    public void setDecode(String decode) {
        this.decode = decode;
    }
 
    public void setLevelNo(Integer levelNo) {
        this.levelNo = levelNo;
    }
 
	public Serializable getPK() {
		return this.id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}	