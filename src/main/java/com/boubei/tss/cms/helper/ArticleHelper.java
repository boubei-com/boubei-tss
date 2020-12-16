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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

import org.dom4j.Element;

import com.boubei.tss.EX;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Attachment;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

public class ArticleHelper {
    
    /**
     * 计算文章的过期时间
     */
    public static Date calculateOverDate(Channel channel){
        //如果 栏目上没有过期时间设置，则不需要再给文章设置过期时间
        String overdueDate = channel.getOverdueDate();
		if( EasyUtils.isNullOrEmpty(overdueDate) ){
			overdueDate = "0";
        }
        
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        switch (overdueDate.charAt(0)) {
        case '0':
            calendar.add(Calendar.YEAR, 100);
            break;
        case '1':
        	calendar.add(Calendar.YEAR, 1);
            break;
        case '2':
            calendar.add(Calendar.MONTH, 3);
            break;
        case '3':
            calendar.add(Calendar.MONTH, 1);
            break;
        case '4':
            calendar.add(Calendar.WEEK_OF_MONTH, 1);
            break;
        case '5':
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            break;
        default:
            break;
        }
        return calendar.getTime();
    }
    
	/**
	 * <p>
	 * 获取文章发布路径 (根据创建时间确定发布路径)
	 * 发布路径 = 站点发布目录/年/月/日
	 * 
     * 格式： 2008/8/20
	 * </p>
	 * @param createTime
	 * @return
	 */
	private static String getArticlePublishPath(Date createTime) {
	    if(createTime == null) {
	        createTime = new Date();
	    }
        return DateUtil.format(createTime, "yyyy/MM/dd");
	}
	
	public static String getArticlePublishPath(Article article) {
	    return getArticlePublishPath(article.getCreateTime());
	}

	/**
	 * <p>
	 * 得到图片和文档附件的路径
	 * </p>
	 * @param site
	 * @return
	 */
    public static String getAttachmentPath(Channel site, Integer attanchmentType) {
        String articlePath = getArticlePublishPath(new Date());
        
        if (Attachment.isImage(attanchmentType)) {
            return site.getPath() + "/" + site.getImagePath() + "/" + articlePath; // image
        }
        if (Attachment.isOfficeDoc(attanchmentType)) {
            return site.getPath() + "/" + site.getDocPath() +  "/" + articlePath; // office doc
        }
        throw new BusinessException(EX.CMS_3);
    }
    
    /**
     * 得到图片,附件路径及下载路径
     * String[]{E:/cms/hzjh/docPath/2008/07/18/1216386988375.txt,
     *          http://localhost:8088/cms/download?id=1216&amp;seqNo=1}
     */
    public static String[] getAttachUploadPath(Channel site, Attachment attachment) {
        String filePath = site.getPath() + "/" + site.getAttanchmentPath(attachment) + "/" + attachment.getLocalPath();
        return new String[]{ filePath, attachment.getDownloadUrl() };
    }
    
    /**
     * 获取系统的默认编码。
     * 如果既不是"UTF-8"又不是"GBK"，则默认为"UTF-8"（防止"ISO-88591"等编码时发布的XML文件被前台读取时会出错）
     */
    public static String getSystemEncoding(){
        String encoding = System.getProperty("file.encoding");
        if( !"GBK".equalsIgnoreCase(encoding) ){
        	encoding = "UTF-8";
        }
        return encoding;
    }
    
    /**
     * 在XML文件中加入附件信息，加入到指定的Element节点下。
     * @param itemElement
     * @param attchments
     */
    public static void addPicListInfo(Element itemElement, Collection<Attachment> attchments){
        for ( Attachment attachment : attchments ) {
        	 // 在指定节点下为每个附件增加一个Attachment节点
            Element attachmenetElement = itemElement.addElement("Attachment");
            
            attachmenetElement.addAttribute("type", attachment.isImage() ? "image" : "file");
            attachmenetElement.addElement("name").addCDATA(attachment.getName());
            attachmenetElement.addElement("url").addCDATA(attachment.getDownloadUrl());
            attachmenetElement.addElement("rurl").addCDATA(attachment.getRelationUrl());
        }
    }
    
    /**
     * 根据查询结果返回的数组重新组建成Article对象
     * @param articleInfos
     * @return
     */
    public static Article createArticle(Object[] articleInfos){
        Article article = new Article();
        
        int index = 0;
        article.setId((Long) articleInfos[index++]);
        article.setTitle((String) articleInfos[index++]);
        article.setAuthor((String) articleInfos[index++]);
        article.setIssueDate((Date) articleInfos[index++]);
        article.setSummary((String) articleInfos[index++]);
        article.setHitCount((Integer) articleInfos[index++]);
        article.setCreatorName((String) articleInfos[index++]);
        article.setCreateTime((Date) articleInfos[index++]);
        article.setStatus((Integer) articleInfos[index++]);
        article.setChannel( (Channel)articleInfos[index++] );
        article.setIsTop((Integer) articleInfos[index++]);
        article.setOverdueDate((Date)articleInfos[index++]);
        article.setSeqNo((Integer) articleInfos[index++]);
        article.setCommentNum((Integer) articleInfos[index++]);
        
        return article;
    }
}
