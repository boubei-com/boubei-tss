/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.file;

import java.io.File;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.boubei.tss.PX;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.Imager;
import com.boubei.tss.util.StringUtil;

public class CreateAttach implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	RecordService recordService = (RecordService) Global.getBean("RecordService");
	
	public static int getAttachType(String filepath) {
		int type;
		if( FileHelper.isImage(filepath) ) {
			// 对超过MAX_PIC_SIZE(默认1M)的图片进行压缩
			try { 
				int maxPicSize = Integer.parseInt( ParamManager.getValue(PX.MAX_PIC_SIZE, "1024") );
				Imager.zoomImage(filepath, maxPicSize);
				Imager.zoomImage(filepath, maxPicSize); // 初次压缩后还是大于maxPicSize则再压缩一次
			} 
			catch (Exception e) { }
			
			type = RecordAttach.ATTACH_TYPE_PIC;
		} else {
			type = RecordAttach.ATTACH_TYPE_DOC;
		}
		return type;
	}
	
	public static String getOrignFileName(String orignFileName) {
		int separatorIndex = Math.max(orignFileName.lastIndexOf("\\"), orignFileName.lastIndexOf("/"));
		if( separatorIndex >= 0) {
			orignFileName = orignFileName.substring(separatorIndex + 1);
		}
		return orignFileName.substring(Math.max(0, orignFileName.length() - 25));
	}

	public String processUploadFile(HttpServletRequest request,
			String filepath, String orignFileName) throws Exception {

		String record = request.getParameter("recordId");
		record = (String) EasyUtils.checkNull(record, request.getParameter("record"));
		Long recordId = null;
    	try { // 先假定是数据表ID（Long型）
    		recordId = Long.valueOf(record.toString());
    	} 
    	catch(Exception e) { // 按名字或表名再查一遍
    		recordId = recordService.getRecordID(record, Report.TYPE1);
    	}
		
    	File targetFile = new File(filepath);
    	int type = getAttachType(filepath);
    	orignFileName = getOrignFileName(orignFileName);
    	RecordAttach attach = null;
    	
		String[] itemIds = StringUtil.split(request.getParameter("itemId"));
		for( String _itemId : itemIds) {
			Long itemId = Long.parseLong(_itemId);
			attach = saveAttach(targetFile, recordId, itemId, type, orignFileName); // 保存附件信息
			
			// 直接上传到指定字段
			String uploadField = request.getParameter("uploadField");
			if( uploadField != null ) {
				_Database db = recordService._getDB(recordId);
				Map<String, Object> item = db.get(itemId);
				String newVal = EasyUtils.obj2String(item.get(uploadField)) +","+ attach.getName() +"#"+ attach.getId();
				db.updateBatch( itemId.toString(), uploadField, newVal, false );
			}
		}
		targetFile.delete();

		/* 
		 * 向前台返回成功信息。
		 * 因为上传附件都是通过一个隐藏的iframe来实现上传的（可防止刷新主页面），所以上传成功回调JS需要加上 parent. 
	    */
		String refreshGrid = request.getParameter("refreshGrid"); // 上传完成后，刷新 TSS Grid
		if( refreshGrid != null) {
			return "parent.addAttach(" + attach.getId() + ", " + attach.getType() + ", '" 
					+ attach.getName() + "', '" + attach.getDownloadUrl() + "', '" + attach.getUploadUser() + "');";
		}
		else {
			return attach.getId().toString();  // 其它地方上传只需返回附件记录ID即可
		}
	}
	
	// TODO 检测用户对记录是否有编辑权限
	private RecordAttach saveAttach(File file, Long recordId, Long itemId, int type, String oldfileName) {
        String attachDir = RecordAttach.getAttachDir(recordId, itemId);
        File rootDir = new File(attachDir);
        long fileSize = file.length();
        
        // 将附件从上传临时目录剪切到站点指定的附件目录里
        String filePath = FileHelper.copyFile(rootDir, file, true, false);
		String fileName = new File( filePath ).getName();
		String fileSuffix = FileHelper.getFileSuffix(fileName);
		
		// 保存附件信息对象
		RecordAttach attach = new RecordAttach();
		attach.setId(null);
		attach.setType(type);
		attach.setName(oldfileName);
		attach.setRecordId(recordId);
		attach.setItemId(itemId);
		attach.setSeqNo(recordService.getAttachSeqNo(recordId, itemId));
		attach.setUploadDate(new Date());
		attach.setUploadUser(Environment.getUserName());
		attach.setOrigin(Environment.getOrigin());
        attach.setFileName(fileName);
        attach.setFileExt(fileSuffix.toLowerCase());
        attach.setFileSize( fileSize / 1024 );
		
        recordService.createAttach(attach);

		return attach;
	}
}