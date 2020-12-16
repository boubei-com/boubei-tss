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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.RoleAction;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.RoleGroup;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.entity.permission.GroupPermission;
import com.boubei.tss.um.entity.permission.GroupResource;
import com.boubei.tss.um.entity.permission.ViewRoleUser;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.DateUtil;

/**
 * 角色相关模块的单元测试
 */
public class RoleModuleTest extends AbstractTest4UM {
    
	@Autowired RoleAction action;
    
    @Autowired IRoleService service;
    @Autowired IUserService userService;
    
    Group mainGroup;
    User mainUser;
    Long role1Id;
    Long role2Id;
    Long roleGroupId;
    
    public void init() {

        super.init();
        
        // 新建一个用户组
        mainGroup = new Group();
        mainGroup.setParentId(UMConstants.MAIN_GROUP_ID);
        mainGroup.setName("财务部");
        mainGroup.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(mainGroup , "", "");
        log.debug(mainGroup);
        
        // 新建一个用户组子组
        Group childGroup = new Group();
        childGroup.setParentId(mainGroup.getId());
        childGroup.setName("财务一部");
        childGroup.setGroupType( mainGroup.getGroupType() );
        groupService.createNewGroup(childGroup , "", "");
        log.debug(childGroup);
        
        // 管理员直接在主组下新增用户
        mainUser = new User();
        mainUser.setLoginName("JonKing");
        mainUser.setUserName("JK");
        mainUser.setPassword("123456");
        mainUser.setGroupId(mainGroup.getId());
        userService.createOrUpdateUser(mainUser , "" + mainGroup.getId(), "");
        log.debug(mainUser);
        
        // 新建角色组
        Role roleGroup = new Role();
        roleGroup.setIsGroup(1);
        roleGroup.setName("角色组一");
        roleGroup.setParentId(UMConstants.ROLE_ROOT_ID);
        action.saveRole(response, request, roleGroup);
        roleGroupId = roleGroup.getId();
        roleGroup.setId((Long) roleGroup.getPK());
        
        // 新建角色
        Role role1 = new Role();
        role1.setIsGroup(0);
        role1.setName("办公室助理");
        role1.setParentId(roleGroupId);
        role1.setStartDate(new Date());
        role1.setEndDate( DateUtil.addYears(50) );
        request.addParameter("Role2UserIds", UMConstants.ADMIN_USER_ID + "," + mainUser.getId());
        request.addParameter("Role2GroupIds", "" + mainGroup.getId());
        action.saveRole(response, request, role1);
        
        role1Id = role1.getId();
        
        // 再新建一个角色
        Role role2 = new Role();
        role2.setIsGroup(0);
        role2.setName("部门经理");
        role2.setParentId(roleGroupId);
        role2.setStartDate(new Date());
        role2.setEndDate( DateUtil.addYears(50) );
        role2.setDescription("unit test");
        
        request.addParameter("Role2UserIds", UMConstants.ADMIN_USER_ID + "," + mainUser.getId());
        request.addParameter("Role2GroupIds", "" + mainGroup.getId());
        action.saveRole(response, request, role2);
        role2Id = role2.getId();
        action.getAllRole2Tree(response);
        
        action.disable(response, role2.getId(), ParamConstants.TRUE);
        
        action.sort(response, role1.getId(), role2.getId(), 1);
        
        Role roleGroup2 = new Role();
        roleGroup2.setIsGroup(1);
        roleGroup2.setName("角色组-2");
        roleGroup2.setParentId(UMConstants.ROLE_ROOT_ID);
        roleGroup2.setDisabled(ParamConstants.TRUE);
        action.saveRole(response, request, roleGroup2);
        
        action.getAllRole2Tree(response);
        
        action.disable(response, role2.getId(), ParamConstants.FALSE);
    }
    
    @Test
    public void testGetRole() {
        // 读取修改角色组的模板
    	action.getRoleGroupInfo(response, UMConstants.DEFAULT_NEW_ID, UMConstants.ROLE_ROOT_ID);
        action.getRoleGroupInfo(response, roleGroupId, null);
        
        // 读取新增或修改角色的模板
        action.getRoleInfo(response, UMConstants.DEFAULT_NEW_ID, UMConstants.ROLE_ROOT_ID);
        action.getRoleInfo(response, roleGroupId, null);
    }
    
    @Test
    public void testDisplayRole() {
        // 读取角色树形结构
        action.getAllRole2Tree(response);
        action.getAllRoleGroup2Tree(response);
    }
    
    @Test
    public void testDisableRole() {
        // 停用角色组
        action.disable(response, roleGroupId, ParamConstants.TRUE);
        
        // 启用角色
        action.disable(response, role1Id, ParamConstants.FALSE);
    }
    
