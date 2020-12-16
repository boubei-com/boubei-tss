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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.boubei.tss.cms.AbstractTest4CMS;
import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.lucene.ArticleContent;
import com.boubei.tss.cms.lucene.executor.KeywordIndexExecutor;
import com.boubei.tss.cms.lucene.executor.TitleIndexExecutor;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.Progressable;

/**
 * 定时策略相关模块的单元测试。
 */
public class CMSJobsTest extends AbstractTest4CMS {
	
	Long siteId;
	Long channel1Id;
	
    public void init() {
        super.init();
 
    	// 新建站点
        Channel site = createSite();
        siteId = site.getId();
        
        // 新建栏目
        Channel channel1 = super.createChannel("时事评论", site, siteId);
        channel1Id = channel1.getId();
        super.createChannel("环球新闻", site, channel1Id);
        
        // 开始测试文章模块
        Long tempArticleId = System.currentTimeMillis();
        
        // 上传附件
        super.uploadDocFile(channel1Id, tempArticleId);
        super.uploadImgFile(channel1Id, tempArticleId);
        
        // 创建文章
        super.createArticle(channel1, tempArticleId).getId();

        List<?> list = getArticlesByChannel(channel1Id);
 
        for(Object temp : list) {
            // 编辑 ---> 待发布
            Article tempArticle = (Article)temp;
            tempArticle.setStatus(CMSConstants.TOPUBLISH_STATUS);
        }
    }
	
	@Test
    public void testExpireJob() {
		List<?> list = getArticlesByChannel(channel1Id);
		 
        for(Object temp : list) {
            Article tempArticle = (Article)temp;
            tempArticle.setOverdueDate(new Date());
            tempArticle.setStatus(CMSConstants.XML_STATUS);
        }
		
        String jobConfig = siteId.toString();
        new ExpireJob().excuteJob(jobConfig);
	}
	
	@Test
    public void testPublishJob() {
        String jobConfig = siteId.toString();
        new PublishJob().excuteJob(jobConfig);
    }
	
	@Test
    public void testIndexJob() {
		// 因进度条里无法启动事务，直接调用excute方法来测试
		List<Long> channelIds = channelService.getAllEnabledChannelIds(siteId);
    	Set<ArticleContent> data = channelService.getIndexableArticles(channelIds, true);
    	
    	Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("data", data); 
        paramsMap.put("siteId", siteId);
  
        IndexJob indexJob = new IndexJob();
		((Progressable)indexJob).execute(paramsMap, new Progress(data.size()));
		
		try { Thread.sleep(1500); } catch(Exception e) { }
    }
	
	@Test
    public void testIndexJob2() {
        String jobConfig = siteId.toString();
        
        IndexJob indexJob = new IndexJob();
		indexJob.excuteJob(jobConfig);
		
		try { Thread.sleep(1500); } catch(Exception e) { }
		
		JobStrategy.getIndexStrategy().executorClass = TitleIndexExecutor.class.getName();
		indexJob.excuteJob(jobConfig);
		
		JobStrategy.getIndexStrategy().executorClass = KeywordIndexExecutor.class.getName();
		indexJob.excuteJob(jobConfig);
    }
}
