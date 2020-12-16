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

import java.util.List;

import javax.persistence.Query;

import org.springframework.stereotype.Repository;

import com.boubei.tss.EX;
import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.TreeSupportDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;

/**
 * Channel的Dao层，负责处理Channel相关的数据库操作
 */
@Repository("ChannelDao")
public class ChannelDao extends TreeSupportDao<Channel> implements IChannelDao {
    
	public ChannelDao() {
        super(Channel.class);
    }
	
	public Channel updateChannel(Channel channel) {
//		update(channel);
		return channel;
	}
 
    public Channel getSiteByChannel(Long channelId) {
        Channel channel = getChannelById(channelId);
        if(channel.isSiteRoot()) {
            return channel;
        }
        return (Channel) getEntity(Channel.class, channel.getSite().getId());
    }
 
    public List<?> getAllSiteChannelList() {
		return getEntities("from Channel where domain is null or domain = ? order by decode", Environment.getDomain());
	}

	public List<?> getChannelsBySiteIdNoPermission(Long siteId) {
        Channel site = getChannelById(siteId);
		return getEntities("from Channel c where c.site.id <> c.id and c.decode like ?", site.getDecode() + "%");
	}
 
    private Channel getChannelById(Long channelId){
        Channel channel = getEntity(channelId);
        if(channel == null){
            throw new BusinessException( EX.parse(EX.CMS_2, channelId) );
        }
        return channel;
    }
 
	public List<Channel> getChildrenById(Long channelId, String operationId) {
        return getChildrenById(channelId);
	}
	
	public List<Channel> getParentsById(Long channelId, String operationId) {
	    return getParentsById(channelId);
	}
 
	public boolean checkBrowsePermission(Long channelId) {
        String hql = "select distinct v from RoleUserMapping r, ChannelPermission v " +
                " where v.resourceId= ? and v.roleId = r.id.roleId and r.id.userId = ? and v.operationId = ?";
        List<?> list = getEntities(hql, channelId, Environment.getNotnullUserId(), CMSConstants.OPERATION_VIEW);
        return list.size() > 0 ;
    }
 
    //-------------------------------------------------- 文章发布相关 －－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    
    public int getPublishableArticlesDeeplyCount(Long channelId, String category ) {
        String decode = getChannelById(channelId).getDecode();
        
        String hql = "select count(*) from Article a where a.channel.decode like ?  and ";

        // 取可发布的文章总数
        List<?> list;
        if (CMSConstants.PUBLISH_ALL.equals(category)) {  //完全发布的话已经发布的也重新发布
            hql += " ( a.status = ? or a.status = ? ) ";
            list = getEntities(hql, decode + "%", CMSConstants.TOPUBLISH_STATUS, CMSConstants.XML_STATUS);
        } 
        else {
            hql += " ( a.status = ? ) ";
            list = getEntities(hql, decode + "%", CMSConstants.TOPUBLISH_STATUS);
        }
 
        return EasyUtils.obj2Int(list.get(0));
    }

    @SuppressWarnings("unchecked")
    public List<Article> getPagePublishableArticlesDeeply(Long channelId, String category, int pageNum, int pageSize) {
        String decode = getChannelById(channelId).getDecode();
        
        String hql = "from Article a where a.channel.decode like ? and ";
        Query query;
        if (CMSConstants.PUBLISH_ALL.equals(category)) {
            hql += " ( a.status = ? or a.status = ? ) ";
            query = em.createQuery(hql);
            query.setParameter(3, CMSConstants.XML_STATUS);
        } 
        else {
            hql += " ( a.status = ? ) ";
            query = em.createQuery(hql);
        }
        
        query.setParameter(1, decode + "%");
        query.setParameter(2, CMSConstants.TOPUBLISH_STATUS);
        query.setFirstResult(pageSize * (pageNum - 1));
        query.setMaxResults(pageSize);
        
        return query.getResultList();
    }
 
    @SuppressWarnings("unchecked")
    public List<Article> getPagePublishableArticles(Long channelId, int pageNum, int pageSize) {
        String hql = "from Article a where a.channel.id = ? and a.status = ? ";
        
        Query query = em.createQuery(hql);
        query.setParameter(1, channelId);
        query.setParameter(2, CMSConstants.TOPUBLISH_STATUS);
        query.setFirstResult( pageSize * (pageNum - 1) );
        query.setMaxResults( pageSize );
        
        return query.getResultList();
    }
}
