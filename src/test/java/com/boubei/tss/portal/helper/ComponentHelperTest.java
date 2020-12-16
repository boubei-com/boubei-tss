/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.helper;

import java.io.File;
import java.net.URL;

import org.junit.Test;

import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

import junit.framework.Assert;

public class ComponentHelperTest {
	
	@Test
	public void test() {
		Assert.assertEquals("<layout/>", ComponentHelper.getComponentConfig("layout", null) );
		
		URL url = URLUtil.getResourceFileUrl("log4j.properties");
        String log4jPath = url.getPath(); 
        String classDir = new File(log4jPath).getParentFile().getPath();
        File file = new File(classDir + "/1.xml");
        FileHelper.writeFile(file, "<layout/>");
        
		Component component = new Component();
		component.setType(3); // portlet
		
    	try {
    		ComponentHelper.importComponent(null, file, component , classDir, "porlet.xml");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("导入XML文件不是规范的门户组件，根节点名称不匹配！", true);
		}
    	
    	File file2 = new File(classDir + "/2.json");
        FileHelper.writeFile(file2, "[]");
    	ComponentHelper.importComponent(null, file2, component , classDir, "porlet.xml");
	}

}
