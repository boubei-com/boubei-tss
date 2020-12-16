/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.servlet;

import javax.servlet.http.HttpServletRequest;

public class MyAfterUpload implements AfterUpload {

	public String processUploadFile(HttpServletRequest request, 
			String filepath, String oldfileName) throws Exception {
		
         return "parent.alert(\"导入成功！\");parent.loadInitData();";
	}
}