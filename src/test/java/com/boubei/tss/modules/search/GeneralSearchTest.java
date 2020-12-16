/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.search;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.RoleAction;
import com.boubei.tss.um.action.SubAuthorizeAction;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.DateUtil;

import junit.framework.Assert;

/**
 * 授权信息相关搜索测试
 */
public class GeneralSearchTest extends AbstractTest4UM {
	
    @Autowired RoleAction roleAction;
    @Autowired SubAuthorizeAction strategyAction;
    @Autowired IUserService userService;
 
    @Test
    public void testGeneralSearch() {
    	// 新建一个用户组
        Group mainGroup = new Group();
        mainGroup.setParentId(UMConstants.MAIN_GROUP_ID);
        mainGroup.setName("G_财务部");
        mainGroup.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(mainGroup , "", "");
        log.debug(mainGroup);
        Long groupId = mainGroup.getId();
        
        // 管理员直接在主组下新增用户
        User user = new User();
        user.setLoginName("G_JonKing");
        user.setUserName("G_JK");
        user.setPassword("123456");
		user.setGroupId(groupId);
        userService.createOrUpdateUser( user , "" + groupId, "");
        log.debug(user);
 
        // 新建角色
        Role role = new Role();
        role.setIsGroup(0);
        role.setName("G_办公室助理");
        role.setParentId(UMConstants.ROLE_ROOT_ID);
        role.setStartDate(new Date());
        role.setEndDate( DateUtil.addYears(UMConstants.ROLE_LIFE_TIME) );
        
        request.addParameter("Role2UserIds", UMConstants.ADMIN_USER_ID + "," + user.getId());
        request.addParameter("Role2GroupIds", "" + groupId);
        roleAction.saveRole(response, request, role);
        Long roleId = role.getId();
         
        // 新建转授策略
        SubAuthorize strategy = new SubAuthorize();
        strategy.setStartDate(new Date());
        strategy.setEndDate( DateUtil.addDays(UMConstants.STRATEGY_LIFE_TIME) );
        strategy.setName("G_转授策略一");
        
        request.addParameter("Rule2UserIds", user.getId() + "");
        request.addParameter("Rule2GroupIds", groupId + "");
        request.addParameter("Rule2RoleIds", roleId + "");
        strategyAction.saveSubauth(response, request, strategy);
        
        generalSearcher.searchUserSubauth(response, groupId);
    	
        generalSearcher.searchRolesByGroup(response, roleId);
    	
        generalSearcher.searchUsersByRole(response, roleId);
    	
    	List<?> result = generalSearcher.getVisiableRoles();
    	Assert.assertTrue(result.size() > 0);
    	Assert.assertTrue(api.getVisiableRoles().size() > 0);
    	
    	result = generalSearcher.getEditableRoles();
    	Assert.assertTrue(result.size() > 0);
    	Assert.assertTrue(api.getVisiableRoles().size() > 0);
    	
    	generalSearcher.getVisiableRolesTree(response);
    	
    	generalSearcher.getUsersByGroupId(groupId);
    	generalSearcher.getUsersByDomain("BD", "loginName");
    	generalSearcher.getDomainGroups();
    	generalSearcher.getUsersByRoleId(roleId);
    	
    	Assert.assertEquals(role.getId(), generalSearcher.getRoleId(role.getName()));
    	Assert.assertNull(generalSearcher.getRoleId( "not exists" ));
    	
    	List<?> users1 = generalSearcher.getDomainUsersByRole(role.getName());
    	List<?> users2 = generalSearcher.getDomainUsersByRole(role.getId()+",2");
    	Assert.assertEquals(users1.size(), users2.size());
    	
    	super.logout();
    	generalSearcher.getUsersByDomain("BD", "loginName");
    	
    }
}
