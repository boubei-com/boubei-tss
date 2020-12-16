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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.RoleAction;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.GroupDTO;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.DateUtil;

import junit.framework.Assert;

public class LoginServiceTest extends AbstractTest4UM {
	
	@Autowired RoleAction roleAction;
    @Autowired IRoleService roleService;
    @Autowired IUserService userService;
    
	Group mainGroup;
    User mainUser;
    Long role1Id;
    
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
        
        // 新增用户
        mainUser = new User();
        mainUser.setLoginName("JonKing");
        mainUser.setUserName("JK");
        mainUser.setPassword("123456");
        mainUser.setGroupId(mainGroup.getId());
        userService.createOrUpdateUser(mainUser , "" + mainGroup.getId(), "");
        log.debug(mainUser);
 
        // 新建角色
        Role role1 = new Role();
        role1.setIsGroup(0);
        role1.setName("办公室助理");
        role1.setParentId(0L);
        role1.setStartDate(new Date());
        role1.setEndDate( DateUtil.addYears(50) );
        request.addParameter("Role2UserIds", UMConstants.ADMIN_USER_ID + "," + mainUser.getId());
        request.addParameter("Role2GroupIds", "" + mainGroup.getId());
        roleAction.saveRole(response, request, role1);
        role1Id = role1.getId();
        
        List<Long> roleIds = new ArrayList<Long>();
        Assert.assertTrue( loginSerivce.getRoleNames(roleIds ).isEmpty() );
        
        roleIds.add(role1Id);
		loginSerivce.getRoleNames(roleIds);
		
		List<OperatorDTO> temp = loginSerivce.getUsersByRoleId(role1Id, Environment.getDomain());
		Assert.assertTrue(temp.isEmpty());
		temp = loginSerivce.getUsersByRole(role1.getName(), Environment.getDomain());
		Assert.assertTrue(temp.isEmpty());
		try {
			loginSerivce.getUsersByRole("不存在的角色", Environment.getDomain());
			Assert.fail();
		} catch(Exception e) { }
    }
    
	@Test
	public void test() {
		Long userId = mainUser.getId();
		Long groupId = mainGroup.getId();
		
		List<Long> roleIds = loginSerivce.getRoleIdsByUserId(userId) ;
		Assert.assertEquals(1, roleIds.size()); // role1
		Assert.assertEquals(role1Id, roleIds.get(0));
		
		List<Group> fatherGroups = loginSerivce.getGroupsByUserId(userId);
		Assert.assertTrue(fatherGroups.size() > 0);
		
		List<GroupDTO> groups = loginSerivce.getGroupTreeByGroupId(groupId);
		Assert.assertTrue(groups.size() == 2);
		
		GroupDTO gDTO = groups.get(0);
		gDTO.setDisabled(0);
		
		List<OperatorDTO> users0 = loginSerivce.getUsersByGroupId(groupId, true);
		List<OperatorDTO> users1 = loginSerivce.getUsersByGroupId(groupId, false);
		List<OperatorDTO> users2 = loginSerivce.getUsersByRoleId(role1Id);
		Assert.assertTrue(users0.size() == 1); // JK
		Assert.assertTrue(users1.size() == 1); // JK
		Assert.assertTrue(users2.size() == 2); // Admin, JK
		Assert.assertEquals("JK", users1.get(0).getAttributesMap().get("userName"));
		log.info( users1.get(0).toString() );
		
		Assert.assertEquals(0, loginSerivce.checkPwdSecurity(userId));
		loginSerivce.resetPassword(userId, "abc123456=11");
		
		loginSerivce.resetPassword(-1L, "123456");
		
		// 检测密码安全情况
		int flag = loginSerivce.checkPwdSecurity(userId);
		Assert.assertEquals(1, flag);
		mainUser.setLastPwdChangeTime( DateUtil.subDays(new Date(), 300) ); // 超过180天没有修改密码
		commonDao.update(mainUser);
		
		flag = loginSerivce.checkPwdSecurity(userId);
		Assert.assertEquals(-1, flag);
		
		String[] emails = loginSerivce.getContactInfos("-1@tssRole,-2@tssGroup,-2@tssGroupDeep,xxx@xxx", false);
		Assert.assertEquals(1, emails.length);
		
		String loginName = mainUser.getLoginName();
		mainUser.setAccountLife( DateUtil.noHMS(new Date()) );
		userService.createOrUpdateUser(mainUser, "", "");
		try {
			loginSerivce.getLoginInfoByLoginName(loginName);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_27, e.getMessage());
		}
		
		userService.startOrStopUser(userId, 1, groupId);
		try {
			userService.startOrStopUser(UMConstants.ADMIN_USER_ID, 1, UMConstants.DEFAULT_ROOT_ID);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_321, e.getMessage());
		}
		
		try {
			loginSerivce.getLoginInfoByLoginName(loginName);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_26, e.getMessage());
		}
		
		Assert.assertNull(loginSerivce.getContactInfos(null, true));
		
		Assert.assertEquals(0, loginSerivce.getContactInfos("boubei", true).length);
		Assert.assertEquals(0, loginSerivce.getContactInfos("boubei@tssRole", true).length);
		
		Group selfRegGroup = (Group) commonDao.getEntity(Group.class, UMConstants.SELF_REGISTER_GROUP_ID);
		String selfRegDomain = selfRegGroup.getDomain();
		List<?> list = loginSerivce.getUsersByDomain(selfRegDomain, "loginName", Environment.getUserId());
		Assert.assertEquals(1, list.size());
		
		list = loginSerivce.getUsersByDomain("企业域", "loginName", Environment.getUserId());
		Assert.assertEquals(0, list.size());
		
		// logout, then reset passwd
		Context.destroyIdentityCard( Context.getIdentityCard().getToken() );
    	Context.destroy();
		loginSerivce.resetPassword(userId, "abc123456=11");
	}
}
