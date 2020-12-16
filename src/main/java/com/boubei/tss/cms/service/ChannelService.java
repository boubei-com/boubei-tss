/* ==================================================================   
 * Created [2006-12-28] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.service;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.dao.IArticleDao;
import com.boubei.tss.cms.dao.IChannelDao;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Attachment;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.helper.ArticleHelper;
import com.boubei.tss.cms.job.PublishUtil;
import com.boubei.tss.cms.lucene.ArticleContent;
import com.boubei.tss.cms.lucene.IndexHelper;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.EasyUtils;

@Service("ChannelService")
public class ChannelService implements IChannelService {

    @Autowired private IChannelDao channelDao;
    @Autowired private IArticleDao articleDao;
     
    public List<?> getAllSiteChannelList() {
        return channelDao.getAllSiteChannelList();
    }
 
	public Channel getChannelById(Long id) {
        Channel channel = channelDao.getEntity(id);
        if (channel == null) {
            throw new BusinessException( EX.parse(EX.CMS_9, id));
        }
        return channel;
	}
	
	public Channel createChannel(Channel channel) {
        Long parentId = channel.getParentId();
        Integer nextSeqNo = channelDao.getNextSeqNo(parentId);
		channel.setSeqNo(nextSeqNo);
		
		Channel parent = channelDao.getEntity(parentId);
        channel.setSite(parent.getSite());
        channel.setDomain(parent.getSite().getDomain());
		
        channel = channelDao.create(channel);
        
	    return channel;
    }
	
	public Channel updateChannel(Channel channel) {
		Long parentId = channel.getParentId();
		Channel parent = channelDao.getEntity(parentId);
        channel.setDomain(parent.getDomain());
        
		channelDao.updateChannel(channel);
		
	    return channel;
    }
	
    public Channel createSite(Channel site) {
        checkPath(site.getPath(), site.getDocPath(), site.getImagePath());
        
        site.setSeqNo(channelDao.getNextSeqNo(CMSConstants.HEAD_NODE_ID));
        site = channelDao.create(site);
        
        site.setSite(site);
        return site;
    }
    
    public Channel updateSite(Channel site) {
        checkPath(site.getPath(), site.getDocPath(), site.getImagePath());
        channelDao.updateChannel(site);
        return site;
    }
    
    /** 检测输入路径的正确性 */
    private void checkPath(String path, String docPath, String imgPath) {
        checkPath(path,    EX.CMS_12);
        checkPath(path + "/" + docPath, EX.CMS_13);
        checkPath(path + "/" + imgPath, EX.CMS_14);
    }
    
    private void checkPath(String path, String errorMSg){
        File file = new File(path);
        if (!(file.exists() && file.isDirectory() && file.canWrite() )
                && !(file.exists() == false && file.mkdirs() == true)) {
            throw new BusinessException(errorMSg);
        }
    }
 
	public void deleteChannel(Long channelId) {
		if (!checkDownPermission(channelId, CMSConstants.OPERATION_DELETE)) {
			throw new BusinessException(EX.CMS_7);
		}
		Channel channel = channelDao.getEntity(channelId);
		
		// 如果删除的站点，则先把对自己的引用去掉，否则因为有外键存在无法删除
		if(channel.isSiteRoot()) {
			channel.setSite(null);
			channelDao.updateChannel(channel); 
		}
		
		// 先删除栏目下文章
        String hql = "from Article a where a.channel.decode like ?";
        List<?> articleList = channelDao.getEntities(hql, channel.getDecode() + "%");
		for ( Object temp : articleList ) {
			Article article = (Article) temp;
			articleDao.deleteArticle(article);
		}
		
        // 删除自己和子栏目
		List<Channel> subChannels = channelDao.getChildrenById(channelId);
		channelDao.deleteAll(subChannels);
	}
	
    /** 对栏目向下权限的判断: 如果个数不相等，说明用户某些子节点不具有指定的权限 */
    protected boolean checkDownPermission(Long resourceId, String operationId) {
        List<Channel> rsourceList  = channelDao.getChildrenById(resourceId);
        List<?> permitedList = channelDao.getChildrenById(resourceId, operationId);

        return rsourceList.size() == permitedList.size();
    }
 
	public void sortChannel(Long channelId, Long toChannelId, Integer direction) {
        channelDao.sort(channelId, toChannelId, direction);
    }
 
    public void moveChannel(Long channelId, Long targetId) {
        Channel channel = channelDao.getEntity(channelId);
        channel.setSeqNo(channelDao.getNextSeqNo(targetId));
        channel.setParentId(targetId);
        channelDao.moveEntity(channel);
        
        Channel target  = channelDao.getEntity(targetId);
        List<Channel> children = channelDao.getChildrenById(channelId);
        for ( Channel temp : children ) {
            temp.setSite(target.getSite());
            temp.setDisabled(target.getDisabled());
 
            channelDao.update(temp);
        }
    }
 
    public void disable(Long siteOrChannelId) {
        if ( !checkDownPermission(siteOrChannelId, CMSConstants.OPERATION_STOP_START) ) {
            throw new BusinessException(EX.CMS_8);
        }
 
        List<Channel> list = channelDao.getChildrenById(siteOrChannelId);
        for ( Channel child : list ) {
            child.setDisabled(CMSConstants.STATUS_STOP);
        }
        
        // 同时将栏目下所有文章设为待发布状态，禁止被搜索到
        channelDao.executeHQL("update Article set status = ? where channel.id = ? and status = ?", 
        		CMSConstants.TOPUBLISH_STATUS, siteOrChannelId, CMSConstants.XML_STATUS);
        
        channelDao.flush();
    }

    public void enableSite(Long siteId) {
        // 启用站点
        Channel channel = channelDao.getEntity(siteId);
        channel.setDisabled(CMSConstants.STATUS_START);
        
        // 启用站点下栏目
        List<?> list = channelDao.getChannelsBySiteIdNoPermission(siteId);
        for ( Object entity : list ) {
            Channel temp = (Channel) entity;
            temp.setDisabled(CMSConstants.STATUS_START);
        }
        channelDao.flush();
    }
 
    public void enableChannel(Long channelId) {
        // 启用所有父亲节点
        List<Channel> parents = channelDao.getParentsById(channelId, CMSConstants.OPERATION_STOP_START);
        for ( Channel parent : parents ) {
            parent.setDisabled(CMSConstants.STATUS_START);
        }
        channelDao.flush();
    }
 
    
    /**************************************************** 以下为栏目（站点）文章发布 *************************************************/

	public void publishArticle(List<Article> articleList) {
		for ( Article article : articleList) {
            // 更新发布日期
			article.setIssueDate( (Date) EasyUtils.checkNull(article.getIssueDate(), new Date()) );
			
			// 取文章附件列表
            List<Attachment> attachments = articleDao.getArticleAttachments(article.getId());
			article.getAttachments().addAll(attachments);
			
			// 发布文章，根据文章 创建日期 来设置xml文件的存放路径
			Channel site = channelDao.getEntity(article.getChannel().getId()).getSite();
            String publishPath = site.getPath()+ "/" + ArticleHelper.getArticlePublishPath(article);
			String pubUrl = PublishUtil.publishOneArticle(article, publishPath);
			
			// 在文章对象里记录发布路径
			article.setPubUrl(pubUrl);
			article.setStatus(CMSConstants.XML_STATUS);
			articleDao.update(article);
		}
	}

    public int getPublishableArticlesDeeplyCount(Long channelId, String category ) {
        return channelDao.getPublishableArticlesDeeplyCount(channelId, category);
    }
    
    public List<Article> getPagePublishableArticlesDeeply(Long channelId, int page, int pageSize, String category) {
        return channelDao.getPagePublishableArticlesDeeply(channelId, category, page, pageSize);
    }
    
    public Integer getPublishableArticlesCount(Long channelId) {
        String hql = "select count(*) from Article a where a.channel.id = ? and a.status = ?";
        List<?> list = channelDao.getEntities(hql, channelId, CMSConstants.TOPUBLISH_STATUS);
        return EasyUtils.obj2Int(list.get(0));
    }

    public List<Article> getPagePublishableArticles(Long channelId, int page, int pageSize) {
        return channelDao.getPagePublishableArticles(channelId, page, pageSize);
    }
    
	@SuppressWarnings("unchecked")
	public List<Long> getAllEnabledChannelIds(Long siteId) {
		String hql = "select o.id from Channel o where o.id <> o.site.id and o.disabled <> 1 and o.site.id = ?";
		return (List<Long>) channelDao.getEntities(hql, siteId);
	}
	
    @SuppressWarnings("unchecked")
	public List<Article> getExpireArticlePuburlList(Date now, Long channelId) {
        String hql = "from Article a " +
                " where a.channel.id = ? and a.status = ? and a.overdueDate <= ? ";
        
        // 需要过期的文章为”已发布“状态的文章。其他状态没必要设置为过期
        return (List<Article>) channelDao.getEntities(hql, channelId, CMSConstants.XML_STATUS, now);
    }
    
    public Set<ArticleContent> getIndexableArticles(List<Long> channelIds, boolean isIncrement) {
    	return IndexHelper.getIndexableArticles(channelIds, isIncrement, channelDao, articleDao);
    }
}