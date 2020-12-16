/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.dm.record.file.OrignUploadFile;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.ExceptionEncoder;
import com.boubei.tss.framework.exception.convert.ExceptionConvertorFactory;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.filter.Filter8APITokenCheck;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.ProgressPool;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

/**
 * 文件上传的文件载体要么在服务器端具备可执行性，要么具备影响服务器端行为的能力，其发挥作用还需要具备以下几个条件：
 * -1. 上传的文件具备可执行性或能够影响服务器行为，所以文件后所在的目录必须在WEB容器覆盖的路径之内；
 * -2. 用户可以从WEB上访问这个文件，从而使得WEB容器解释执行该文件；
 * -3. 上传后的文件必须经过应用程序的安全检查，以及不会被格式化、压缩等处理改变其内容
 * 
 * 如何安全上传文件:
 * -1. 最有效的，将文件上传目录直接设置为不可执行，对于Linux而言，撤销其目录的'x'权限；
 * -2. 文件类型检查：强烈推荐白名单方式，结合MIME Type、后缀检查等方式；此外对于图片的处理可以使用压缩函数或resize函数，处理图片的同时破坏其包含的HTML代码；
 * -3. 使用随机数改写文件名和文件路径，使得用户不能轻易访问自己上传的文件
 * -4. 单独设置文件服务器的域名
 * 
 * 注：最大可以上传文件大小为10M
 * 
 * 微信小程序等上传附件：
 * var url =  "https://www.boudata.com/tss/remote/upload?afterUploadClass=com.boubei.tss.dm.record.file.CreateAttach&type=2";
	url += "&recordId=" + recordId;  
	url += "&itemId=" + selectedLineId;
	url += "&uName=" + uName;  
	url += "&uToken=" + uToken;
	url += "&client=" + wx;
 */
@WebServlet(urlPatterns={"/auth/file/upload", "/remote/upload"})
@MultipartConfig(maxFileSize = 1024 * 1024 * 10)
public class Servlet4Upload extends HttpServlet {

	private static final long serialVersionUID = -6423431960248248353L;

	Logger log = Logger.getLogger(this.getClass());

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
        String servletPath = request.getServletPath() + "";
        if( servletPath.endsWith("/remote/upload") ) { // 远程（微信小程序、APP等）上传，先校验令牌，通过则进行自动登录
        	Filter8APITokenCheck.checkAPIToken( request );
        }
		
        String script = "";
		try {
			Collection<Part> partList = request.getParts();
	        for(Part part : partList) {
	        	if( part.getContentType() != null ) {
	        		String result = doUpload(request, part); // 自定义输出到指定目录
					script += EasyUtils.checkTrue(result != null && script.indexOf(result) < 0, result, "");
	        	}
	        }
			
		} catch (Exception _e) {
			ProgressPool.finish( new Progress(100) ); // 进度设置为完成
			
			Exception e = ExceptionConvertorFactory.getConvertor().convert(_e);
			ExceptionEncoder.printErrorMessage(_e);
			
			String errorMsg = "异常提示：" + e.getMessage();
			errorMsg = Pattern.compile("\t|\r|\n|\'").matcher(errorMsg).replaceAll(" "); // 剔除换行，以免alert不出来
			script = "parent.alert('" + errorMsg + "');";
		} 
		
		script = (String) EasyUtils.checkNull(script, "console.log('上传完成')");
		
		// 上传文件在一个独立的iframe里执行，完成后，输出一个html到该iframe，以触发提示脚本
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().print("<html><script>" +script+ "</script></html>");
	}
	
	String doUpload(HttpServletRequest request, Part part) throws Exception {
		/* 
		 * gets absolute path of the web application, tomcat7/webapps/tss 
		 * String defaultUploadPath = request.getServletContext().getRealPath("");
		 */
		Map<String,String> params = DMUtil.getRequestMap(request, true);
		String uploadPath = DMUtil.getAttachPath() + File.separator + "upload";
        FileHelper.createDir(uploadPath);
 
		// 获取上传的文件真实名字(含后缀)
		String contentDisp = part.getHeader("content-disposition");
		String orignFileName = "";
		String[] items = contentDisp.split(";");
		for (String item : items) {
			if (item.trim().startsWith("filename")) {
				orignFileName = item.substring(item.indexOf("=") + 2, item.length() - 1);
				break;
			}
		}
		
		String subfix = FileHelper.getFileSuffix(orignFileName), newFileName;
		
		// 允许使用原文件名
		String useOrignName = params.get("useOrignName");
		// 指定文件名
		String useSpecifiedName = params.get("useSpecifiedName");
		if(useOrignName != null) {
			newFileName = orignFileName;
		} else if(useSpecifiedName != null){
			newFileName = useSpecifiedName + "." + subfix;
		} else {
			newFileName = System.currentTimeMillis() + "." + subfix; // 重命名
		}
		
        String newFilePath = uploadPath + File.separator + newFileName;
        long fileSize = part.getSize() / 1024;
        
        // 自定义输出到指定目录
		InputStream is = part.getInputStream();
		FileOutputStream fos = new FileOutputStream(newFilePath);
		int data = is.read();
		while(data != -1) {
		  fos.write(data);
		  data = is.read();
		}
		fos.close();
		is.close();
		
		
		String afterUploadClass = params.get("afterUploadClass");
		String jsCallback = EasyUtils.obj2String( params.get("callback") );
		
		// 导入数据：则记录上传的数据附件信息 & 检查是否重复在导入
		if( Excel.isCSV(newFilePath) || Excel.isXLS(newFilePath) || Excel.isXLSX(newFilePath) ) {
			
			ProgressPool.checkRepeat(); 
			
			OrignUploadFile ogf = new OrignUploadFile();
			ogf.setId((Long) ogf.getPK());
			ogf.setType(subfix);
			ogf.setName(newFileName);
			ogf.setPath(newFilePath);
			ogf.setSize(fileSize);
			ogf.setOld(orignFileName);
			ogf.setOrigin(Environment.getOrigin());
			ogf.setAfter(afterUploadClass);
			Global.getCommonService().create(ogf);
		}
		
		// 执行附件后续逻辑操作
		AfterUpload afterUpload = (AfterUpload) BeanUtil.newInstanceByName(afterUploadClass);
		return afterUpload.processUploadFile(request, newFilePath, orignFileName)
				+ EasyUtils.checkTrue(jsCallback.length() == 0, "", ";" + jsCallback);
	}
}
