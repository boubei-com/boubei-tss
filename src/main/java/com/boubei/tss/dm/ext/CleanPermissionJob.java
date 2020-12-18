/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ext;

import java.io.File;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.timer.AbstractJob;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.FileHelper;

/**
 * 清理垃圾权限信息
 * 
 * com.boubei.tss.dm.ext.CleanPermissionJob | 0 0 01 * * ? | X
 * 
 */
public class CleanPermissionJob extends AbstractJob {
	
	IUserService userService = (IUserService) Global.getBean("UserService");
	
	protected boolean needSuccessLog() {
		return true;
	}
 
	protected String excuteJob(String jobConfig, Long jobID) {
 
		log.info("------------------- 清理权限信息......");
		
		String[] resources = "role,group,report,record,navigator".split(",");
		String[] permissionTables = "um_permission_role,um_permission_group,dm_permission_report,dm_permission_record,portal_permission_navigator".split(",");
		
		int index = 0;
		for(String resource : resources ) {
			String permissionTable = permissionTables[index++];
			
			// 1.清理资源已经不存在的权限信息
			String sql = "delete from " +permissionTable+ " where id > 0 and (roleId not in (select id from um_role) or resourceId not in (select id from view_" +resource+ "_resource) )";
			SQLExcutor.excute(sql, DMConstants.LOCAL_CONN_POOL);
			
			// 2.清理重复生成的权限信息
			sql = "delete from " +permissionTable+ " where id not in (select id from (SELECT min(t.id) id FROM " +permissionTable+ " t group by t.resourceId, t.roleId, t.operationId, t.permissionState, t.isPass, t.isGrant) t)";
			SQLExcutor.excute(sql, DMConstants.LOCAL_CONN_POOL);
		}
		
		log.info("------------------- 清理权限信息 Done");
		
		// 处理过期的用户、角色、转授策略等
		userService.overdue();
		
		
		// 清除 um_roleusermapping
		SQLExcutor.excute("delete from um_roleusermapping where userId > -10000 or roleId > -10000", DMConstants.LOCAL_CONN_POOL);
		
		// 清除临时表残留数据
		SQLExcutor.excute("truncate table TBL_TEMP_", DMConstants.LOCAL_CONN_POOL);
		
		// 清除残留在export 和 upload 里的附件
		FileHelper.deleteFilesInDir("", new File(DMUtil.getAttachPath() + "/export") );
//		FileHelper.deleteFilesInDir("", new File(DMUtil.getAttachPath() + "/upload") ); // Excel导入文件需要保留
		
		// TODO 循环所有域，如果一个域管理员的策略过期了，则需要把 域管理员 授权出去的账号 的角色一并收回来
		
		return EX.DEFAULT_SUCCESS_MSG;
	}
	 
}
