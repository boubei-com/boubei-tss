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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.framework.persistence.entityaop.OperateInfo;
import com.boubei.tss.framework.web.display.grid.GridAttributesMap;
import com.boubei.tss.framework.web.display.grid.IGridNode;
import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

/** 
 * 文章Article实体对象
 * 
 * 栏目文章如何分域：
 * 1、栏目站点可以设置domain值，设置后站点下栏目都会设置上domain值
 * 2、只对对应domain域的用户可见
 * 3、域用户新增、修改文章，会设置文章domain = Enviroment.domain，只对相同域的用户可见
 * 4、如需支持发布到门户匿名访问，除了将栏目和门户授权匿名用户访问，还需将域添加到 系统参数 PUBLIC_DOMAINS （默认值为：'无域','BD'）
 */
@Entity
@Table(name = "cms_article")
@SequenceGenerator(name = "article_sequence", sequenceName = "article_sequence", initialValue = 1, allocationSize = 10)
public class Article extends OperateInfo implements IGridNode, IXForm {
    
	public static final String[] IGNORE_PROPERTIES = new String[] { "id", "status", "hitCount", "issueDate" };
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "article_sequence")
    private Long   id;			
	
	@Column(nullable = false)
    private String title;		// 标题
    private String subtitle;	// 副标题
    private String keyword;		// 关键字
    private String author;		// 作者
    private String summary;		// 摘要
    private String htmlRef;		// 引用的网页地址，全文检索时直接连接到此地址
    
    @Column(length = 4000)
    private String  content = ""; //正文内容  
    
    @Lob    
    private String  comment;	  // 评论  
    private int commentNum = 0;   // 评论次数
    
    private Date    overdueDate;  // 过期时间
    private Date    issueDate;	  // 发布日期	开始为空,只有审核和发布阶段可以改
    private String  pubUrl;       // 发布路径
    
    private Integer seqNo = 0;
    private Integer hitCount = 0; // 点击率
	
    private Integer status = CMSConstants.START_STATUS;	   // 文章的状态 1：编辑中 2：待发布 3：已发布生成xml 4：过期 
    
    @ManyToOne(fetch = FetchType.EAGER)
    private Channel channel;      // 文章所属栏目
    
    private Integer isTop = ParamConstants.FALSE;   // 文章是否置顶
    
    private String domain;
    
    @Transient 
    List<Attachment> attachments = new ArrayList<Attachment>();  // 存放文章附件列表
    
    public String toString(){
        return "(id:" + this.id + ", title:" + this.title + ")"; 
    }
 
    public String getAuthor() {
        return author;
    }
 
    public void setAuthor(String author) {
        this.author = author;
    }
 
    public String getContent() {
        return content;
    }
 
    public void setContent(String content) {
        this.content = EasyUtils.obj2String(content);
    }
 
    public Integer getHitCount() {
        return hitCount;
    }
 
    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }
 
    public Long getId() {
        return id;
    }
 
    public void setId(Long id) {
        this.id = id;
    }
 
    public Date getIssueDate() {
        return issueDate;
    }
 
    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }
 
    public String getKeyword() {
        return keyword;
    }
 
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }
 
    public void setStatus(Integer status) {
        this.status = status;
    }
 
    public String getSubtitle() {
        return subtitle;
    }
 
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
 
    public String getSummary() {
        return summary;
    }
 
    public void setSummary(String summary) {
        this.summary = summary;
    }
 
    public String getTitle() {
        return title;
    }
 
    public void setTitle(String title) {
        this.title = title;
    }
 
	public String getPubUrl() {
		return pubUrl;
	}
 
	public void setPubUrl(String pubUrl) {
		this.pubUrl = pubUrl;
	}
 
	public Date getOverdueDate() {
		return overdueDate;
	}
 
	public void setOverdueDate(Date overdueDate) {
		this.overdueDate = overdueDate;
	}
    
    public GridAttributesMap getAttributes(GridAttributesMap map) {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        BeanUtil.addBeanProperties2Map(this, attributes, "channel");
        map.putAll(attributes);
        map.put("channel.id", channel.getId());
        map.put("channel.name", channel.getName());
        
        boolean overdue = overdueDate != null && overdueDate.before(new Date());
        map.put("icon", "images/article_" + EasyUtils.checkTrue(overdue, 1, 0) + ".gif");
        
        map.put("commentNum", "<a href='javascript:void(0)' onclick='showComments(" + id + ")'>" + commentNum + "</a>");
        
        return map;
    }

    public Map<String, Object> getAttributes4XForm() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        BeanUtil.addBeanProperties2Map(this, map, "channel");
        map.put("id", id);
        map.put("channel.id", channel.getId());
        
        map.put("createTime", DateUtil.format(createTime));
        map.put("overdueDate", DateUtil.format(overdueDate));
        if(issueDate != null) {
        	map.put("issueDate", DateUtil.format(issueDate));
        }
        
        return map;
    }
 
    public List<Attachment> getAttachments() {
        return attachments;
    }

    public Integer getIsTop() {
        return isTop;
    }

    public void setIsTop(Integer isTop) {
        this.isTop = isTop;
    }

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	public Serializable getPK() {
		return this.id;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getCommentNum() {
		return commentNum;
	}

	public void setCommentNum(int commentNum) {
		this.commentNum = commentNum;
	}

	public Integer getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
	}

	public String getHtmlRef() {
		return htmlRef;
	}

	public void setHtmlRef(String htmlRef) {
		this.htmlRef = htmlRef;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}