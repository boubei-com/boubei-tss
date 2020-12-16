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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.display.grid.GridAttributesMap;
import com.boubei.tss.framework.web.display.grid.IGridNode;
import com.boubei.tss.util.DateUtil;

/** 
 * <p> 文件附件Attachment实体对象</p>
 */
@Entity
@Table(name = "cms_attachment")
@SequenceGenerator(name = "attach_sequence")
public class Attachment implements IEntity, IGridNode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "attach_sequence")
	private Long id;
    
	@Transient
    private Article article;    // 所属文章
    
	private Long articleId;
	
    private Integer seqNo = 0;	// 附件序号 PK
    
    @Column(nullable = false)
    private Integer type;		// 附件类型	1：图片 2：office文档
    
    @Column(nullable = false)
    private String name;		// 附件名称（上传前）
    
    @Column(nullable = false)
    private String fileName;	// 附件名称（上车后）  默认为时间戳
    private String fileExt;		// 附件后缀	eg: gif
    private Long   fileSize;    // 附件大小
    
    @Column(nullable = false)
    private String url;         // 值默认为 "/download?id="
    
    @Column(nullable = false)
    private String localPath;   // 上传后目录，eg: 2015/09/12
    
    private Date   uploadDate;	// 上传日期
    
    @Transient
    private String uploadName;  // 上传后生成的下载路径
    
    private Integer hitCount = 0; // 点击下载次数
 
	public String getLocalPath() {
		return localPath;
	}
 
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
 
	public String getUrl() {
		return url;
	}
 
	public void setUrl(String url) {
		this.url = url;
	}
 
    public String getFileExt() {
        return fileExt;
    }
 
    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }
 
    public String getFileName() {
        return fileName;
    }
 
    public void setFileName(String fileName) {
        this.fileName = fileName;
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
 
    public Date getUploadDate() {
        return uploadDate;
    }
 
    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public boolean isImage(){
        return RecordAttach.ATTACH_TYPE_PIC.equals(type);
    }
    
    public static boolean isImage(Integer type){
        return RecordAttach.ATTACH_TYPE_PIC.equals(type);
    }

    public static boolean isOfficeDoc(Integer type){
        return RecordAttach.ATTACH_TYPE_DOC.equals(type);
    }
    
    public GridAttributesMap getAttributes(GridAttributesMap map) {
    	map.put("articleId", getArticleId());
        map.put("seqNo", getSeqNo());
        map.put("localPath", localPath);
        map.put("type", type);
        map.put("name", name);
        map.put("fileSize", this.getFileSize());
        map.put("downloadUrl", this.getDownloadUrl());
        map.put("relationUrl", this.getRelationUrl());
        map.put("hitCount", this.getHitCount());
        map.put("uploadDate", DateUtil.formatCare2Second(uploadDate));
        
        map.put("_url", "<a href='/tss/" + this.getRelationUrl() + "' target='_blank'>打开</a>");
		map.put("topOpt", "<a href='javascript:void(0)' onclick='setTop(" + this.getPK() + ")'>置顶</a>");
		map.put("delOpt", "<a href='javascript:void(0)' onclick='delAttach(" + this.getPK() + ")'>删除</a>");
        
        return map;
    }
 
    /**
     * 绝对地址，返回格式类似：http://localhost:8088/tss/download?id=1216&seqNo=1 
     * @param baseUrl
     * @return 
     */
    public String getDownloadUrl(){
        String baseUrl = Context.getApplicationContext().getCurrentAppServer().getBaseURL();
        return baseUrl + this.getUrl() + this.getArticleId() + "&seqNo=" + getSeqNo();
    }
    
    /**
     * 相对地址，返回格式类似：download?id=1216&seqNo=1 
     * @param baseUrl
     * @return 
     */
    public String getRelationUrl(){
        String temp = this.getUrl().substring(1); //去掉 '/download?id=' 的 '/'
        return temp + this.getArticleId() + "&seqNo=" + this.getSeqNo();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public Long getArticleId() {
		return articleId;
	}

	public void setArticleId(Long articleId) {
		this.articleId = articleId;
	}
	
	public Serializable getPK() {
		return this.id;
	}

	public Integer getHitCount() {
		return hitCount;
	}

	public void setHitCount(Integer hitCount) {
		this.hitCount = hitCount;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
}