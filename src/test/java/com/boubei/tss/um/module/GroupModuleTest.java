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

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.ProgressPool;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.GroupAction;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.UMQueryCondition;
import com.boubei.tss.um.service.IResourceService;
import com.boubei.tss.um.service.IUserService;

/**
 * 用户组织相关模块的单元测试。
 * 
 * 注：排序组时，事务内组表的decode发生了变化，但资源视图内的相应decode值取出来还是排序以前的值，即视图的数据未更新。
 *    TODO 需要排查上述情况出现的原因，是H2数据库问题？抑或其他原因？
 */
public class GroupModuleTest extends AbstractTest4UM {
    
	static final String APPLICATION_ID = Config.getAttribute(PX.APPLICATION_CODE).toLowerCase();

	@Autowired GroupAction action;
    
	@Autowired IGroupDao groupDao;
    @Autowired IUserService userService;
    @Autowired IResourceService appService;
    
    Group mainGroup1;
    Long mainGroupId;
    
    public void init() {
        super.init();
        
    	// 检查初始化的组是否存在
    	List<?> groups = groupService.findGroups();
    	for(Object temp : groups) {
            log.debug(temp);
        }
        log.debug("\n");
    	
        mainGroup1 = new Group();
        mainGroup1.setParentId(UMConstants.MAIN_GROUP_ID);
        mainGroup1.setName("主用户组一");
        mainGroup1.setGroupType( Group.MAIN_GROUP_TYPE );
        mainGroup1.setDomain("boubei.com");
        groupService.createNewGroup(mainGroup1 , "", "-1,-8");
        log.debug(mainGroup1 + "\n");
        
        mainGroupId = mainGroup1.getId();
        
        User user1 = new User();
        user1.setLoginName("JonKing");
        user1.setUserName("JK");
        user1.setPassword("123456");
        user1.setGroupId(mainGroupId);
        userService.createOrUpdateUser(user1 , "" + mainGroupId, "-1,-8");
        log.debug(user1 + "\n");
        
        List<User> users = groupService.getUsersByGroupId(mainGroupId);
        assertEquals(1, users.size());
        log.debug(users.get(0) + "\n");
        
        login(user1);
        Assert.assertTrue( Environment.inGroup(mainGroup1.getName(), false) );
        Assert.assertTrue( Environment.inGroup(mainGroup1.getName(), true ) );
        Assert.assertFalse( Environment.inGroup("xxxxxxxx", true ) );
        
        login("Admin");
    }
    
    @After
    public void tearDown() throws Exception {
    	_TestUtil.printLogs(logService);
    	super.tearDown();
    }
    
    @Test
    public void testStartOrStopGroup() {
        action.startOrStopGroup(response, mainGroupId, 1);
        action.startOrStopGroup(response, mainGroupId, 0);
    }
 
    @Test
    public void testGroupRoles() {
    	List<?> roles = groupService.findEditableRoles();
        assertEquals(2, roles.size());  // 域管理员/开发者
        
        roles = groupService.findRolesByGroupId(mainGroupId);
        assertEquals(1, roles.size());
        log.debug(roles.get(0) + "\n");
    }
    
    @Test 
    public void testDeleteGroupAndUsers() {
    	Group mg = new Group();
        mg.setParentId(UMConstants.MAIN_GROUP_ID);
        mg.setName("主用户组-T1");
        mg.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(mg , "", "-1,-8");
        
        User user1 = new User();
        user1.setLoginName("JonKing-T");
        user1.setUserName("JK");
        user1.setPassword("123456");
        user1.setGroupId(mg.getId());
        userService.createOrUpdateUser(user1 , "" + mg.getId(), "-1,-9");
        
        // 正常删除
        action.deleteGroup(response, mg.getId());
        
        // 删除用户自己所在的组
        try {
        	action.deleteGroup(response, -1L);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.U_20, e.getMessage());
        }
        
        // 组下有用户，且已经登录过，需要先移除用户才能删除组 
        mg = new Group();
        mg.setParentId(UMConstants.MAIN_GROUP_ID);
        mg.setName("主用户组-T2");
        mg.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(mg , "", "-1,-8");
        
