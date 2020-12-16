/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4TSS;
import com.boubei.tss._TestUtil;
import com.boubei.tss.cms.action.ArticleAction;
import com.boubei.tss.cms.action.ChannelAction;
import com.boubei.tss.cms.dao.IArticleDao;
import com.boubei.tss.cms.dao.IChannelDao;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.job.PublishManger;
import com.boubei.tss.cms.service.IArticleService;
import com.boubei.tss.cms.service.IChannelService;
import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

public class AbstractTest4CMS extends AbstractTest4TSS { 
 
    @Autowired protected IChannelService channelService;
    @Autowired protected IArticleService articleService;
    @Autowired protected IChannelDao channelDao;
    @Autowired protected IArticleDao articleDao;
    
    protected File tempDir1;
    protected File tempDir2;
    protected File tempDir3;
    
    public static String CK_FILE_PATH = "application.properties";
 
    protected void init() {
    	super.init();
    	
    	URL url = URLUtil.getResourceFileUrl(CK_FILE_PATH);
        String log4jPath = url.getPath(); 
        File classDir = new File(log4jPath).getParentFile();
        Assert.assertTrue(FileHelper.checkFile(classDir, CK_FILE_PATH));
        
        tempDir1 = FileHelper.createDir(classDir + "/temp1");
        tempDir2 = FileHelper.createDir(classDir + "/temp2");
        tempDir3 = FileHelper.createDir(classDir + "/temp3");
    }
    
//    static String UPLOAD_PATH = ParamConfig.getAttribute(Servlet4Upload.UPLOAD_PATH);
    static String UPLOAD_PATH = FileHelper.ioTmpDir() + "/upload";
    
	@Autowired protected ChannelAction channelAction;
    @Autowired protected ArticleAction articleAction;
    @Autowired protected PublishManger   publishManger;
    
    // 新建站点
    protected Channel createSite() {
        Channel site = new Channel();
        site.setParentId(CMSConstants.HEAD_NODE_ID);
        site.setName("我的门户" + System.currentTimeMillis());
        site.setPath(tempDir1.getPath());
        site.setDocPath("doc");
        site.setImagePath("img");
        site.setOverdueDate("0");
        site.setDomain(Environment.getDomainOrign());
        
        site = channelService.createSite(site);
        Long siteId = site.getId();
        assertNotNull(siteId);
        
        return site;
    }
    
    // 新建栏目
    protected Channel createChannel(String name, Channel site, Long parentId) {
        Channel channel = new Channel();
        BeanUtil.copy(channel, site, new String[]{"id", "name", "domain"});
        channel.setName(name);
        channel.setParentId(parentId);
        channel = channelService.createChannel(channel);
        
        return channel;
    }
    
    protected Article createArticle(Channel channel, Long tempArticleId) {
        Article article = new Article();
        article.setTitle("历史的轮回怪圈");
        article.setAuthor("Jon.King");
        article.setSubtitle("对于当前中国社会矛盾的历史思考");
        article.setKeyword("历史 轮回 社会矛盾");
        article.setSummary("历史 轮回 社会矛盾");
        article.setHtmlRef("www.xxx.com/index.html"); article.getHtmlRef();
        article.setOverdueDate(new Date(System.currentTimeMillis() + 1000*60*60*24*365));
        article.setChannel(channel);
        article.setSeqNo(1);
        article.setDomain(Environment.getDomain());
        
        String attachUrl = "download?id=" + tempArticleId + "&amp;seqNo=1";
        String content = " 最近几年以来，社会矛盾逐渐成为了中国公众瞩目的焦点---" +
                "官员腐败，贫富差距扩大化，三农问题，仇富心态，教育改革，房产价格，医疗社保等一系列问题犹如走马灯" +
                "一般纷纷闯入人们的视野，同时，许多富于代表性的事件也先后挑战着中国人眼球和心理的承受能力。" +
                "我们的社会怎么了？相信只要稍微有点头脑的人都会问这个问题，也有很多人提出了解答。其中，有一种最为" +
                "通行的说法就是:历史的轮回。具体地说来，历朝历代的统治时期大致可以分为三个阶段：" +
                "1。天下创世 2。矛盾丛生3。总崩溃    个别王朝由于在中晚期实行了一些改革，缓和了社会矛盾，" +
                "从而延长了统治时间。这是中国历史所特有的周期性振荡。综观国史，史实让笔者也不得不承认上述说法的合理性。" +
                "那么，目前的中国是否在重新陷入这个历史的轮回怪圈呢？这倒的确是一个值得商榷的问题。" + attachUrl;  // 文章正文引用图片
        
		request.addParameter("ArticleContent", content);
		request.addParameter("articleId", tempArticleId.toString());
        
		request.addParameter("attachList", "1,2,3,4");
		request.addParameter("isCommit", "false");
		articleAction.saveArticleInfo(response, request, article);
        Long articleId = article.getId();
        assertNotNull(articleId);
        
        return article;
    }
    
