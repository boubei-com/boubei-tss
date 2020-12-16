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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.cms.AbstractTest4CMS;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.helper.ArticleQueryCondition;
import com.boubei.tss.framework.Config;
import com.boubei.tss.modules.param.ParamConstants;

/**
 * 文章站点栏目相关模块的单元测试。
 */
public class ArticleModuleTest extends AbstractTest4CMS {
	
	@Test
    public void testArticleAttach() {
		Assert.assertNull( articleDao.getAttachment(-1L, 1) );
	}
 
	@Test
    public void testArticleModule() {
    	// 新建站点
        Channel site = createSite();
        Long siteId = site.getId();
        
        // 新建栏目
        Channel channel1 = super.createChannel("时事评论", site, siteId);
        Channel channel2 = super.createChannel("体育新闻", site, siteId);
        Channel channel3 = super.createChannel("NBA战况", site, channel2.getId());
        Long channelId = channel1.getId();
        
        channel2.setOverdueDate("3");
        channel2.setOverdueDate("4");
        channel3.setOverdueDate("5");
        
        // 开始测试文章模块
		articleAction.initArticleInfo(response, channelId);
        Long tempArticleId = System.currentTimeMillis();
        
        // 上传附件
        super.uploadDocFile(channelId, tempArticleId);
        super.uploadImgFile(channelId, tempArticleId);
        super.uploadDocFile(channelId, tempArticleId);
        super.uploadImgFile(channelId, tempArticleId);
        super.uploadDocFile(channelId, tempArticleId);
        super.uploadImgFile(channelId, tempArticleId);
        
        Article article = super.createArticle(channel1, tempArticleId);
        Long articleId = article.getId();
        
        articleAction.getArticleInfo(response, articleId);
        
        // 修改文章
        request.removeParameter("attachList");
        request.addParameter("attachList", "1,5");
        request.removeParameter("isCommit");
		request.addParameter("isCommit", Config.TRUE);
        articleAction.saveArticleInfo(response, request, article);
        
        request.removeParameter("attachList");
        request.addParameter("attachList", "");
        articleAction.saveArticleInfo(response, request, article);
        
        List<?> list = getArticlesByChannel(channelId);
        assertNotNull(list);
        assertEquals(1, list.size());
        
        // 置顶、解除置顶
        articleAction.doOrUndoTopArticle(response, articleId);
        assertEquals(article.getIsTop(), ParamConstants.TRUE);
        assertNotNull(article.getSeqNo());
        
        articleAction.doOrUndoTopArticle(response, articleId);
        assertEquals(article.getIsTop(), ParamConstants.FALSE);
        
        articleAction.getChannelArticles(response, channelId, 1);
        
		try {
			articleService.getChannelArticles(-99L, 1);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.CMS_5, -99L) , e.getMessage());
		}
        
        ArticleQueryCondition condition = new ArticleQueryCondition();
        condition.setTitle("轮回");
        condition.setChannelId(channelId);
		articleAction.queryArticles(response, 1, condition);
		
		condition.setChannelId(channelId);
		condition.setCreateTime(new Date());
		condition.setIsDesc(ParamConstants.TRUE);
		condition.setAuthor("Jon.King");
		condition.setKeyword("历史 轮回");
		condition.setSummary("历史");
		condition.setOrderField("author");
		articleAction.queryArticles(response, 1, condition);
		
		// 移动文章
        articleAction.moveArticle(response, article.getId(), channel3.getId());
        assertEquals(article.getChannel().getId(), channel3.getId());
    	try {
    		articleService.moveArticle(articleId, siteId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.CMS_6, e.getMessage());
		}
        
        // 添加/删除/读取评论
        request.addParameter("comment", "very good!");
        articleAction.addComment(response, request, articleId);
        
        request.addParameter("comment", "so bad!");
        articleAction.addComment(response, request, articleId);
        
        List<?> comments = articleAction.getComment(request, articleId);
        Assert.assertTrue(comments.size() == 2);
        
        articleAction.delComment(request, response, articleId, 1);
        articleAction.delComment(request, response, articleId, 100);
        
        comments = articleAction.getComment(request, articleId);
        Assert.assertTrue(comments.size() == 1);
       
        // 最后删除文章、栏目、站点
        articleAction.deleteArticle(response, articleId);
    	try {
    		articleService.deleteArticle(articleId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.CMS_0, e.getMessage());
		}
        
        deleteSite(siteId);
        
        assertTrue(_TestUtil.printLogs(logService) > 0);
    }
}