    @Test
    public void testRoleMove() {
        // 对角色进行移动
        action.move(response, role2Id, UMConstants.ROLE_ROOT_ID);
        
        action.move(response, role2Id, service.getRoleById(role2Id).getParentId());
        
        try {
        	action.move(response, role2Id, role2Id);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.U_28, e.getMessage());
        }
        
        service.disable(roleGroupId, ParamConstants.TRUE);
        action.move(response, role2Id, roleGroupId);
        
        service.disable(roleGroupId, ParamConstants.FALSE);
        Role roleGroup = service.getRoleById(roleGroupId);
        roleGroup.setDescription("unit test");
        service.saveRoleGroup(roleGroup);
    }
    
    @Test
    public void testRolePermission() {
    	action.getOperation(response, UMConstants.ANONYMOUS_ROLE_ID);
    	action.getOperation(response, role2Id);
    	
        request.addParameter("applicationId", "tss");
        request.addParameter("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
        action.initSetPermission(response, request, 1, role1Id);
        action.initSetPermission(response, request, 2, role1Id);
        action.getApplications(response, role1Id, 1);
        
        action.getResourceTypes(response, "tss");
        
        /* 
         * 授权测试 PermissionRank
         * LOWER_PERMISSION            = "1";  普通授权
         * AUTHORISE_PERMISSION        = "2";  可授权授权
         * PASSON_AUTHORISE_PERMISSION = "3";  可传递授权
         * SUB_AUTHORISE_PERMISSION    = "4";  权限转授
 		 */
        log.debug("====================== 开始测试授权 ============================");
        
    	try {
    		action.getPermissionMatrix(response, request, null, 1, 1L);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals("请选择授权级别", e.getMessage());
		}
    	try {
    		request.removeParameter("applicationId");
    		request.removeParameter("resourceType");
    		action.getPermissionMatrix(response, request, "1", 1, 1L);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals("请选择应用系统", e.getMessage());
		}
    	try {
    		request.addParameter("applicationId", "tss");
    		action.getPermissionMatrix(response, request, "1", 1, 1L);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals("请选择资源类型", e.getMessage());
		}
		
		try {
			resourcePermission.addResource(-99L, UMConstants.GROUP_RESOURCE_TYPE_ID);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("权限补齐时找不到资源对象，请检查资源视图（......GroupResource）中是否包含该对象？是否过滤掉了？", true);
		}
    	
    	request.removeParameter("applicationId");
    	request.addParameter("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
    	action.getPermissionMatrix(response, request, "1", 0, mainGroup.getId());
        
        //一、 多个资源授权给单个角色
        request.addParameter("applicationId", "tss");
        request.addParameter("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
        action.getPermissionMatrix(response, request, "2", 1, role1Id);
        
        // 授权内容, 当多个资源对一个角色授权时:  resource1|2224, resource2|4022
   	    // 竖线后面为各个权限选项的打勾情况【0: 没打勾, 1: 仅此节点，虚勾 2: 此节点及所有子节点，实勾 3:禁用未选中 4:禁用已选中】
        String permissions = mainGroup.getId() + "|22"; // 主用户组全勾
        
        request.addParameter("applicationId", "tss");
        request.addParameter("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
        request.addParameter("permissions", permissions);
        action.savePermission(response, request, "2", 1, role1Id);
        action.getPermissionMatrix(response, request, "2", 1, role1Id);
        
        // 检查资源和权限对象
        _TestUtil.printEntity(super.permissionHelper, GroupPermission.class.getName());
        List<?> list = super.permissionHelper.getEntities("from " + GroupPermission.class.getName());
        for(Object temp : list) {
        	GroupPermission gp = (GroupPermission) temp;
        	Assert.assertEquals(gp.getId(), gp.getPK());
        	Assert.assertNotNull(gp.getResourceName());
        }
        list = super.permissionHelper.getEntities("from " + GroupResource.class.getName());
        for(Object temp : list) {
        	GroupResource gr = (GroupResource) temp;
        	Assert.assertEquals(gr.getAttributes().get("id"), gr.getPK());
        	Assert.assertNotNull(gr.getName());
        	Assert.assertNotNull(gr.getDecode());
        	Assert.assertNotNull(gr.getParentId());
        	Assert.assertEquals(UMConstants.GROUP_RESOURCE_TYPE_ID, gr.getResourceType());
        }
        
        // 二、单个资源授权给多个角色。当资源对角色进行授权时, roleId表示resourceId
        Long resourceId = mainGroup.getId(); // 当资源对角色进行授权时, roleId表示resourceId
        request.addParameter("applicationId", "tss");
        request.addParameter("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
        action.getPermissionMatrix(response, request, "2", 0, resourceId);
        
        // 授权内容, 当单个资源对多个角色授权时:  roleId1|2224, roleId2|4022
   	    // 竖线后面为各个权限选项的打勾情况【0: 没打勾, 1: 仅此节点，虚勾 2: 此节点及所有子节点，实勾 3:禁用未选中 4:禁用已选中】
        request.removeParameter("applicationId");
        request.removeParameter("permissions");
        action.savePermission(response, request, "2", 0, resourceId);
        
        permissions = role2Id + "|22";
        request.addParameter("applicationId", "tss");
        request.addParameter("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
        request.removeParameter("permissions");
        request.addParameter("permissions", permissions);
        action.savePermission(response, request, "2", 0, resourceId);
        action.getPermissionMatrix(response, request, "2", 0, resourceId);
        
        _TestUtil.printEntity(super.permissionHelper, "GroupPermission");
        
        // 界面上操作授权时，每一次都会把之前产生的授权信息也传递回来；而本测试没有第一步资源给角色1的授权信息加上，相当于删除了第一步的授权信息
        
        login(mainUser.getId(), mainUser.getLoginName()); // 更好登录用户，看其权限
        request.addParameter("applicationId", "tss");
        request.addParameter("resourceType", UMConstants.GROUP_RESOURCE_TYPE_ID);
        action.getPermissionMatrix(response, request, "2", 1, role1Id);
        _TestUtil.printEntity(super.permissionHelper, "RoleUserMapping");
        
        printVisibleMainGroups(2); // 主用户组（Main-Group）、 财务部（“财务一部”移到了根目录下，对“财务部”下用户不可见）
        
        // 清除授权
        action.clearPermission(response, request, "2", 0, resourceId);
        action.clearPermission(response, request, "2", 1, role1Id);
        request.removeParameter("applicationId");
        action.clearPermission(response, request, "2", 1, role2Id);
        
        printVisibleMainGroups(0);
        
        login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER); // 换回Admin登录
        
        list = super.permissionHelper.getEntities("from " + ViewRoleUser.class.getName());
        for(Object temp : list) {
        	ViewRoleUser vru = (ViewRoleUser) temp;
        	Assert.assertNotNull(vru.getId().getRoleId());
        	Assert.assertNotNull(vru.getId().getRoleId());
        	Assert.assertTrue(vru.equals(vru));
        	log.debug(vru.getId());
        }
        
        // test other
        List<Long> roleUserList = new ArrayList<Long>();
        roleUserList.add(UMConstants.ANONYMOUS_ROLE_ID);
        roleUserList.add(UMConstants.ANONYMOUS_ROLE_ID);
        loginSerivce.saveUserRoleMapping(-1L, roleUserList);
		
		
		permissionService.clearPermissionData(GroupPermission.class.getName());
    }
    
    
    @Autowired private IRoleDao roleDao;
    
    @Test
    public void testDeleteRole() {
    	List<?> list1 = roleDao.getEntities("from GroupUser");
    	List<?> list2 = roleDao.getEntities("from RoleUser");
    	List<?> list3 = roleDao.getEntities("from RoleGroup");
    	if(list1.size() > 0) {
    		GroupUser gu = (GroupUser) list1.get(0);
    		Assert.assertEquals(gu.getId(), gu.getPK());
    		Assert.assertNotNull(gu.getUserId());
    		Assert.assertNotNull(gu.getGroupId());
    		gu.setId((Long) gu.getPK());
    	}
    	
    	if(list2.size() > 0) {
    		RoleUser ru = (RoleUser) list2.get(0);
    		Assert.assertEquals(ru.getId(), ru.getPK());
    		Assert.assertNotNull(ru.getUserId());
    		Assert.assertNotNull(ru.getRoleId());
    		ru.setId((Long) ru.getPK());
    		Assert.assertNull( ru.getStrategyId() );
    		Assert.assertNull( ru.getModuleId() );
    		log.debug(ru.toString() + ru.hasCode());
    	}
    	
    	if(list3.size() > 0) {
    		RoleGroup rg = (RoleGroup) list3.get(0);
    		Assert.assertEquals(rg.getId(), rg.getPK());
    		Assert.assertNotNull(rg.getRoleId());
    		Assert.assertNotNull(rg.getGroupId());
    		rg.setId((Long) rg.getPK());
    		Assert.assertNull( rg.getStrategyId() );
    	}
    	
    	 // 删除角色组
        action.delete(response, roleGroupId);
        action.getAllRole2Tree(response);
    }

	private void printVisibleMainGroups(int size) {
        List<?> groups = groupService.findGroups();
        for(Object temp : groups) {
            log.debug(temp);
        }
        assertEquals(size, groups.size()); 
	}
}