        user1 = new User();
        user1.setLoginName("JonKing-T");
        user1.setUserName("JK");
        user1.setPassword("123456");
        user1.setGroupId(mg.getId());
        user1.setLogonCount( 1 );
        userService.createOrUpdateUser(user1 , "" + mg.getId(), "-1,-9");
        try {
        	groupDao.removeGroup(mg);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.U_322, mg.getName()) , e.getMessage());
        }
    }
    
    @Test
    public void testMainGroupCRUD() {
    	
    	request.addParameter("Group2UserExistTree", "");
    	request.addParameter("Group2RoleExistTree", "-1,-8");
    	action.saveGroup(response, request, mainGroup1);
    	
    	Group group0 = new Group();
    	group0.setParentId(UMConstants.MAIN_GROUP_ID);
    	group0.setName("主用户组二0");
    	group0.setGroupType( Group.MAIN_GROUP_TYPE );
        action.saveGroup(response, request, group0);
        action.deleteGroup(response, group0.getId());
    	
        Group group2 = new Group();
        group2.setParentId(UMConstants.MAIN_GROUP_ID);
        group2.setName("主用户组二");
        group2.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(group2 , "", "-1,-8");
        action.saveGroup(response, request, group2);
        action.saveGroup(response, request, group2);
        
        action.deleteGroup(response, group2.getId());
        group2.setId(null);
        groupService.createNewGroup(group2 , "", "-1,-8");
        
        Group group3 = new Group();
		group3.setParentId(mainGroupId);
        group3.setName("主用户组一.1");
        group3.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(group3 , "", "-1,-8");
 
        List<Group> groups = groupDao.getGroupsByType(Environment.getUserId(), 
        		UMConstants.GROUP_VIEW_OPERRATION, Group.MAIN_GROUP_TYPE);
        for(Group temp : groups) {
        	temp.setName(temp.getName() + "...");
            groupService.editExistGroup(temp, "", "-1,-8");
        }
        
        List<?> sList = generalSearcher.searchResource("Group", "组一.1");
		Assert.assertEquals(1, sList.size() );
		Assert.assertEquals("root > Main-Group... > 主用户组一...", ((Object[])sList.get(0))[2] );
        
        log.debug("Testing sort group......");
        action.sortGroup(response, mainGroupId, group2.getId(), 1);
        
        action.move(response, group2.getId(), mainGroupId);
        
        action.getVisibleGroup2Tree(response);
        
        Object[] result = groupService.getMainGroupsByOperationId(UMConstants.GROUP_EDIT_OPERRATION);
        List<?> groupIds = (List<?>) result[0];
        List<?> mainGroups = (List<?>) result[1];
        for(Object temp : groupIds) {
            log.debug(temp);
        }
        for(Object temp : mainGroups) {
            log.debug(temp);
        }
        log.debug("\n");
        
        try {
        	groupService.createDomainGroup("哈哈", 99999L);
        	Assert.fail();
        } catch( Exception e) {
        	Assert.assertEquals(EX.parse(EX.U_53, 99999L), e.getMessage());
        }
     }
    
    @Test
    public void testAssistGroupCRUD() {
        User user1 = userService.getUserByLoginName("JonKing");
        
        Group group1 = new Group();
        group1.setParentId(UMConstants.ASSISTANT_GROUP_ID);
        group1.setName("辅助组一");
        group1.setGroupType( Group.ASSISTANT_GROUP_TYPE );
        groupService.createNewGroup(group1 , "" + user1.getId(), "-1,-8");
        
        groupService.deleteGroup(group1.getId());
        group1.setId(null);
        groupService.createNewGroup(group1 , "" + user1.getId(), "-1,-8");
        
        Group group2 = new Group();
        group2.setParentId(UMConstants.ASSISTANT_GROUP_ID);
        group2.setName("辅助组二");
        group2.setGroupType( Group.ASSISTANT_GROUP_TYPE );
        groupService.createNewGroup(group2 , "", "-1,-8");
        
        Group group3 = new Group();
        group3.setParentId(group1.getId());
        group3.setName("辅助组一.1");
        group3.setGroupType( Group.ASSISTANT_GROUP_TYPE );
        groupService.createNewGroup(group3 , "12,13", "-1,-8");
        
        groupService.editExistGroup(group3 , "12,14", "-1,-8");
        
        List<User> users = groupService.getUsersByGroupId(group1.getId());
        assertEquals(1, users.size());
        log.debug(users.get(0) + "\n");
        
        List<?> roles = groupService.findRolesByGroupId(group1.getId());
        assertEquals(1, roles.size());
        log.debug(roles.get(0) + "\n");
        
        Object[] result = groupService.getAssistGroupsByOperationId(UMConstants.GROUP_EDIT_OPERRATION);
        List<?> groupIds = (List<?>) result[0];
        List<?> assistGroups = (List<?>) result[1];
        for(Object temp : groupIds) {
            log.debug(temp);
        }
        for(Object temp : assistGroups) {
            log.debug(temp);
        }
        log.debug("\n");
        
        action.getCanAddedGroup2Tree(response, Group.ASSISTANT_GROUP_TYPE);
        
        action.move(response, group2.getId(), group1.getId());
        try {
        	action.move(response, group2.getId(), mainGroupId);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("不能移动到不同类型的组织下面", true);
        }
    }
    
    @Test
    public void testGetGroupTree() {
        action.getAllGroup2Tree(response);
        
        action.getCanAddedGroup2Tree(response, Group.MAIN_GROUP_TYPE);
        action.getCanAddedGroup2Tree(response, Group.ASSISTANT_GROUP_TYPE);
    }
    
    @Test
    public void testGetGroupInfo() {
        action.getGroupInfo(response, UMConstants.MAIN_GROUP_ID, UMConstants.DEFAULT_NEW_ID, Group.MAIN_GROUP_TYPE);
        action.getGroupInfo(response, UMConstants.MAIN_GROUP_ID, mainGroupId, Group.MAIN_GROUP_TYPE);
        
        action.getGroupInfo(response, UMConstants.ASSISTANT_GROUP_ID, UMConstants.DEFAULT_NEW_ID, Group.ASSISTANT_GROUP_TYPE);
    }
    
    @Test
    public void testGetOperation() {
    	action.getOperation(response, mainGroupId);
    }
    
    @Test
    public void getUserByGroupId() {
    	action.getUserByGroupId(response, UMConstants.MAIN_GROUP_ID);
    }
    
    @Test
    public void getProgressActionSupport() {
    	String code = "123456";
    	Progress progress = new Progress(100);
		ProgressPool.putSchedule(code, progress);
    	
		action.getProgress(response, "not found");
    	action.getProgress(response, code);
    	try {
    		action.doConceal(response, code); // 取消进度条
    		Assert.fail("should throw exception but didn't.");
    	} 
    	catch(Exception e) {
    		Assert.assertTrue(true);
    	}
    	
    	action.doConceal(response, code); // 重复取消进度条
    	
    	// second test
    	progress = new Progress(100);
		ProgressPool.putSchedule(code, progress);
		
    	progress.setException( new BusinessException("I'm ill") );
    	progress.setNormal(false); // 设置为异常
    	try {
    		action.getProgress(response, code);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("I'm ill", true);
        }
    	
    	// third test
    	progress = new Progress(100);
		ProgressPool.putSchedule(code, progress);
		progress.add(100);
    	action.getProgress(response, code);
    }
    
    @Test 
    public void testGroupDao() {
    	groupDao.getUsersByGroup(mainGroupId, 1, " u.id asc ");
    	
    	UMQueryCondition condition = new UMQueryCondition();
    	condition.setGroupId(-99L);
		Assert.assertNull( groupDao.searchUser(condition ) );
		
		Assert.assertFalse(groupDao.isOperatorInGroup(-99L, -1L));
		
		Assert.assertEquals(0, groupDao.getUsersByGroupIds(null).size());
		Assert.assertEquals(0, groupDao.getParentGroupByGroupIds(null, 1L, "1").size());
		
		Assert.assertNull( groupDao.findMainGroupByUserId(-99L) );
    }
}
