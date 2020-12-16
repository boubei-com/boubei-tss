/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.module;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.UserLog;
import com.boubei.tss.util.EasyUtils;

/**
 * 角色相关模块的单元测试
 */
public class UMDaoTest extends AbstractTest4UM {
    
    
    @Autowired IRoleDao roleDao;
    
    @Test
    public void test1() {
    	Long groupId = 1L, roleId = 1L, userId = 1L;
    	
    	GroupUser gu = new GroupUser();
    	gu.setGroupId(groupId);
    	gu.setUserId(userId);
    	roleDao.createObject(gu);
    	
		roleDao.deleteGroupSubAuthorizeInfo(groupId , roleId);
		
		UserLog ulog = new UserLog();
		ulog.setId(null);
		EasyUtils.obj2Json(ulog);
    }
}
