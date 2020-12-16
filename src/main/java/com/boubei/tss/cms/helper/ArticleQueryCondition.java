/* ==================================================================   
 * Created [2006-12-28] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.helper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.boubei.tss.PX;
import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConfig;

/** 
 * 文章列表的查询条件
 */
public class ArticleQueryCondition extends MacrocodeQueryCondition {
 
    private String  title;
    private String  author;
    private Integer status;  // 流程状态
    private Date    createTime;
   
    private Long    channelId;
    private List<Long> channelIds;
    
    private String keyword;
    private String summary;
    
    private String orderField;  // 排序字段
    private Integer isDesc;     // 是否降序排序
    
    public Set<String> getIgnoreProperties() {
        super.getIgnoreProperties().add("channelIds");
        super.getIgnoreProperties().add("orderField");
        super.getIgnoreProperties().add("isDesc");
        return super.getIgnoreProperties();
    }
    
    public Map<String, Object> getConditionMacrocodes() {
        Map<String, Object> map = super.getConditionMacrocodes();
        map.put("${title}",  " and o.title  like :title");
        map.put("${author}", " and o.author like :author");
        map.put("${keyword}", " and o.keyword like :keyword");
        map.put("${summary}", " and o.keyword like :summary");
        
        map.put("${status}", " and o.status = :status");
        map.put("${createTime}", " and o.createTime > :createTime");
        
        map.put("${channelId}",  " and o.channel.id = :channelId");
        
        String publicDomains = ParamConfig.getAttribute(PX.PUBLIC_DOMAINS, "'无域','BD'");
        map.put("#{domainOrNoDomain}", " and o.domain in (" +publicDomains+ ",'" +Environment.getDomain()+ "')");

        return map;
    }
 
    public String getAuthor() {
        return wrapLike(author);
    }
    public String getTitle() {
        return wrapLike(title);
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
 
    public Integer getStatus() {
        return status;
    }
 
    public void setStatus(Integer status) {
        this.status = status;
    }
 
    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Long getChannelId() {
        return channelId;
    }

    public List<Long> getChannelIds() {
        return channelIds;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public void setChannelIds(List<Long> channelIds) {
        this.channelIds = channelIds;
    }
 
    public String getOrderField() {
        return orderField;
    }

    public void setOrderField(String orderField) {
        this.orderField = orderField;
    }

    public Integer getIsDesc() {
        return isDesc;
    }

    public void setIsDesc(Integer isDesc) {
        this.isDesc = isDesc;
    }
    
	public String getKeyword() {
		return wrapLike(keyword);
	}
	
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getSummary() {
		return wrapLike(summary);
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
}