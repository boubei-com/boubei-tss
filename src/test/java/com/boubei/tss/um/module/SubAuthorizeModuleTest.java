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

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.RoleAction;
import com.boubei.tss.um.action.SubAuthorizeAction;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.um.service.ISubAuthorizeService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.DateUtil;

/**
 * 角色转授策略相关模块的单元测试
 */
public class SubAuthorizeModuleTest extends AbstractTest4UM {
    
	@Autowired SubAuthorizeAction action;
	@Autowired RoleAction roleAction;
    
    @Autowired ISubAuthorizeService service;
    @Autowired IRoleService roleService;
    @Autowired IUserService userService;
    
    @Test
    public void testCRUD() {
        // 新建一个用户组
        Group mainGroup = new Group();
        mainGroup.setParentId(UMConstants.MAIN_GROUP_ID);
        mainGroup.setName("R_财务部");
        mainGroup.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(mainGroup , "", "");
        log.debug(mainGroup);
        Long mainGroupId = mainGroup.getId();
        
        // 新建一个用户组子组
        Group childGroup = new Group();
		childGroup.setParentId(mainGroupId);
        childGroup.setName("R_财务一部");
        childGroup.setGroupType( mainGroup.getGroupType() );
        groupService.createNewGroup(childGroup , "", "");
        log.debug(childGroup);
        
        // 管理员直接在主组下新增用户
        User boss = new User();
        boss.setLoginName("R_JonKing");
        boss.setUserName("R_JK");
        boss.setPassword("123456");
        boss.setGroupId(mainGroupId);
        userService.createOrUpdateUser(boss , "" + mainGroupId, "");
        
        User user1 = new User();
        user1.setLoginName("Tom");
        user1.setUserName("Tom");
        user1.setPassword("123456");
        user1.setGroupId(mainGroupId);
        userService.createOrUpdateUser(user1 , "" + mainGroupId, "");
 
        // 新建角色
        Role role = new Role();
        role.setIsGroup(0);
        role.setName("总经理");
        role.setParentId(UMConstants.ROLE_ROOT_ID);
        role.setStartDate(new Date());
        role.setEndDate( DateUtil.addYears(50) );
        request.addParameter("Role2UserIds", boss.getId() + "");
        roleAction.saveRole(response, request, role);
        Long roleId = role.getId();
        
        login(boss); // 登录boss，看其权限
        printUserRoleMapping(boss.getId(), 2); // 匿名角色 + 总经理
        
        login(user1); // 登录user1，看其权限
        printUserRoleMapping(user1.getId(), 1); // 只有匿名角色
        
        // 开始测试转授策略模块的功能
        action.getSubauthInfo(response, UMConstants.DEFAULT_NEW_ID);
 
        SubAuthorize strategy = new SubAuthorize();
        strategy.setStartDate(new Date());
        strategy.setEndDate( DateUtil.addYears(50) );
        strategy.setName("转授策略一");
        strategy.setDescription("unit test");
        strategy.setBuyerId(-1L);
        strategy.setOwnerOrg(null);
        strategy.getOwnerOrg();
        
        action.saveSubauth(response, request, strategy);
        
        request.addParameter("Rule2UserIds", user1.getId() + "");
        request.addParameter("Rule2GroupIds", mainGroupId + "," + childGroup.getId());
        request.addParameter("Rule2RoleIds", roleId + "");
        
        try {
        	action.saveSubauth(response, request, strategy);
        	Assert.fail();
        } catch(Exception e) {
        	Assert.assertEquals(EX.U_48, e.getMessage()); // 权限不足
        }
        
        login(boss);
        
        try {
        	action.delete(response, strategy.getId());
        	Assert.fail();
        } catch(Exception e) {
        	Assert.assertEquals(EX.U_48, e.getMessage()); // 权限不足
        }
        
        action.saveSubauth(response, request, strategy);
        action.saveSubauth(response, request, strategy);
        strategy.setId((Long) strategy.getPK());
        
        Long strategyId = strategy.getId();
        action.getSubauthInfo(response, strategyId);
        
        login(user1.getId(), user1.getLoginName()); // 更好登录用户，看其权限
        printUserRoleMapping(user1.getId(), 2); // 匿名角色 + 转授所得角色
        
        action.disable(response, strategyId, 1);
        action.getSubauth2Tree(response);
        
        login(user1.getId(), user1.getLoginName()); // 更好登录用户，看其权限
        printUserRoleMapping(user1.getId(), 1); // 匿名角色 （转授策略停用了）
        
        action.disable(response, strategyId, 0);
        strategy = (SubAuthorize) commonDao.getEntity(SubAuthorize.class, strategyId);
        strategy.setEndDate( DateUtil.addDays(new Date(), 1) );
        commonDao.update(strategy);
        
        login(user1.getId(), user1.getLoginName()); // 更好登录用户，看其权限
        printUserRoleMapping(user1.getId(), 2); // 匿名角色 + 转授所得角色（转授策略重新启用）
        
        // 测试用户(组)失去某角色后，转授出去的是否也被顺利收回
        String role2UserIds = UMConstants.ADMIN_USER_ID + "," + user1.getId();
        String role2GroupIds = "" + mainGroup.getId();
        roleService.saveRole2UserAndRole2Group(role, role2UserIds, role2GroupIds);

        role = roleService.getRoleById(role.getId());
        roleService.saveRole2UserAndRole2Group(role, "", role2GroupIds); // 先从用户上去掉该角色，此时用户通过继承组的角色继续拥有该角色
        roleService.saveRole2UserAndRole2Group(role, role2UserIds, "");  // 从用户组上去掉该角色，单独授回用户该角色
        roleService.saveRole2UserAndRole2Group(role, "", "");
        
        // 测试删除、策略树读取
        action.getSubauth2Tree(response);
        
        action.delete(response, strategyId);
        
        _TestUtil.printEntity(super.permissionHelper, "RoleGroup");
        _TestUtil.printEntity(super.permissionHelper, "RoleUser");
        
        login(user1.getId(), user1.getLoginName()); // 更好登录用户，看其权限
        printUserRoleMapping(user1.getId(), 1); // 匿名角色 （转授策略删除了）
        
        action.getSubauth2Tree(response);
       
    }
    
    protected void printUserRoleMapping(Long userId, int count) {
        List<?> list = permissionHelper.getEntities("from RoleUserMapping where userId=?1", userId);
        assertEquals(count, list.size());
        
        log.debug("表【RoleUserMapping】的所有记录:");
        for(Object temp : list) {
            log.debug(temp);
        }
        log.debug("\n");
    }
}
