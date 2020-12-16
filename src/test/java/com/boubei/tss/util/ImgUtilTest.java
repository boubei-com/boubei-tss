/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.cms.AbstractTest4CMS;

public class ImgUtilTest {
	
	File tempDir1;
	
	@Before
	public void setUp() {
		URL url = URLUtil.getResourceFileUrl(AbstractTest4CMS.CK_FILE_PATH);
        String log4jPath = url.getPath(); 
        File classDir = new File(log4jPath).getParentFile();
        Assert.assertTrue(FileHelper.checkFile(classDir, AbstractTest4CMS.CK_FILE_PATH));
        
        tempDir1 = FileHelper.createDir(classDir + "/temp1");
	}
    
	@Test
	public void testResize() {
		try {
			// 先用java创建一张图片
			int width = 1600;   
	        int height = 1000;   
	        String s = "8341";   
	        
	        String filePath = tempDir1 + "/1.jpg";
	        File file = new File(filePath);   
	           
	        Font font = new Font("Serif", Font.ITALIC, 25*30);   
	        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);   
	        Graphics2D g2 = (Graphics2D)bi.getGraphics();   
	        g2.setBackground(Color.WHITE);   
	        g2.clearRect(0, 0, width, height);   
	        g2.setPaint(Color.RED);   
	        g2.setFont(font);
	           
	        FontRenderContext context = g2.getFontRenderContext();   
	        Rectangle2D bounds = font.getStringBounds(s, context);   
	        double x = (width - bounds.getWidth()) / 2.5;   
	        double y = (height - bounds.getHeight()) / 1.2;   
	        double baseY = y - bounds.getY();   
	           
	        g2.drawString(s, (int)x, (int)baseY);   
	        ImageIO.write(bi, "jpg", file);  
	        Imager.zoomImage(file.getPath(), 10);
	        Imager.zoomImage(file.getPath(), 100);
	        
	        File file2 = new File(tempDir1 + "/2.png");
	        ImageIO.write(bi, "png", file2);  
	        Imager.zoomImage(file2.getPath(), 10);
	        Imager.zoomImage(file2.getPath(), 100);
	        
//	        File file3 = new File(tempDir1 + "/11.jpg");
//	        Imager.zoomImage(file3.getPath(), 1024);
	        
//	        File file4 = new File(tempDir1 + "/16.png");
//	        Imager.zoomImage(file4.getPath(), 100);
	        
	        // 测试缩略图1
			Imager ip = new Imager(filePath);
			ip.resize(0.98);
			
			// 测试缩略图2
			Imager.markSLPic(new File(filePath), 5);
			Imager.markSLPic(new File(filePath), 5);
			
			Imager.markSLPic(new File(filePath + "xxx"), 5);
		} 
		catch (Exception e) {
			Assert.assertFalse(e.getMessage(), true);
		}
	}
}
