/* ==================================================================   
 * Created [2019-09-19] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import com.boubei.tss.cache.aop.CacheClear;
import com.boubei.tss.util.EasyUtils;

/**
[
  {'label':'数据表','code':'tbl','options':{'codes':'wms_sku|wms_order|wms_asn','names':'货品表|出库单|入库单'}},
  {'label':'字段编码','code':'code','options':{'codes':'udf1|udf2|udf4|udf4|lotatt01|invstatus','names':'udf1|udf2|udf4|udf4|批号|货品状态'}},
  {'label':'字段名称','code':'label'},
  {'label':'字段类型','code':'type','options':{'codes':'hidden|','names':'隐藏|显示'}},
  {'label':'是否可空','code':'nullable','options':{'codes':'true|false','names':'是|否'}},
  {'label':'是否唯一','code':'unique_','options':{'codes':'true|false','names':'是|否'}},
  {'label':'下拉选项','code':'options'},
  {'label':'默认值','code':'defaultValue'},
  {'label':'校验正则值','code':'checkReg'},
  {'label':'列宽度','code':'cwidth'},
  {'label':'列对齐','code':'align'},
  {'label':'谁能查看','code':'role1','jsonUrl':'/tss/wx/api/roles'},
  {'label':'谁能编辑','code':'role2','jsonUrl':'/tss/wx/api/roles'},
  {'label':'自定义1','code':'udf1'},
  {'label':'自定义2','code':'udf2'},
  {'label':'自定义3','code':'udf3'},
  {'label':'用户','code':'user'},
  {'label':'用户组织','code':'user_org'}
]
 */
@Entity
@Table(name = "dm_record_field")
@CacheClear
public class RecordField extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "record_field_sequence")
	@GenericGenerator(name = "record_field_sequence", strategy = "native")
	private Long id;
	
	@Column(name = "tbl")
	private String table;  // 自定义表字段所属表，为空表示全域所有表
	
	private String user;   // 用户级自定义，为空则是域级自定义
	private Long user_org; // 用户所属组织（经营组织、运营组织）级自定义
	
	private String code;
	
	private String label;
	
	private String type; // string/int/number/hidden...
	
	private String nullable;
	
	@Column(name = "unique_")
	private String unique;
	
	private String readonly;
	
	@Column(length = 4000)
	private String options; 	 // 下拉选项
	private String defaultValue; // 默认值
	private String checkReg;	 // 正则校验
	
	private String cwidth;
	private String align;
	
	private String role1; // 谁能查看
	private String role2; // 谁能编辑
	@Transient
	public  String role;  // 查看、编辑、null
	
	private String udf1;
	private String udf2;
	private String udf3;
	
	private Integer seqNo; // 字段排序，用于Grid及form布局
	
	public boolean containsRole1(List<Long> roles) {
		return EasyUtils.contains(roles, this.role1);
	}
	public boolean containsRole2(List<Long> roles) {
		return EasyUtils.contains(roles, this.role2);
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

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getNullable() {
		return nullable;
	}

	public void setNullable(String nullable) {
		this.nullable = nullable;
	}

	public String getRole1() {
		return role1;
	}

	public void setRole1(String role1) {
		this.role1 = role1;
	}

	public String getRole2() {
		return role2;
	}

	public void setRole2(String role2) {
		this.role2 = role2;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUdf1() {
		return udf1;
	}

	public void setUdf1(String udf1) {
		this.udf1 = udf1;
	}

	public String getUdf2() {
		return udf2;
	}

	public void setUdf2(String udf2) {
		this.udf2 = udf2;
	}

	public String getUdf3() {
		return udf3;
	}

	public void setUdf3(String udf3) {
		this.udf3 = udf3;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public String getCwidth() {
		return cwidth;
	}

	public void setCwidth(String cwidth) {
		this.cwidth = cwidth;
	}

	public String getAlign() {
		return align;
	}

	public void setAlign(String align) {
		this.align = align;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getCheckReg() {
		return checkReg;
	}

	public void setCheckReg(String checkReg) {
		this.checkReg = checkReg;
	}

	public String getUnique() {
		return unique;
	}

	public void setUnique(String unique) {
		this.unique = unique;
	}

	public String getReadonly() {
		return readonly;
	}

	public void setReadonly(String readonly) {
		this.readonly = readonly;
	}

	public Long getUser_org() {
		return user_org;
	}

	public void setUser_org(Long user_org) {
		this.user_org = user_org;
	}
	public Integer getSeqNo() {
		return seqNo;
	}
	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
	}
}
