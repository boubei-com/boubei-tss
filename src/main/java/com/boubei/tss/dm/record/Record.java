/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.permission.RecordResource;
import com.boubei.tss.framework.persistence.entityaop.IDecodable;
import com.boubei.tss.framework.persistence.entityaop.OperateInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;
import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.permission.IResource;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <pre> 
 * 数据表（又称数据表）定义类。
 * 说明：
 * 1、数据表权限项分为： 维护数据表、删除数据表、录入数据、浏览数据、维护数据
 * 2、拥有”维护数据表“权限的，可以编辑数据表的定义、移动至其它分组及授予角色
 * 3、拥有“删除数据表”权限的，可以删除数据表
 * 4、拥有“录入数据”权限的，可打开录入页，进行数据录入，并可以修改、删除自己录入的数据；只能看到自己录入的数据；可以复制自己的数据
 * 5、拥有“浏览数据”权限的，可以查看或查询其它人的录入数据，但不能修改和删除
 * 6、拥有“维护数据”权限的，可以查看或查询其它人的录入数据，并能修改和删除
 * 7、同时拥有“录入数据”和“浏览数据”权限的，可以复制他人创建的数据，但不能编辑和删除，只能编辑/删除自己创建数据
 * </pre>
 */
@Entity
@Table(name = "dm_record")
@SequenceGenerator(name = "record_sequence", sequenceName = "record_sequence", initialValue = 2000, allocationSize = 10)
@JsonIgnoreProperties(value={"pk", "attributes4XForm", "attributes", "parentClass", "createTime", "creatorName", 
		"updatorId", "updateTime", "updatorName", "lockVersion", "decode", "seqNo", "levelNo", "active", "resourceType"})
public class Record extends OperateInfo implements IXForm, IDecodable, IResource {
	
	public static final int TYPE0 = 0;  // 数据表分组
	public static final int TYPE1 = 1;  // 数据表
    
	public static final String DEFAULT_TABLE = "tbl_auto";
	public static final Long DEFAULT_PARENT_ID = 0L;
    
    // 资源类型： 数据表
    public static final String RESOURCE_TYPE = "D2"; 
    
