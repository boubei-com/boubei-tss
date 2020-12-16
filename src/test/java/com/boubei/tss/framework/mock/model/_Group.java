/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.boubei.tss.framework.persistence.entityaop.IDecodable;
import com.boubei.tss.framework.web.display.tree.ILevelTreeNode;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;

@Entity
@Table(name = "test_group", uniqueConstraints = { 
        @UniqueConstraint(columnNames = { "code" })
    })
@SequenceGenerator(name = "group_sequence", sequenceName = "group_sequence", initialValue = 1000, allocationSize = 10)
public class _Group implements IDecodable, ILevelTreeNode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "group_sequence")
    private Long id;
    private String code;
    private String name;
    private String remark;
    
    private Long    parentId = 0L;// 父节点
    private Integer levelNo; // 层次值
    private Integer seqNo;   // 排序号
    private String  decode;  // 层码
    
    private Integer deleted;
    private Long recycleId;
   
    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Long getRecycleId() {
        return recycleId;
    }

    public void setRecycleId(Long recycleId) {
        this.recycleId = recycleId;
    }

    public String toString() {
        return "【id=" + id + "，code=" + code + "，name=" + name 
                + "，parentId=" + parentId + "，levelNo=" + levelNo 
                + "，seqNo=" + seqNo + "，decode=" + decode+ "】";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getLevelNo() {
        return levelNo;
    }

    public void setLevelNo(Integer levelNo) {
        this.levelNo = levelNo;
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

    public Class<?> getParentClass() {
        return _Group.class;
    }

    public TreeAttributesMap getAttributes() {
        return new TreeAttributesMap(id, name);
    }
    
	public Serializable getPK() {
		return this.id;
	}
}
