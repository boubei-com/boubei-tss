/* ==================================================================   
 * Created [2006-12-28] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.util;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import com.boubei.tss.modules.param.ParamConfig;

/**
 * 制作图片文件的缩略图
 */
public class Imager {
    
    private String destFilePath;// 如果需要保存目标文件到其他目录时需要
    
    private String srcFile;
    private String destFile;
    
    private Image img;

    private int width;
    private int height;

    /**
     * 构造函数
     * @param filePath
     *            构造函数参数 源文件（图片）的路径
     * @throws IOException
     */
    public Imager(String filePath) throws IOException {
        File _file = new File(filePath); // 读入文件
        destFilePath = _file.getParent();
        srcFile = _file.getName();
        
        // 生成文件命名为原文件名 + "_s"
        destFile = srcFile.substring(0, srcFile.lastIndexOf(".")) + "_s.jpg"; 
        
        // 构造Image对象
        img = javax.imageio.ImageIO.read(_file); 
       
        width  = img.getWidth(null);  // 得到源图宽
        height = img.getHeight(null); // 得到源图长
    }

    /**
     * 强制压缩/放大图片到固定的大小
     * 
     * @param w
     *            int 新宽度
     * @param h
     *            int 新高度
     * @throws IOException
     */
    public String resize(int w, int h) throws IOException {
        BufferedImage _image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        _image.getGraphics().drawImage(img, 0, 0, w, h, null); // 绘制缩小后的图

        String target = destFilePath + "/" + destFile;
        
        FileOutputStream out = new FileOutputStream(target); // 输出到文件流
        ImageIO.write(_image, "JPEG", out);
        out.close();
        
        return target;
    }

    /**
     * 按照固定的比例缩放图片
     * 
     * @param t
     *            double 比例
     * @throws IOException
     */
    public String resize(double t) throws IOException {
        int w = (int) (width * t);
        int h = (int) (height * t);
        return resize(w, h);
    }
    
    /*
     * 图片按比率缩放
     * maxPicSize为文件大小上限, 单位 K
     */
	public static void zoomImage(String src, Integer maxPicSize) throws Exception {
		File srcFile = new File(src);
		long fileSize = srcFile.length();
		String subfix = FileHelper.getFileSuffix(srcFile.getName());
		String zoomableTypes = ParamConfig.getAttribute("ZOOM_PIC_TYPES", "jpg,jpeg,bmp,gif,png"); // 这些格式支持有损压缩，png等不支持
		List<String> list = Arrays.asList( zoomableTypes.split(",") ); 
		
		if (fileSize <= maxPicSize * 1024 || !list.contains(subfix.toLowerCase()))  { // 文件本身已小于size（K）时，不做缩放
			return;
		}
			
		Double rate = (maxPicSize * 1024.0) / fileSize; // 获取长宽缩放比例
		rate = Math.max(rate, 0.5);

		BufferedImage bufImg = ImageIO.read(srcFile);
		Image Itemp = bufImg.getScaledInstance(bufImg.getWidth(), bufImg.getHeight(), Image.SCALE_SMOOTH);

		AffineTransformOp ato = new AffineTransformOp(AffineTransform.getScaleInstance(rate, rate), null);
		Itemp = ato.filter(bufImg, null);
		
		ImageIO.write((BufferedImage) Itemp, subfix, srcFile);
	}
	
	public static String markSLPic(File picFile, int size) {
		String picPath = picFile.getPath();
		if( FileHelper.isImage(picPath) ) {
			String slPicDir = picFile.getParent() + "/sl";
			
			// 检查缩略图是否已存在
			String slPicPath = slPicDir + "/" + picFile.getName();
			if( new File(slPicPath).exists() ) {
				return slPicPath;
			}
			
			try {
				slPicPath = FileHelper.copyFile( new File(slPicDir), picFile, true, false);
				Imager.zoomImage(slPicPath, size); // 缩略图大小，不超过size
				Imager.zoomImage(slPicPath, size);
				return slPicPath;
			} 
			catch (Exception e) {
			}
		}
		return picPath; // 如果缩略失败，则还是采用原图片
	}
}
