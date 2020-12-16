/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.module;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.portal.AbstractTest4Portal;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.action.FileAction;
import com.boubei.tss.portal.helper.MovePortalFile;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

/**
 * 管理门户、页面、布局器、修饰器、布局器等的附件资源的模块的单元测试。
 */
public class PortalFileOperationTest extends AbstractTest4Portal {
    
	@Autowired FileAction fileAction;
 
	@Test
    public void test() {
        String modelDir = URLUtil.getWebFileUrl(PortalConstants.MODEL_DIR).getPath();
        FileHelper.createDir(modelDir);
        
        File testFile = new File(modelDir + "/layout/layout12/test.txt");
        FileHelper.writeFile(testFile, "Just Test!");
        
        request = new MockHttpServletRequest();
        request.addParameter("type", "layout");
        request.addParameter("code", "layout12");
        request.addParameter("filter", "txt");
        fileAction.listAvailableFiles(response, request);
        
        String layoutPath = PortalConstants.MODEL_DIR + "layout/layout12";
        
        request = new MockHttpServletRequest();
		request.addParameter("contextPath", layoutPath);
    	request.addParameter("fileNames", "test.txt");
    	request.addParameter("folderNames", "1,2,3");
        fileAction.download(response, request);
        
        File testFile2 = new File(modelDir + "/layout/xxx.txt");
        FileHelper.writeFile(testFile2, "Just Test 222222!");
        
        super.uploadFile(layoutPath, testFile2);
        
        request = new MockHttpServletRequest();
    	request.addParameter("contextPath", layoutPath);
    	request.addParameter("newFileName", "temp");
    	fileAction.addDir(response, request);
    	
    	request = new MockHttpServletRequest();
    	request.addParameter("contextPath", layoutPath);
    	request.addParameter("fileName", "temp");
    	String newFileName = "D" + System.currentTimeMillis();
    	request.addParameter("newFileName", newFileName);
        fileAction.renameFile(response, request);
		try {
			fileAction.renameFile(response, request);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("同名文件(夹)已经存在，重命名失败！", true);
		}
        
        request = new MockHttpServletRequest();
        request.addParameter("type", "layout");
        request.addParameter("code", "layout12");
        fileAction.listAvailableFiles(response, request);
        
        request = new MockHttpServletRequest();
        request.addParameter("contextPath", layoutPath);
        fileAction.listAvailableFiles(response, request);
        
        request = new MockHttpServletRequest();
        request.addParameter("type", "wrongType");
        fileAction.listAvailableFiles(response, request);
        
        request = new MockHttpServletRequest();
        request.addParameter("contextPath", layoutPath);
    	request.addParameter("fileNames", "xxx.txt");
    	request.addParameter("folderNames", newFileName);
    	fileAction.deleteFile(response, request);
    	
    	Context.setResponse(response = new MockHttpServletResponse());
    	request.removeParameter("fileNames");
    	request.removeParameter("folderNames");
    	request.addParameter("fileNames", "test.txt");
    	request.addParameter("folderNames", "");
        fileAction.download(response, request);
        fileAction.deleteFile(response, request);
        
        Context.setResponse(response = new MockHttpServletResponse());
        request.removeParameter("fileNames");
    	request.removeParameter("folderNames");
    	request.addParameter("fileNames", "");
    	request.addParameter("folderNames", "1,2,3");
    	try {
    		 fileAction.download(response, request);
    	} catch(Exception e) {}
        fileAction.deleteFile(response, request);
    }
	
	@Test
    public void test2() {
        request = new MockHttpServletRequest();
        request.addParameter("type", "reportTL");
        request.addParameter("filter", "html");
        fileAction.listAvailableFiles(response, request);
        
        request.removeParameter("filter");
        request.addParameter("filter", "*");
        fileAction.listAvailableFiles(response, request);
        
        request.addParameter("code", "noExsits");
        fileAction.listAvailableFiles(response, request);
        
        request.removeParameter("contextPath");
        try {
			new MovePortalFile().processUploadFile(request, null, null);
		} catch (Exception e) {
		}
	}
}
