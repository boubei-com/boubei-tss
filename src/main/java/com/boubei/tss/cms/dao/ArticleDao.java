/* ==================================================================   
 * Created [2006-12-28] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.dao;

import java.io.File;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Attachment;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.helper.ArticleHelper;
import com.boubei.tss.cms.helper.ArticleQueryCondition;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.persistence.pagequery.PaginationQueryByHQL;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.EasyUtils;

/**
 * Article的Dao层，负责处理Article相关的数据库操作
 */
@Repository("ArticleDao")
public class ArticleDao extends BaseDao<Article> implements IArticleDao {
    
    public ArticleDao() {
        super(Article.class);
    }
 
    public Article saveArticle(Article article) {
    	article.setDomain(Environment.getDomain());
        return create(article);
    }

    public void deleteArticle(Article article) {
        Long articleId = article.getId();
        
        // 删除附件
        List<Attachment> attachments = getArticleAttachments(articleId); // 后台查找的新建文章时上传的附件列表
        for ( Attachment attachment : attachments ) {
            // 删除附件 TODO 如果是缩略图则还需删除原图片
            Channel site = article.getChannel().getSite();
			new File( ArticleHelper.getAttachUploadPath(site, attachment)[0] ).delete();
           
            // 删除老的附件信息
            super.delete(attachment);
        }
        
        // 删除文章生成的xml文件 （如果有的话）
        String pubUrl = article.getPubUrl();
        if ( !EasyUtils.isNullOrEmpty(pubUrl) ) {
            new File(pubUrl).delete(); 
        }
        
        super.delete(article);
    }
    
    public Integer getAttachmentIndex(Long articleId) {
        String hql = "select max(o.seqNo) from Attachment o where o.articleId = ?";
        List<?> list = getEntities(hql, articleId);
        Integer nextSeqNo = (Integer) list.get(0);
        if (nextSeqNo == null) {
        	nextSeqNo = 0;
        }
        return nextSeqNo + 1;
    }
    
    public Attachment getAttachment(Long articleId, Integer seqNo) {
        String hql = " from Attachment o where o.articleId = ? and o.seqNo = ?";
        List<?> list = getEntities(hql, articleId, seqNo);
        if( list.size() > 0 ) {
            Attachment attachment = (Attachment) list.get(0);
            attachment.setArticle(getEntity(attachment.getArticleId()));
            
			return attachment;
        }
        return null;
    }
 
	@SuppressWarnings("unchecked")
	public List<Attachment> getArticleAttachments(Long articleId) {
        List<Attachment> list = (List<Attachment>)getEntities("from Attachment o where o.articleId = ? order by seqNo", articleId);
		for ( Attachment attachment : list ) {
			attachment.setArticle(getEntity(articleId));
		}
		return list;
	}

	public List<?> getPublishedArticleByChannel(Long channelId) {
		String hql = "select o.id, o.pubUrl, o.issueDate " +
				" from Article o " +
				" where o.channel.id = ? and o.status = ? ";
        return getEntities(hql, channelId, CMSConstants.XML_STATUS );
	}
 
    //* *****************************************            for page search         ********************************************
    
	public PageInfo getPageList(Long channelId, Integer pageNum) {
	    String hql = "select o.id, o.title, o.author, o.issueDate, o.summary, "
				+ "  o.hitCount, o.creatorName, o.createTime, "
				+ "  o.status, o.channel, o.isTop, o.overdueDate, o.seqNo, o.commentNum"
				+ " from Article o "
				+ " where o.channel.id = :channelId ${domain} ";
		
        ArticleQueryCondition condition = new ArticleQueryCondition();
        condition.setChannelId(channelId);
        condition.getPage().setPageNum(pageNum);
        condition.getOrderByFields().add(" o.seqNo desc, o.issueDate desc, o.id desc ");  //  默认按createTime排序

		PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
		return pageQuery.getResultList();
	}
	
	public PageInfo getArticlePageList(ArticleQueryCondition condition) {
		String hql = "select o.id, o.title, o.author, o.issueDate, o.summary, "
				+ "  o.hitCount, o.creatorName, o.createTime, "
				+ "  o.status, o.channel, o.isTop, o.overdueDate, o.seqNo, o.commentNum"
                + " from Article o, Temp t"
				+ " where o.channel.id = t.id and t.thread=" + Environment.threadID()
				+ "  ${title} ${author} ${keyword} ${summary} ${createTime} ${status} ${domain} " ;
        
        String orderField = condition.getOrderField();
		String orderBy = orderField == null ? null : "o." + orderField;
        if( orderBy != null && ParamConstants.TRUE.equals(condition.getIsDesc()) ) {
            orderBy += " desc ";
        }
        
        if(orderBy == null) {
            orderBy = " o.seqNo desc, o.issueDate desc, o.id desc "; // 默认按文章的创建时间排序
        }
        condition.getOrderByFields().add(orderBy);
 
		PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
		return pageQuery.getResultList();
	}
 

	//* *****************************************  for portlet  ********************************************
     
    public PageInfo getChannelPageArticleList(ArticleQueryCondition condition) {
    	// select字段的顺序不能变，新增字段放最后
        String hql = "select o.id, o.title, o.author, o.summary, o.issueDate, o.createTime, o.hitCount, o.isTop, o.commentNum, o.seqNo, o.htmlRef "
                + " from Article o"
                + " where 1=1 ${channelId} ${status} #{domainOrNoDomain}"
                + " order by o.isTop desc, o.seqNo desc, o.issueDate desc, o.id desc";
 
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
        return pageQuery.getResultList();
    }
    
    public PageInfo getArticlesByChannelIds(ArticleQueryCondition condition) {
        insertIds2TempTable(condition.getChannelIds());
        condition.setChannelIds(null);
 
        String hql = "select o.id, o.title, o.author, o.summary, o.issueDate, o.createTime, o.hitCount, o.isTop, o.commentNum, o.seqNo, o.htmlRef "
                    + " from Article o, Temp t "
                    + " where o.channel.id = t.id and t.thread=" + Environment.threadID()
                    + " ${status} ${createTime} #{domainOrNoDomain} "
                    + " order by o.isTop desc, o.seqNo desc, o.issueDate desc, o.id desc";
        
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
        return pageQuery.getResultList();
    }
}