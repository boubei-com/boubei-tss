/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.module;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.cms.AbstractTest4CMS;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.service.IRemoteArticleService;

/**
 * 文章站点栏目相关模块的单元测试。
 */
public class ArticleAPITest extends AbstractTest4CMS {
	
	@Autowired IRemoteArticleService articleAPI;

	@Test
    public void testArticleAPI() {
    	// 新建站点
        Channel site = createSite();
        Long siteId = site.getId();
        
        // 新建栏目
        Channel channel1 = super.createChannel("时事评论2", site, siteId);
        Long channelId = channel1.getId();
        
        // 开始测试文章模块
		articleAction.initArticleInfo(response, channelId);
        Long tempArticleId = System.currentTimeMillis();
        
        // 上传附件
        super.uploadDocFile(channelId, tempArticleId);
        super.uploadImgFile(channelId, tempArticleId);
        
        Article article = super.createArticle(channel1, tempArticleId);
        Long articleId = article.getId();
        
        // test getArticleListByChannelAndTime
    	try {
    		articleAPI.getArticleListByChannelAndTime(null, null, null);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("栏目ID不能为空!", true);
		}
        
    	try {
    		articleAPI.getArticleListByChannelAndTime(channelId, 2017, null);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("栏目ID不能为空!", true);
		}
        
    	try {
    		articleAPI.getArticleListByChannelAndTime(-1L, 2017, 1);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("栏目ID不能为空!", true);
		}
    	articleAPI.getArticleListByChannelAndTime(channelId, 2016, 11);
    	
    	// test getAttachmentInfo
    	Assert.assertNull(articleAPI.getAttachmentInfo(articleId, -1, null));
    	Assert.assertNotNull(articleAPI.getAttachmentInfo(articleId, 1, channelId));
    	
    	// test search
    	String result = articleAPI.search(siteId, null, 1, 100);
    	log.debug(result);
    	try {
    		articleAPI.search(-100L, null, 1, 100);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue( EX.parse(EX.CMS_22, -100L) , true);
		}
    	
    	// test getArticleXML
    	Assert.assertEquals("", articleAPI.getArticleXML(-92L));
    }
}
