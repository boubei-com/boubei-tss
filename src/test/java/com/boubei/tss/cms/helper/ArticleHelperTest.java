/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.helper;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.util.DateUtil;

public class ArticleHelperTest {
	
	@Test
	public void test() {
		Channel site = new Channel();
		site.setPath("temp");
		System.out.println( site.getPath() );
		
		Date now = new Date();
		Channel channel = new Channel();
		Date overDate = ArticleHelper.calculateOverDate(channel);
		Assert.assertTrue(overDate.after( DateUtil.addDays(now, 365*99) ));
		
		for(int i = 0; i <= 5; i++) {
			channel.setOverdueDate(String.valueOf(i));
			channel.setId( (long) i );
			
			overDate = ArticleHelper.calculateOverDate(channel);
			Assert.assertTrue(overDate.after( now ));
		}
		
		channel.setOverdueDate("6");
		overDate = ArticleHelper.calculateOverDate(channel);
		Assert.assertTrue( DateUtil.format(now).equals(DateUtil.format(overDate)) );
		
		Assert.assertEquals(DateUtil.format(now, "yyyy/MM/dd"), 
				ArticleHelper.getArticlePublishPath(new Article()));
		
        try{
        	ArticleHelper.getAttachmentPath(channel, -100);
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertEquals(EX.CMS_3, e.getMessage());
	    }
        
        String encoding = ArticleHelper.getSystemEncoding();
        System.setProperty("file.encoding", "UTF-8");
        Assert.assertEquals("UTF-8", ArticleHelper.getSystemEncoding());
        System.setProperty("file.encoding", "GBK");
        Assert.assertEquals("GBK", ArticleHelper.getSystemEncoding());
        System.setProperty("file.encoding", "ISO-88591");
        Assert.assertEquals("UTF-8", ArticleHelper.getSystemEncoding());
        System.setProperty("file.encoding", encoding);
        Assert.assertEquals(encoding, ArticleHelper.getSystemEncoding());
	}
	
}