    // 上传附件
    protected void uploadDocFile(Long channelId, Long articleId) {
    	AfterUpload upload = new CreateAttach();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    
	    EasyMock.expect(mockRequest.getParameter("articleId")).andReturn(articleId.toString());
	    EasyMock.expect(mockRequest.getParameter("channelId")).andReturn(channelId.toString());
	    EasyMock.expect(mockRequest.getParameter("type")).andReturn(RecordAttach.ATTACH_TYPE_DOC.toString());
	    EasyMock.expect(mockRequest.getParameter("petName")).andReturn(null);
	    
	    try {
			String filepath = UPLOAD_PATH + "/123.txt";
			FileHelper.writeFile(new File(filepath), "我们爱技术创新。");
	        
	        mocksControl.replay(); 
			upload.processUploadFile(mockRequest, filepath, "123.txt");
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
	    
	    _TestUtil.printEntity(super.permissionHelper, "Attachment"); 
    }
    
    // 上传图片
    protected void uploadImgFile(Long channelId, Long articleId) {
    	AfterUpload upload = new CreateAttach();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    
	    EasyMock.expect(mockRequest.getParameter("articleId")).andReturn(articleId.toString());
	    EasyMock.expect(mockRequest.getParameter("channelId")).andReturn(channelId.toString());
	    EasyMock.expect(mockRequest.getParameter("type")).andReturn(RecordAttach.ATTACH_TYPE_PIC.toString());
	    EasyMock.expect(mockRequest.getParameter("petName")).andReturn(null);
	    
	    try {
			// 先用java创建一张图片
			int width = 100, height = 100;   
	        String s = "TSS LOGO";   
	        
	        String filePath = UPLOAD_PATH + "/111.jpg";
	        File file = new File(filePath);   
	           
	        Font font = new Font("Serif", Font.BOLD, 10);   
	        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);   
	        Graphics2D g2 = (Graphics2D)bi.getGraphics();   
	        g2.setBackground(Color.WHITE);   
	        g2.clearRect(0, 0, width, height);   
	        g2.setPaint(Color.RED);   
	           
	        FontRenderContext context = g2.getFontRenderContext();   
	        Rectangle2D bounds = font.getStringBounds(s, context);   
	        double x = (width - bounds.getWidth()) / 2;   
	        double y = (height - bounds.getHeight()) / 2;   
	        double ascent = -bounds.getY();   
	        double baseY = y + ascent;   
	           
	        g2.drawString(s, (int)x, (int)baseY);   
	        ImageIO.write(bi, "jpg", file); 
	        
	        mocksControl.replay(); 
	        upload.processUploadFile(mockRequest, filePath, "111.jpg");
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
	    
	    _TestUtil.printEntity(super.permissionHelper, "Attachment"); 
    }
    
    protected void deleteSite(Long siteId) {
        channelService.deleteChannel(siteId);
        try {
        	channelService.getChannelById( siteId );
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("读取不到则抛出异常", true);
        }
        
        String hql = "from Article a where a.channel.site.id=? ";
        List<?> articles = channelDao.getEntities(hql, siteId);
        assertTrue(articles.isEmpty());
    }
    
    protected List<?> getArticlesByChannel(Long channelId) {
        String hql = "from Article a where a.channel.id = ? ";
        return channelDao.getEntities(hql, channelId);
    }

}
