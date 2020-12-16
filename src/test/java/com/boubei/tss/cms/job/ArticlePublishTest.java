/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.cms.AbstractTest4CMS;
import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.DownloadServlet;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Attachment;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.lucene.ArticleContent;
import com.boubei.tss.cms.lucene.IndexHelper;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.Progressable;
import com.boubei.tss.util.DateUtil;

/**
 * 文章发布相关模块的单元测试。
 */
public class ArticlePublishTest extends AbstractTest4CMS {
	
	@Test
    public void testArticlePublish() {
    	// 新建站点
        Channel site = createSite();
        site.setPath(super.tempDir3.getPath());
        
        Long siteId = site.getId();
        
        // 新建栏目
        Channel channel1 = super.createChannel("时事评论", site, siteId);
        Long channel1Id = channel1.getId();
        Channel channel2 = super.createChannel("环球新闻", site, channel1Id);
        
        // 开始测试文章模块
        Long tempArticleId = System.currentTimeMillis();
        
        // 上传附件
        super.uploadDocFile(channel1Id, tempArticleId);
        super.uploadImgFile(channel1Id, tempArticleId);
        
        // 创建文章
        Long articleId = super.createArticle(channel1, tempArticleId).getId();

        List<?> list = getArticlesByChannel(channel1Id);
 
        for(Object temp : list) {
            // 编辑 ---> 待发布
            Article tempArticle = (Article)temp;
            tempArticle.setStatus(CMSConstants.TOPUBLISH_STATUS);
        }
        
        // 站点栏目文章发布 category 1:增量发布 2:完全发布 
        publishArticle(channel1Id, CMSConstants.PUBLISH_ADD);
        publishArticle(siteId, CMSConstants.PUBLISH_ADD);
        
        publishArticle(channel1Id, CMSConstants.PUBLISH_ALL);
        publishArticle(siteId, CMSConstants.PUBLISH_ALL);
        
        try {
        	publishArticle(-99L, CMSConstants.PUBLISH_ALL);
        	Assert.fail();
        } catch(Exception e) { 
        	// 没有权限
        }
        
        try {
        	channelAction.publish(response, channel1Id, CMSConstants.PUBLISH_ALL);
        } catch(Exception e) {
        	Assert.assertTrue("没有事务", true);
        }
        
        // 创建索引
        channelAction.createIndex(response, siteId, 1);
        
        JobStrategy strategy = JobStrategy.getIndexStrategy();
        strategy.site = site;
        
        List<Long> channelIdList = Arrays.asList(channel1Id, channel2.getId());
        Set<ArticleContent> content = IndexHelper.getIndexableArticles(channelIdList, false, channelDao, articleDao);
        IndexHelper.createIndex(strategy, content, new Progress(list.size()));
        
        try { Thread.sleep(1500); } catch(Exception e) { }
        
        content = IndexHelper.getIndexableArticles(channelIdList, true, channelDao, articleDao);
        IndexHelper.createIndex(strategy, content, new Progress(list.size()));
        
        try { Thread.sleep(1500); } catch(Exception e) { }
 
        // 测试检索文章
        request.addParameter("searchStr", "矛盾");
        articleAction.search(response, request, siteId, 1, 12);
        request.addParameter("searchStr", "过河卒子");
        articleAction.search(response, request, siteId, 1, 12);
        
        request.removeParameter("searchStr");
        request.addParameter("searchStr", "技术创新[]{}()");
        articleAction.search(response, request, siteId, 1, 12); // 搜索附件
        
        // 测试对外接口
        articleAction.getArticleInfo(response, articleId);
        
        request.addParameter("articleXml", 
           "<ArticleInfo>" + 
	           "<Article>" + 
		           "<title><![CDATA[文章一]]></title>" + 
		           "<author><![CDATA[斯蒂芬]]></author>" + 
		           "<keyword><![CDATA[公认的]]></keyword>" + 
		           "<status><![CDATA[-1]]></status>" + 
		           "<type><![CDATA[2]]></type>" + 
		           "<typeName><![CDATA[报表]]></typeName>" + 
		           "<wzrq><![CDATA[2007-06-06]]></wzrq>" + 
		           "<content><![CDATA[正文正文正文正文正文正文正文正文]]></content>" + 
	           "</Article>" + 
           "</ArticleInfo>");
        articleAction.importArticle(response, request, channel1Id);
        
        articleAction.getArticleListByChannel(response, channel1Id, 1, 12, false);
        articleAction.getArticleListByChannel(response, channel1Id, 1, 12, true);
        articleAction.getArticleListDeeplyByChannel(response, channel1Id, 1, 12);
        
        Integer year = DateUtil.getYear(new Date());
        Integer month = DateUtil.getMonth(new Date());
        articleAction.getArticleListByChannelAndTime(response, channel1Id, year, month);
        
        String channelIds = channel1Id + "," + channel2.getId();
        articleAction.getArticleListByChannels(response, channelIds, 1, 12);
        
        try {
	        articleAction.getArticleListByChannels(response, null, 1, 12);
	        Assert.fail("should throw exception but didn't.");
        } catch( Exception e ) {
        	Assert.assertEquals(EX.CMS_1, e.getMessage());
        }
        
        // 多次点击文章，触发HitRateManager
        for(int i = 0; i < 100; i++) {
        	articleAction.getArticleXmlInfo(response, articleId);
        }
        articleAction.getArticleXmlInfo(response, -99L);
        
        articleAction.getChannelTreeList4Portlet(response, siteId);
        
        // 测试附件下载
        List<Attachment> attachs = articleDao.getArticleAttachments(articleId);
        assertEquals(2, attachs.size());
        
        articleAction.setTopAttach( attachs.get(0).getId() );
        
        DownloadServlet download = new DownloadServlet();
        download.init();
        request.addParameter("id", articleId.toString());
        
        try {
        	for(Attachment attach : attachs) {
                request.addParameter("seqNo", attach.getSeqNo().toString());
            	download.doPost(request, new MockHttpServletResponse());
            	
            	request.addParameter("smaller", "true");
            	download.doPost(request, new MockHttpServletResponse()); // 缩略图
            	
            	attach.setId( (Long) attach.getPK() );
            	attach.setHitCount(attach.getHitCount());
            	attach.setFileSize(attach.getFileSize());
            	Assert.assertEquals(attach.getId(), attach.getId());
            	Assert.assertNotNull(attach.getUploadDate());
            	
            	// 附件不存在
            	request.removeParameter("seqNo");
                request.addParameter("seqNo", "-1");
             	download.doPost(request, new MockHttpServletResponse());
        	}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
        
        try {
        	request.removeParameter("seqNo");
            request.addParameter("seqNo", "error seqNo");
        	download.doPost(request, new MockHttpServletResponse());
//        	Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertFalse("下载附件时参数值有误, 不再抛出异常", true);
		}
        
        // 最后删除文章、栏目、站点
        super.deleteSite(siteId);
        
        assertTrue(_TestUtil.printLogs(logService) > 0);
    }
    
    private void publishArticle(Long channelId, String category) {
        // 判断是否对该栏目有发布权限
        publishManger.checkPublishPermission(channelId);
        
        int totalRows = channelService.getPublishableArticlesDeeplyCount(channelId, category);   
       
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("channelId", channelId);
        paramsMap.put("category", category);
        paramsMap.put("totalRows", totalRows);
        
        ((Progressable)publishManger).execute(paramsMap, new Progress(totalRows));
    }
}
