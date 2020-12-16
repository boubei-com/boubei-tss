/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.service.IUserService;

public class FetchPermissionAfterLoginTest extends AbstractTest4UM {
	
	@Autowired protected IUserService userService;

	@Test
	public void testExcute() {
		HttpSession session = request.getSession();

		session.setAttribute("LOGIN_MSG", "Login TEST");

		new FetchPermissionAfterLogin().execute();

		List<?> roleIds = (List<?>) session.getAttribute(SSOConstants.USER_RIGHTS_L);
		assertTrue(roleIds.size() > 0);
		
		assertTrue( Environment.getOwnRoles().size() > 0 );
		
		assertTrue( !Environment.isDomainAdmin() );
		assertTrue( !Environment.isDeveloper() );
		
		assertTrue( PermissionHelper.checkRole("-1,-10000") );
		assertTrue( PermissionHelper.checkRole("-12,-10000") );
		assertTrue( PermissionHelper.checkRole("-1,-10001") );
		assertTrue( PermissionHelper.checkRole("-1") );
		assertTrue( PermissionHelper.checkRole("") );
		assertTrue( PermissionHelper.checkRole(null) );
		Assert.assertFalse( PermissionHelper.checkRole("12,13") );
		Assert.assertFalse( PermissionHelper.checkRole("12") );

		Object loginName = session.getAttribute(SSOConstants.USER_ACCOUNT);
		assertNotNull(loginName);
		log.debug(loginName);
		
		//-------------------------------------------- 测试新建用户的登陆 -------------------------------------
		// 新建一个用户组
        Group mainGroup = new Group();
        mainGroup.setParentId(UMConstants.MAIN_GROUP_ID);
        mainGroup.setName("R_财务部");
        mainGroup.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(mainGroup , "", "");
        log.debug(mainGroup);
        Long mainGroupId = mainGroup.getId();
        
        // 管理员直接在主组下新增用户
        User mainUser = new User();
        mainUser.setLoginName("R_JonKing");
        mainUser.setUserName("R_JK");
        mainUser.setPassword("123456");
        mainUser.setGroupId(mainGroupId);
        userService.createOrUpdateUser(mainUser , "" + mainGroupId, "");
        log.debug(mainUser);
        
        login(mainUser.getId(), mainUser.getLoginName()); // 更好登录用户，看其权限
        new FetchPermissionAfterLogin().execute();

		int groupLevel = (Integer) session.getAttribute("GROUP_LEVEL");
		assertTrue( groupLevel == 1 );
		
		String lastGroupName =  (String) session.getAttribute(SSOConstants.USER_GROUP);
		assertTrue( mainGroup.getName().equals(lastGroupName) );
		
		Assert.assertNull( session.getAttribute("USERS_OF_DOMAIN") );
		
		// test group domain, need refresh cache
		mainGroup.setDomain("boubei");
		commonDao.update(mainGroup);
		
		CacheHelper.flushCache(CacheLife.SHORT.toString(), "ByUserId(" +mainUser.getId()+ ")");
		login(mainUser.getId(), mainUser.getLoginName()); // 更好登录用户，看其权限
        new FetchPermissionAfterLogin().execute();
        
		Assert.assertEquals("boubei", session.getAttribute("DOMAIN"));
		Assert.assertEquals("'R_JonKing'", session.getAttribute("USERS_OF_DOMAIN"));
		Assert.assertEquals(mainUser.getId().toString(), session.getAttribute("USERIDS_OF_DOMAIN"));
	}

}