    // 数据表资源操作ID
    public static final String OPERATION_EDIT    = "2"; // 维护录入表
    public static final String OPERATION_DELETE  = "3"; // 删除录入表
    public static final String OPERATION_CDATA   = "1"; // 录入数据, create data/delete data
    public static final String OPERATION_VDATA   = "4"; // 查看数据，授此操作权限的用户能看到所有录入数据, view data
    public static final String OPERATION_EDATA   = "5"; // 维护数据，授此操作权限的用户能看到所有录入数据，且能编辑 edit data
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "record_sequence")
    private Long    id;         // 主键
    
    @Column(length = 100, nullable = false)
    private String  name;       // 展示名称
    
    @Column(nullable = false)
    private Integer type;       // 0：数据表分组   1: 数据表
    
    private String  datasource; // 保存至哪个数据源
    
    @Column(name = "rctable")
    private String  table;  // 保存至哪个表。注：如果需要以table作为数据表接口标识，则不宜有表空间，比如ts1.j_order
    
    @Lob 
    private String define;  // 录入字段定义
    
    /** 定制的录入界面: 可以自己定制数据表单和展示表格 */
    private String customizePage;
    
    /** 定制的JS： 用于Form表单的校验，自动计算等  */
    @Column(length = 4000)  
    private String customizeJS;
    
    /** 定制的JS： 用于Grid的定制化需求等  */
    @Column(length = 2000)  
    private String customizeGrid;
    
    /** 
     * 定制的过滤条件，可按登录人的角色、组织等信息进行过滤 , 1=1 <#if org??> and org='${org}' </#if> 
     *  1=1<#if 1=0>showCUV< /#if>
     */
    @Column(length = 1000)  
    private String customizeTJ;
    
    @Column(length = 2000)  
    private String workflow;
    
    /* 更多高级配置 */
    private String icon;
    private Integer mobilable = ParamConstants.FALSE; // 移动端录入 1：支持 0：不支持
    private String  wxicon;
    private String  wxurl;  // 自定义小程序展示页面
    private Integer logicDel = ParamConstants.FALSE;
    private Integer showCreator  = ParamConstants.FALSE;  // 显示创建人、创建时间
    private Integer ignoreDomain = ParamConstants.FALSE; // 忽略【域】
    private Integer makePublic   = ParamConstants.FALSE; // 审核后公开
   
    @Column(length = 2000)
    private String  remark; 
    
    private Long    parentId;  // 父节点
    private Integer seqNo;    // 排序号
    private String  decode;  // 层码，要求唯一
    private Integer levelNo;// 层次值
    
    private Integer disabled = ParamConstants.FALSE; // 停用/启用标记，默认为启用
    private Integer needLog  = ParamConstants.TRUE;  // 记录修改日志，适用于重要性高的数据表
    private Integer needQLog = ParamConstants.FALSE; // 记录查询日志，适用于数据量大的数据表，统计查询性能和次数
    private Integer needFile = ParamConstants.TRUE;  // 是否需要附件上传
    private Integer batchImp = ParamConstants.TRUE;  // 是否允许批量导入
	
    public boolean isActive() {
    	return !ParamConstants.TRUE.equals(this.getDisabled());
    }

	public String toString() {
        return "数据表【id = " + this.id + ", name = " + this.name + "】";
    }
    
    public boolean equals(Object obj) {
    	Record object = (Record) obj;
        return this.id.equals(object.getId());
    }
    
    public String getDatasource() {
        return DMConstants.getDS(datasource);
    }
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public TreeAttributesMap getAttributes() {
        TreeAttributesMap map = new TreeAttributesMap(id, name);;
       
        map.put("code", this.table);
        map.put("parentId", parentId);
        map.put("type", type);
        String icon;
        if(TYPE1 == type) {
            map.put("customizePage", customizePage);
            map.put("define", define);
            
            if(DMConstants.RECORD_EMPTY_TABLE.equals(this.table)) {
            	icon = "page_" + getDisabled() + ".gif";
            } else {
            	icon = "record_" + getDisabled() + ".png";
            }
        } 
        else {
        	icon = "folder.gif";
        }
        map.put("icon", "images/" + icon );
        
        
        map.put("disabled", getDisabled());
        map.put("batchImp", this.batchImp);
        map.put("_open", String.valueOf( (this.remark+"").indexOf("open") >= 0) );
 
        return map;
    }

    public Map<String, Object> getAttributes4XForm() {
        Map<String, Object> map = new HashMap<String, Object>();
        BeanUtil.addBeanProperties2Map(this, map);
        return map;
    }

    public Class<?> getParentClass() {
        if(this.parentId.equals(DEFAULT_PARENT_ID)) {
            return RecordResource.class;
        }
        return this.getClass();
    }

	public String getResourceType() {
		return RESOURCE_TYPE;
	}
 
	public Serializable getPK() {
		return this.id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
 
	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Integer getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
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

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getDefine() {
		return define;
	}

	public void setDefine(String define) {
		this.define = define;
	}

	public String getCustomizePage() {
		return customizePage;
	}

	public void setCustomizePage(String customizePage) {
		this.customizePage = customizePage;
	}

	public String getCustomizeJS() {
		return customizeJS;
	}

	public void setCustomizeJS(String customizeJS) {
		this.customizeJS = customizeJS;
	}

	public String getCustomizeTJ() {
		return EasyUtils.obj2String(customizeTJ);
	}

	public void setCustomizeTJ(String customizeTJ) {
		this.customizeTJ = customizeTJ;
	}

	public Integer getNeedLog() {
		return needLog;
	}

	public void setNeedLog(Integer needLog) {
		this.needLog = needLog;
	}

	public String getCustomizeGrid() {
		return customizeGrid;
	}

	public void setCustomizeGrid(String customizeGrid) {
		this.customizeGrid = customizeGrid;
	}

	public Integer getBatchImp() {
		return batchImp;
	}

	public void setBatchImp(Integer batchImp) {
		this.batchImp = batchImp;
	}

	public Integer getDisabled() {
		return disabled == null ? ParamConstants.FALSE : disabled;
	}

	public void setDisabled(Integer disabled) {
		this.disabled = disabled;
	}
	
    public Integer getNeedFile() {
		return needFile;
	}

	public void setNeedFile(Integer needFile) {
		this.needFile = needFile;
	}
	
	public static List<String> SYS_TABLES = new ArrayList<String>();
	
	public boolean inSysTable() {
		return SYS_TABLES.contains( this.getTable() );
	}

	/**
	 * 优先取 dm_workflow_def 里的定义
	 */
	public String getWorkflow() {
		String sql = "select define from dm_workflow_def where domain=? and tableId = ?";
		Object _workflow = SQLExcutor.queryVL(sql, "define", Environment.getDomain(), this.id);
		return (String) EasyUtils.checkNull(_workflow, workflow);
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

	public String getIcon() {
		return (String) EasyUtils.checkNull(this.icon, DMUtil.getExtendAttr(this.remark, "icon"));
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getWxicon() {
		return (String) EasyUtils.checkNull(this.wxicon, DMUtil.getExtendAttr(this.remark, "wxicon"));
	}

	public void setWxicon(String wxicon) {
		this.wxicon = wxicon;
	}
	
	public String getWxurl() {
		return (String) EasyUtils.checkNull(this.wxurl, DMUtil.getExtendAttr(this.remark, "wxurl"));
	}

	public void setWxurl(String wxurl) {
		this.wxurl = wxurl;
	}

	public Integer getLogicDel() {
		return logicDel;
	}

	public void setLogicDel(Integer logicDel) {
		this.logicDel = logicDel;
	}

	public Integer getMobilable() {
		return mobilable;
	}

	public void setMobilable(Integer mobilable) {
		this.mobilable = mobilable;
	}

	public Integer getShowCreator() {
		return showCreator;
	}

	public void setShowCreator(Integer showCreator) {
		this.showCreator = showCreator;
	}

	public Integer getIgnoreDomain() {
		return ignoreDomain;
	}

	public void setIgnoreDomain(Integer ignoreDomain) {
		this.ignoreDomain = ignoreDomain;
	}

	public Integer getNeedQLog() {
		return needQLog;
	}

	public void setNeedQLog(Integer needQLog) {
		this.needQLog = needQLog;
	}

	public Integer getMakePublic() {
		return makePublic;
	}

	public void setMakePublic(Integer makePublic) {
		this.makePublic = makePublic;
	}
}
