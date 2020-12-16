/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.boubei.tss.util.FileHelper;
 
/**
 * 初始化Portal测试的组件模型存放目录（model目录）及freemarker文件的目录
 *
 */
public class InitJettyEnv { 
 
    protected Logger log = Logger.getLogger(this.getClass());    
    
    @Test
    public void intiForJettyRun() {
        
        String portalTargetPath = _TestUtil.getProjectDir() + "/target";
        
        FileHelper.createDir(portalTargetPath + "/portal/model/portal");
        FileHelper.createDir(portalTargetPath + "/portal/model/portlet");
        FileHelper.createDir(portalTargetPath + "/portal/model/layout");
        FileHelper.createDir(portalTargetPath + "/portal/model/decorator");
        
        File freemarkerDir = FileHelper.createDir(portalTargetPath + "/freemarker");
        FileHelper.writeFile(new File(freemarkerDir + "/common.ftl"), "");
    }
    
    public static void main(String[] args) throws Exception{
        Runtime lRuntime = Runtime.getRuntime();
        System.out.println("Free Momery:" + lRuntime.freeMemory()/1024/1024 + "M ");
        System.out.println("Max Momery:" + lRuntime.maxMemory()/1024/1024 + "M");
        System.out.println("Total Momery:" + lRuntime.totalMemory()/1024/1024 + "M");
        System.out.println("Available Processors : " + lRuntime.availableProcessors());
        
        System.out.println( "rp-122".replaceFirst("rp-", "") );
    }
    
    /**
	 * Runtime.getRuntime().totalMemory() ==> byte
	 */
	@Test
	public void test1() {
		int size = 1000000;
		
		System.gc();
		printMemoryCost();

		Object[] data = new Object[size];
		for (int i = 0; i < size; i++) {
			data[i] = Integer.valueOf(i);
		}
		printMemoryCost();
		data = null;
		System.gc();
		
		Object[] data2 = new Object[size];
		for (int i = 0; i < size; i++) {
			data2[i] =  Long.valueOf(i);
		}
		printMemoryCost();
		data2 = null;
		System.gc();
		
		Object[] data3 = new Object[size];
		for (int i = 0; i < size; i++) {
			data3[i] =  String.valueOf(i);
		}
		printMemoryCost();
    }

	private void printMemoryCost() {
		Runtime rt = Runtime.getRuntime();
		long costMemory0 = (rt.totalMemory() - rt.freeMemory()) / (1024*1024);
		System.out.println("memory cost: " + costMemory0 + "M");
	}
}
