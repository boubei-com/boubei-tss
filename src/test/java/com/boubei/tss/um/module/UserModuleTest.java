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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.SystemInfo;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.UserAction;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.entity.UserToken;
import com.boubei.tss.um.helper.UMQueryCondition;
import com.boubei.tss.um.service.IResourceService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;

/**
 * 用户相关模块的单元测试
 */
public class UserModuleTest extends AbstractTest4UM {
    
	@Autowired UserAction action;
	@Autowired SystemInfo si;
    
    @Autowired IUserService service;
    @Autowired IResourceService appService;
    @Autowired IUserDao userDao;
    
    Group mainGroup;
    Long  mainGroupId;
    
    Group assitantGroup;
    User user1;
    
    public void init() {
    	super.init();
        
    	// 检查初始化的组是否存在
    	List<?> groups = groupService.findGroups();
    	for(Object temp : groups) {
            log.debug(temp);
        }
        log.debug("\n");
        
        ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.ADMIN_ROLE, PX.ADMIN_ROLE, UMConstants.ANONYMOUS_ROLE_ID.toString());
        groups = groupService.findGroups();
    	for(Object temp : groups) {
            log.debug(temp);
        }
        log.debug("\n");
    	
        mainGroup = new Group();
        mainGroup.setParentId(UMConstants.MAIN_GROUP_ID);
        mainGroup.setName("主用户组一");
        mainGroup.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(mainGroup , "", "-1,-8");
        log.debug(mainGroup + "\n");
        
        mainGroupId = mainGroup.getId();
        
        // 管理员直接在主组下新增用户
        user1 = new User();
        user1.setLoginName("U_JonKing");
        user1.setUserName("U_JK");
        user1.setPassword("123456");
		user1.setGroupId(mainGroupId);
		user1.setAddress("ZheJiang HZ");
		user1.setCertificate("身份证");
		user1.setCertificateNo("332624******");
		user1.setUdf("210000");
		user1.setTelephone("88819585");
		user1.setEmail("jk@boubei.com");
        service.createOrUpdateUser(user1, "" + mainGroupId, "-1,-8,-9");
        Assert.assertNull( user1.getLastLogonTime() );
        
        List<User> users = groupService.getUsersByGroupId(mainGroupId);
        assertEquals(1, users.size());
        log.debug(users.get(0) + "\n");
        
        // 建一个辅助用户组
        assitantGroup = new Group();
        assitantGroup.setParentId(UMConstants.ASSISTANT_GROUP_ID);
        assitantGroup.setName("第一纵队");
        assitantGroup.setGroupType( Group.ASSISTANT_GROUP_TYPE );
        groupService.createNewGroup(assitantGroup , user1.getId() + "", "-1");
        log.debug(assitantGroup + "\n");
    }
    
    @After
    public void tearDown() throws Exception {
    	_TestUtil.printLogs(logService);
    	super.tearDown();
    }
    
    @Test
    public void testOverdue() {
    	service.overdue();
    }
    
    @Test
    public void getUsersByGroup() {
    	action.getUsersByGroupId(response, mainGroupId, 1);
    	
    	List<User> mainUsers  = groupService.getUsersByGroupId(mainGroupId);
        assertEquals(1, mainUsers.size());
       
		action.searchUser(response, 1, mainGroupId, "U_JK");
		
		PageInfo pageInfo = service.searchUser(mainGroupId, "U_JK", 1);
		Assert.assertTrue(pageInfo.getItems().size() > 0);
		
		pageInfo = service.searchUser(mainGroupId, "88819585", 1);
		Assert.assertTrue(pageInfo.getItems().size() > 0);
		
		pageInfo = service.searchUser(mainGroupId, "jk@boubei.com", 1);
		Assert.assertTrue(pageInfo.getItems().size() > 0);
		
		pageInfo = service.searchUser(mainGroupId, "U_JonKing", 1);
		Assert.assertTrue(pageInfo.getItems().size() > 0);
		
		pageInfo = service.searchUser(mainGroupId, "金", 1);
		Assert.assertTrue(pageInfo.getItems().size() == 0);
		
		Assert.assertNull( userDao.getGroup2User(-1L, -101L) );
    }
    
    @Test
    public void getUMQueryCondition() {
		UMQueryCondition userQueryCon = new UMQueryCondition();
        userQueryCon.setGroupId(mainGroupId);
        userQueryCon.getPage().setPageNum(1);
		userQueryCon.setBirthday(new Date());
		userQueryCon.setCertificateNo("332624");
		userQueryCon.setEmployeeNo("");
		userQueryCon.setGroupIds(Arrays.asList(mainGroupId));
		userQueryCon.setLoginName("U_JonKing");
		userQueryCon.setUserName("U_JK");
		
		userQueryCon.toConditionString();
    }
    
    @Test
    public void testUserCRUD() {
    	// 删除用户和辅助用户组的联系
    	action.deleteUser(response, assitantGroup.getId(), user1.getId());
    	
    	// 删除用户
    	action.deepDeleteUser(mainGroupId, user1.getId());
    	logout();
    	action.deepDeleteUser(mainGroupId, user1.getId());
    	login(UMConstants.ADMIN_USER);
    	
    	// 删除系统管理员
    	try {
    		action.deleteUser(response, UMConstants.MAIN_GROUP_ID, UMConstants.ADMIN_USER_ID);
    		Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.U_32, "Admin"), e.getMessage());
        }
		
		// 新增用户
		user1.setId(null);
		request.addParameter("User2GroupExistTree", "" + mainGroupId);
    	request.addParameter("User2RoleExistTree", "-1");
		action.saveUser(response, request, user1);
		log.debug(user1);
		
		// 没有组，修改无效
		request.removeParameter("User2GroupExistTree");
		user1.setUserName("U_JK_kkkkk");
		action.saveUser(response, request, user1);  
        
        // 注册一个用户
        User user2 = new User();
        user2.setLoginName("U_JonKing-R");
        user2.setUserName("U_JK-R");
        user2.setPassword("123456");
        user2.setEmail("jk@163.com");
        user2.setTelephone("13388887777");
        user2.setBelongUserId(user1.getId());
        service.regUser(user2);
        log.debug(user2);
        Assert.assertEquals(user1.getId(), user2.getBelongUserId());
        
        // 修改用户
        user2.setUserName("JK-2");
        try {
        	action.modifyUserSelf(response, user2);
    		Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("非本人不能修改", true);
        }
        
        try {
        	action.checkPermission(UMConstants.MAIN_GROUP_ID, "xxx");
    		Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( "permit denied", e.getMessage());
        }
        
        try {
        	User user3 = new User();
        	userDao.checkUserAccout(user3);
    		Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.U_50, e.getMessage());
        }
        
        logout();
        login(user2);
        
        action.modifyUserSelf(response, user2);
        
        logout();
        login(user2);
        
        // 注册一个同名用户
        User user3 = new User();
        BeanUtil.copy(user3, user2);
        user3.setId(null);
    	try {
    		service.regUser(user3);
    		Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.U_29, user3.getLoginName()), e.getMessage());
        }
    	
    	user3.setLoginName("user3xxx");
    	try {
    		service.regUser(user3);
    		Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.U_30, user3.getEmail()), e.getMessage());
        }
    	
    	user3.setEmail("user3xxx@163.com");
    	try {
    		service.regUser(user3);
    		Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.U_31, user3.getTelephone()), e.getMessage());
        	user3._mobile();
        }
    }
  
    @Test
    public void getUserInfo() {
    	action.getUserInfoAndRelation(response, UMConstants.DEFAULT_NEW_ID, mainGroupId); // 获取新增用户模板
        action.getUserInfoAndRelation(response, user1.getId(), mainGroupId); // 获取编辑用户模板
        
        action.getOnlineUserInfo(response);
        
        action.getUserInfo(response);
        
        action.getRegisterForm(response);
        
        action.getOperatorInfo(response);
        action.getOperatorInfo(response);
        
        action.getUserHas("false");
        
        action.getUserHas("true");
    }
    
    @Test
    public void startOrStopUser() {
    	action.startOrStopUser(response, mainGroupId, user1.getId(), ParamConstants.TRUE);
        action.startOrStopUser(response, mainGroupId, user1.getId(), ParamConstants.FALSE);
        
        action.startOrStopUser(response, mainGroupId, user1.getId(), ParamConstants.TRUE);
        
        Long assistantGroupId = assitantGroup.getId();
		groupService.startOrStopGroup(assistantGroupId, ParamConstants.TRUE);
		groupService.startOrStopGroup(mainGroupId, ParamConstants.TRUE);
		
        action.startOrStopUser(response, assistantGroupId, user1.getId(), ParamConstants.FALSE);
        
        user1 = service.getUserById(user1.getId());
        user1.setAccountLife(DateUtil.parse("2012-12-12"));
        service.createOrUpdateUser(user1,  "" + mainGroupId, "-1");
        try {
        	action.startOrStopUser(response, assistantGroupId, user1.getId(), ParamConstants.FALSE);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.U_27, e.getMessage());
        }
        
        action.moveUser(response, UMConstants.MAIN_GROUP_ID, user1.getId());
        action.moveUser(response, mainGroupId, user1.getId());
    }
    
    @Test
    public void initUserInfo() {
		action.initAuthenticateMethod(response, mainGroupId);
		action.uniteAuthenticateMethod(response, mainGroupId, "com.boubei.tss.um.sso.UMPasswordIdentifier");

		request.addParameter("password", "369852");
		action.initPassword(request, response, mainGroupId, user1.getId());
		action.initPassword(request, response, mainGroupId, 0L);
		 
		try {
			service.initPasswordByGroupId(mainGroupId, user1.getId(), null);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.U_33, e.getMessage());
		}
		
		service.createOrUpdateUser(user1, "", null);
    }
    
    @Test
	public void testSuUser() {
    	login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
    	
    	String sessionId = Context.getRequestContext().getSessionId();
    	Context.sessionMap.put(sessionId, request.getSession());
    	
    	IOnlineUserManager manager = (IOnlineUserManager) Global.getBean("DBOnlineUserService");
        manager.register("token_0", "TSS", sessionId,  -1L, "Admin");
        
        
        String cKey = "synchronized-" + user1.getLoginName(); 
		Pool cache = CacheHelper.getNoDeadCache();
		cache.putObject(cKey, new Object());
		cache.getObject(cKey);
    	
    	si.su( request, user1.getLoginName() );
    	si.su( request, user1.getLoginName() );
    	Assert.assertNotNull( cache.getObject(cKey) );
    	
    	si.su( request, "xxxxxx" );
    	si.su( request, "admin" );
    }

	@Test
	public void testUserToken() {
		UserToken ut = new UserToken();
		ut.setId(null);
		ut.setCreateTime(new Date());
		ut.setCreator("Admin");
		ut.setExpireTime(new Date());
		ut.setRemark("test");
		ut.setDescription("test");
		ut.setResource("report1");
		ut.setToken("XXXXXXX");
		ut.setType("D1");
		ut.setUpdateTime(new Date());
		ut.setUpdator("Admin");
		ut.setUser("JK");
		ut.setVersion(0);
		ut.setDomain("DomainX");
		ut.setAppId("WMS");
		userDao.createObject(ut);
		ut = (UserToken) userDao.getEntity(UserToken.class, ut.getPK());
		
		ut.toString();
		Assert.assertEquals(ut.getId(), ut.getPK());
		ut.getCreateTime();
		ut.getCreator();
		ut.getExpireTime();
		ut.getRemark();
		ut.getDescription();
		ut.getResource();
		ut.getToken();
		ut.getType();
		ut.getUpdateTime();
		ut.getUpdator();
		ut.getUser();
		ut.getVersion();
		ut.getDomain();
		ut.getAppId();
	}
}
