/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record.permission.RecordPermission;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.dm.report.permission.ReportPermission;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.cloud.entity.DomainInfo;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.Message;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.entity.permission.RolePermission;
import com.boubei.tss.um.helper.UMQueryCondition;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.um.sso.UMPasswordIdentifier;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

@Service("UserService")
public class UserService implements IUserService{
	
	@Autowired private IUserDao  userDao;
	@Autowired private IRoleDao  roleDao;
	@Autowired private IGroupDao groupDao;
	
	@Autowired private IGroupService groupService;
	@Autowired private IRoleService  roleService;
	@Autowired private ReportService reportService;
	@Autowired private RecordService recordService;

	public void deleteUser(Long groupId, Long userId) {
		/* 
		 * 登录过的用户( entity.getLogonCount() > 0 )不能被删除，只能被停用。
		 * 防止域管理员把域下用户删除，导致删除用户创建的数据表记录无法被查询到，甚至会可能被其它域下后期注册的同名用户吸走了） 
         */
		User entity = userDao.getEntity(userId);
        if( Environment.getUserId().equals(userId) ) { 
            throw new BusinessException( EX.parse(EX.U_32, entity.getLoginName()) );
        }
        
        Group group = groupDao.getEntity(groupId);
        if(Group.ASSISTANT_GROUP_TYPE.equals(group.getGroupType())){
        	groupDao.delete(userDao.getGroup2User(groupId, userId));
        } 
        else {
        	userDao.removeUser(entity);	
        	
        	// 判断用户是否为开发者，是的话删除其开发目录、角色组、及其发布的模块
        	if( UMConstants.DEV_GROUP_ID.equals(groupId) ) {
        		String loginName = entity.getLoginName();
				Param env = ParamManager.getSimpleParam( loginName );
        		String[] groups = env.getValue().split(",");
        		Long reportGroupId = EasyUtils.obj2Long(groups[0]);
        		Long recordGroupId = EasyUtils.obj2Long(groups[1]);
        		Long roleGroupId = EasyUtils.obj2Long(groups[3]);
        		
        		try {
	        		reportService.delete(reportGroupId);
	        		recordService.delete(recordGroupId);
	        		roleService.delete(roleGroupId); 
        		} 
        		catch(Exception e) { }
        		
        		userDao.deleteAll( userDao.getEntities(" from ModuleDef where creator = ?1", loginName) );
        	}
        }
	}
	
	public void deepDeleteUser(Long groupId, Long userId) {
		User user = this.getUserById(userId);

		userDao.executeHQL("delete from SubAuthorize where buyerId = ?1", userId);
		userDao.executeHQL("delete from Account where belong_user = ?1", user);
		
		this.deleteUser(groupId, userId);
		
		userDao.executeHQL("delete from CloudOrder where creator = ?1", user.getLoginName());
	}

	public User getUserById(Long id) {
		User entity = userDao.getEntity(id);
		userDao.evict(entity);
		return entity;
	}

    public void updateUser(User user) {
        userDao.update(user);
    }

    public User getUserByLoginName(String loginName) {
        return userDao.getUserByAccount(loginName, false);
    }
 
    public void initPasswordByGroupId(Long groupId, Long userId, String initPassword) {
        if ( EasyUtils.isNullOrEmpty(initPassword) ) {
            throw new BusinessException(EX.U_33);
        }
        
        List<User> userList;
        
        // 如果指定了用户，则只初始化该用户的密码
        if(userId != null && userId.longValue() > 0) {
        	userList = Arrays.asList(userDao.getEntity(userId));
        }
        else {
        	userList = groupDao.getUsersByGroupIdDeeply(groupId);
        }
       
        for (User user : userList) {
			user.setOrignPassword( initPassword );  // 主用户组进行密码初始化时加密密码
            userDao.initUser(user);
        }
    }
    
    public void uniteAuthenticateMethod(Long groupId, String authMethod) {
        List<User> userList = groupDao.getUsersByGroupIdDeeply(groupId);
        for (User user : userList) {
            user.setAuthMethod(authMethod);
            userDao.update(user);
        }           
    }
    
	/* 
	 * 新建的用户或密码已修改的用户，则对用户名＋密码进行MD5加密 
	 */
	public void createOrUpdateUser(User user, String groupIdsStr, String roleIdsStr) {
        Long userId = user.getId();
        String password = user.getPassword();
        
        userDao.checkUserAccout(user);  //新建用户需要检测登陆名是否重复（修改也需要检测，以防手机号和邮箱改成和其它账号重复）
        
        if( userId == null ) {
            user.setOrignPassword( password );
            user.setAccountLife(null);
            user.setAuthMethod( UMPasswordIdentifier.class.getName() );
            user.setOrigin(Environment.getUserName());
            
            user = userDao.create(user);
            userId = user.getId();
        } 
        else {
        	User older = userDao.getEntity(userId);
            userDao.evict(older);
            if ( !password.equals(older.getPassword()) ) { // 密码被修改
            	user.setOrignPassword( password );
            }    
             
            userDao.update(user);
        }
        
        saveUser2Group(userId, groupIdsStr);
        saveUser2Role (userId, roleIdsStr);
        
        // 检查被操作用户是否为域管理员，域管理员不能移动到域组以外的组
        checkDomainAdmin(userId);
        
        Group mGroup = groupDao.findMainGroupByUserId(userId);
        groupDao.evict(mGroup);
		userDao.recordUserLog(user, mGroup, "save");
        
        // 刷新用户的缓存信息
        CacheHelper.flushCache(CacheLife.SHORT.toString(), "ByUserId(" +userId+ ")");
        CacheHelper.flushCache(CacheLife.SHORT.toString(), "ByLoginName(" +user.getLoginName()+ ")");
        CacheHelper.flushCache(CacheLife.SHORTER.toString(), "getUsers");
	}
	
    private void checkDomainAdmin(Long userId) {
		String hql1 = "select g from GroupUser gu, Group g where gu.userId = ?1 and gu.groupId = g.id and (g.levelNo = 4 or g.domainRoot = 1)";
		List<?> list = userDao.getEntities(hql1, userId);
		if( Environment.isDomainAdmin() && Environment.getUserId().equals(userId) &&list.isEmpty() ) {
			throw new BusinessException(EX.U_52);
		}
	}

	/** 用户对组 */
    private void saveUser2Group(Long userId, String groupIdsStr) {
        List<?> user2Groups = userDao.findUser2GroupByUserId(userId);
        Map<Long, Object> historyMap = new HashMap<Long, Object>(); //把老的组对用户记录做成一个map，以"userId"为key
        for (Object temp : user2Groups) {
            GroupUser groupUser = (GroupUser) temp;
            historyMap.put(groupUser.getGroupId(), groupUser);
        }
        
        if ( !EasyUtils.isNullOrEmpty(groupIdsStr) ) {
            String[] groupIds = groupIdsStr.split(",");
            for (String temp : groupIds) {
            	if(EasyUtils.isNullOrEmpty(temp)) continue;
            	
                // 如果historyMap里面没有，则新增用户组对用户的关系；如果historyMap里面有，则从历史记录中移出；剩下的将被删除
                Long groupId = Long.valueOf(temp);
                if (historyMap.remove(groupId) == null) { 
                    createUser2Group(userId, groupId); // 如果历史数据里面没有，则新增
                } 
            }
        }
        
        // historyMap中剩下的就是该删除的了
        userDao.deleteAll(historyMap.values());
    }

    /* 用户对角色 */
    private void saveUser2Role(Long userId, String roleIdsStr) {
        List<?> user2Roles = userDao.findRoleUserByUserId(userId);
        Map<Long, Object> historyMap = new HashMap<Long, Object>(); //把老的组对用户记录做成一个map，以"userId"为key
        for (Object temp : user2Roles) {
            RoleUser roleUser = (RoleUser) temp;
            historyMap.put(roleUser.getRoleId(), roleUser);
        }
        
        // 检查操作人是否对角色有管理权限
        List<?> editableRoles = groupService.findEditableRoles();
        List<Long> editableRoleIds = new ArrayList<Long>();
        for(Object o : editableRoles) {
        	editableRoleIds.add( ((Role)o).getId() );
        }
        
        if ( !EasyUtils.isNullOrEmpty(roleIdsStr) ) {
            String[] roleIds = roleIdsStr.split(",");
            for (String temp : roleIds) {
            	if(EasyUtils.isNullOrEmpty(temp)) continue;
            	
                // 如果historyMap里面没有，则新增用户组对用户的关系；如果historyMap里面有，则从历史记录中移出；剩下的将被删除
                Long roleId = Long.valueOf(temp);
                
                if (historyMap.remove(roleId) == null && editableRoleIds.contains(roleId)) { 
                    createUser2Role(userId, roleId); 
                } 
            }
        }
        
        // historyMap中剩下的就是该删除的了
        userDao.deleteAll(historyMap.values());
    }

    /* 新建用户对组的关系 */
    private void createUser2Group(Long userId, Long groupId) {
        GroupUser groupUser = new GroupUser(userId, groupId);
        userDao.createObject(groupUser);
    }

    /* 新建用户对角色的关系 */
    private void createUser2Role(Long userId, Long roleId) {
        RoleUser user2Role = new RoleUser();
        user2Role.setRoleId(roleId);
        user2Role.setUserId(userId);
        userDao.createObject(user2Role);
    }
    
	public Map<String, Object> getInfo4CreateNewUser(Long groupId) {
        Group group = groupDao.getEntity(groupId);
        
        List<Group> groups = new ArrayList<Group>();
		groups.add(group);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("User2RoleTree", groupService.findEditableRoles()); // 操作人拥有的角色列表
        map.put("User2GroupExistTree", groups);
        map.put("User2RoleExistTree", groupDao.findRolesByGroupId(groupId)); // 新建用户继承所在组的角色列表
        map.put("disabled", group.getDisabled());
        return map;
    }

    public Map<String, Object> getInfo4UpdateExsitUser(Long userId) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("UserInfo", userDao.getEntity(userId));
        map.put("User2GroupExistTree", groupDao.findGroupsByUserId(userId));
        
        List<?> list = userDao.findRolesByUserId(userId);
        
        List<Role> roles1 = new ArrayList<Role>();
        List<Role> roles2 = new ArrayList<Role>();
        for( Object o : list ) {
        	Object[] objs  = (Object[]) o;
        	Role r = (Role) objs[0];
        	if( roles1.contains(r) ) continue;
        	
        	Long saId = (Long)objs[1];
			if( saId != null ) {
        		SubAuthorize sa = (SubAuthorize) userDao.getEntity(SubAuthorize.class, saId);
        		
        		// 有效期内，且所有人为当前指定用户
				if( sa != null && sa.getEndDate().after(new Date()) && userId.equals(sa.getOwnerId())) {
        			User buyer = userDao.getEntity( sa._buyer() );
        			Role copy = new Role();
            		String saer = (String) EasyUtils.checkTrue(buyer.getId().equals(sa.getOwnerId()), "", buyer.getUserName());
					copy.setName(r.getName() + "（" +saer + "转授）");  // 转授的角色在保存用户时忽略
            		roles2.add(copy);
            		
            		roles1.add(r);
        		}
        	} 
			else {
				roles2.add(r);
				roles1.add(r);
        	}
        }
		map.put("User2RoleExistTree", roles2);
		map.put("User2RoleTree", groupService.findEditableRoles());
        return map;
    }
	
	public void startOrStopUser(Long userId, Integer disabled, Long groupId) {
		if(Environment.getUserId().equals(userId)) {
            throw new BusinessException(EX.U_321);
        }
		
		User user = userDao.getEntity(userId);
		if ( ParamConstants.FALSE.equals(disabled) ) { // 启用用户
            if( isOverdue(userId) ) {
            	throw new BusinessException(EX.U_27);
            }
            
			// 同时也启用用户组
		    Group group = groupDao.getEntity(groupId);
		    
		    // 辅助用户组（当选中辅助用户下的停用用户启用时）
	        if(Group.ASSISTANT_GROUP_TYPE.equals(group.getGroupType())){ 
	            group = groupDao.findMainGroupByUserId(userId); // 找到用户所在主用户组
	        }
	        
	        // 如果主用户组状态为停用的话，则向上启用该主用户组及其所有父节点
	        if(ParamConstants.TRUE.equals(group.getDisabled())) {
	            groupService.startOrStopGroup(groupId, ParamConstants.FALSE);
		    }
		}
		 
        user.setDisabled(disabled);
        userDao.update(user);
	}
	
	public void moveUser(Long userId, Long groupId) {
        Group oldGroup = groupDao.findMainGroupByUserId(userId);
        List<?> list = userDao.getEntities("from GroupUser where userId=?1 and groupId=?2", userId, oldGroup.getId());
        groupDao.deleteAll(list);
        
        GroupUser groupUser = new GroupUser(userId, groupId);
        userDao.createObject(groupUser);
        
        checkDomainAdmin(userId);
        Group targetGroup = groupDao.getEntity(groupId);
        groupDao.evict(targetGroup);
		userDao.recordUserLog( userDao.getEntity(userId) , targetGroup, "move");
	}
	
	// 判断用户是否过期
	private boolean isOverdue(Long userId){
		List<?> list = userDao.getEntities(" from User o where o.id=?1 and o.accountLife < ?2", userId, new Date());
		return !EasyUtils.isNullOrEmpty(list);
	}
 
    public PageInfo getUsersByGroupId(Long groupId, Integer pageNum, String orderBy) {
        return groupDao.getUsersByGroup(groupId, pageNum, orderBy);
    }
 
    public PageInfo searchUser(Long groupId, String searchStr, int page) {
    	UMQueryCondition condition = new UMQueryCondition();
    	condition.getPage().setPageNum(page);
    	condition.setGroupId(groupId);
    	
    	condition.setLoginName(searchStr);
    	PageInfo result = groupDao.searchUser(condition);
    	if( result.getItems().size() > 0 ) {
    		return result;
    	}
    	
    	condition.setLoginName(null);
    	condition.setUserName(searchStr);
    	result = groupDao.searchUser(condition);
    	if( result.getItems().size() > 0 ) {
    		return result;
    	}
    	
    	condition.setUserName(null);
    	condition.setTelephone(searchStr);
    	result = groupDao.searchUser(condition);
    	if( result.getItems().size() > 0 ) {
    		return result;
    	}
    	
    	condition.setTelephone(null);
    	condition.setEmail(searchStr);
    	result = groupDao.searchUser(condition);
    	
    	return result;
    }
    
	/* 
	 * TODO 检查用户自身对转授出去的角色是否还有关联，如果没有了，则需要在转授信息里去除这些角色的关联信息。
	 * 1、creatorId --> list<subauth> --> list<roleId>
	 * 2、check userId & roleId 的关系是否还在，不在则删除转授权里的关联
	 */
	public void overdue() { 
		Date today = new Date();
		userDao.executeHQL("update User u set u.disabled = 1 where u.accountLife < ?1", today);
		userDao.executeHQL("update Role r set r.disabled = 1 where r.endDate < ?1", today);
		userDao.executeHQL("update SubAuthorize s set s.disabled = 1 where s.endDate < ?1", today);
	}
	
	/************************************* register module **************************************/
	
	public void regBusiness(User user, String domain) {
		regBusiness(user, domain, null);
	}
	public void regBusiness(User user, String domain, Long parent) {
		Group domainGroup = groupService.createDomainGroup(domain, parent);
    	Long newDomainGroupId = domainGroup.getId();
		user.setGroupId(newDomainGroupId);
		user.setDomain( domainGroup.getDomain() );
    	
    	// 默认创建一个客户组【customer】，小程序默认注册在各个域的客户组下
    	Group customerGroup = new Group();
    	customerGroup.setName(Group.CUSTOMER_GROUP);
    	customerGroup.setGroupType(Group.MAIN_GROUP_TYPE);
    	customerGroup.setParentId( newDomainGroupId );
    	groupService.createNewGroup(customerGroup, "", "");
    	
    	// 创建一行域扩展信息 DomainInfo
    	DomainInfo info = new DomainInfo();
    	info.setName( domainGroup.getName() );
    	info.setDomain( domainGroup.getDomain() );
    	info.setCreateTime(new Date());
    	info.setCreator( user.getLoginName() );
    	userDao.createObject(info);
    	
    	info.setPrefix(null); // 不再是domainGroupId 或 info.id, 没有意义
    	info.setCreator( user.getLoginName() );
    	userDao.update(info);
    	
    	// 域账号兼有开发者功能
    	if( "REG_BDEV".equals( ParamConfig.getAttribute(PX.REGABLE) ) ) {
			this.regDeveloper(user);
			setRole2User(user, UMConstants.DEV_ROLE_ID); // 给与开发者角色，默认开发者角色需要$开发域下才有
		} 
    	else {
    		// 判断用户是否已存在，则直接移动到当前新域下（适用于用户购买账号生成域）
    		Long userId = user.getId();
			if(userId != null) {
    			this.moveUser(userId, newDomainGroupId);
    			setRole2User(user, UMConstants.DOMAIN_ROLE_ID); // 给与域管理员角色（普通用户升级为独立域的场景）
    			userDao.recordUserLog( user, domainGroup, "设为域管理员" );
    			
    			// 刷新此用户的用户组织、角色、菜单等缓存
    			CacheHelper.flushCache(CacheLife.SHORT.toString(), "ByUserId(" +userId+ ")");
    	        CacheHelper.flushCache(CacheLife.SHORT.toString(), "ByLoginName(" +user.getLoginName()+ ")");
    	        CacheHelper.flushCache(CacheLife.SHORT.toString(), "getMenuItems");
    		} 
    		else {
    			this.regUser(user);
    		}
		}
	}
 
	private void setRole2User(User user, Long role) {
		RoleUser ru = new RoleUser();
 		ru.setRoleId(role);
 		ru.setUserId(user.getId());
 		roleDao.createObject(ru);
	}
	
	public void regUser(User user) {
		regUser(user, false);
	}
	
	public void regUser(User user, boolean regByOrder) {
		if( !regByOrder && ParamConfig.getAttribute(PX.REGABLE, "true").equals( Config.FALSE ) ) {
			throw new BusinessException(EX.U_46);
		}
		
		// 创建用户
		userDao.checkUserAccout(user);
        user.setOrignPassword( user.getPassword() );
        user.setAuthMethod(UMPasswordIdentifier.class.getName());
        user.setAccountLife(null);  // 默认有效期50年
        userDao.create(user);
        
        // 自注册用户默认加入到自注册用户组(特殊组)
        user.setGroupId( (Long) EasyUtils.checkNull(user.getGroupId(), UMConstants.SELF_REGISTER_GROUP_ID) );
        createUser2Group(user.getId(), user.getGroupId());
        
        // 所有用户默认授予“域管理员”角色，包括个人用户，其域为“自注册域”
        setRole2User(user, UMConstants.DOMAIN_ROLE_ID);
        
        userDao.recordUserLog(user, groupDao.findMainGroupByUserId(user.getId()), "reg");
 		
 		// 发送一条欢迎消息
 		Message msg = new Message();
		msg.setReceiverId(user.getId());
		msg.setReceiver(user.getLoginName());
		msg.setTitle("欢迎您来到它山石的世界！");
		msg.setContent(ParamManager.getValue("welcomeMsg", ""));
		roleDao.createObject(msg);
    }
	
	// 保证事务完整性
	public void regDeveloper(User user) {
		if( ParamConfig.getAttribute(PX.REGABLE_DEV, "true").equals( Config.FALSE ) ) {
			throw new BusinessException(EX.U_47);
		}
		
		// add to devGroup, 如果是企业兼开发者注册，则注册到企业域下
		Long devGroup = (Long) EasyUtils.checkNull(user.getGroupId(), UMConstants.DEV_GROUP_ID);
		user.setGroupId(devGroup); 
		this.regUser(user);
		
		// 借用Admin的权限完成下面资源的注册
		String token = userDao.mockLogin("Admin");
    	PermissionHelper ph = PermissionHelper.getInstance();
    	
    	Role rGroup = new Role();
		rGroup.setName(user.getUserName() + "角色组");
		rGroup.setIsGroup(ParamConstants.TRUE);
		rGroup.setParentId(UMConstants.ROLE_ROOT_ID);
		roleService.saveRoleGroup(rGroup);
		
		Role myRole = new Role(); 
		myRole.setName("$" + user.getUserName());
		myRole.setParentId(rGroup.getId());
		myRole.setStartDate(new Date());
        myRole.setEndDate( DateUtil.addYears(UMConstants.ROLE_LIFE_TIME) );
		roleService.saveRole2UserAndRole2Group(myRole, user.getId()+"", "");
		ph.createPermission(myRole.getId(), rGroup, UMConstants.ROLE_VIEW_OPERRATION, 2, 0, 0, RolePermission.class.getName());
		ph.createPermission(myRole.getId(), rGroup, UMConstants.ROLE_EDIT_OPERRATION, 2, 0, 0, RolePermission.class.getName());
    	
    	Report group1 = new Report();
    	group1.setName(user.getUserName() + "开发目录");
    	group1.setRemark("在此目录下创建你的功能报表吧，或者可以从给它换个名字开始。open it");
    	group1.setType(Report.TYPE0);
    	Long devReportRoot = reportService.getReportId("name", "我的报表", Report.TYPE0);
		group1.setParentId( devReportRoot );
    	reportService.createReport(group1);
    	reportService.startOrStop(group1.getId(), 0);
    	ph.createPermission(myRole.getId(), group1, Report.OPERATION_DISABLE, 2, 1, 0, ReportPermission.class.getName());
    	ph.createPermission(myRole.getId(), group1, Report.OPERATION_EDIT, 2, 1, 0, ReportPermission.class.getName());
    	ph.createPermission(myRole.getId(), group1, Report.OPERATION_VIEW, 2, 1, 0, ReportPermission.class.getName());
    	
    	Record group2 = new Record();
    	group2.setName(user.getUserName() + "开发目录");
    	group2.setRemark("在此目录下创建你的功能数据表吧，或者可以从给它换个名字开始。open it");
    	group2.setType(Report.TYPE0);
    	Long devRecordRoot = recordService.getRecordID("我的功能", Record.TYPE0);
		group2.setParentId( devRecordRoot );
    	recordService.createRecord(group2);
    	recordService.startOrStop(group2.getId(), 0);
    	ph.createPermission(myRole.getId(), group2, Record.OPERATION_CDATA, 2, 1, 0, RecordPermission.class.getName());
    	ph.createPermission(myRole.getId(), group2, Record.OPERATION_VDATA, 2, 1, 0, RecordPermission.class.getName());
    	ph.createPermission(myRole.getId(), group2, Record.OPERATION_EDATA, 2, 1, 0, RecordPermission.class.getName());
    	ph.createPermission(myRole.getId(), group2, Record.OPERATION_EDIT, 2, 0, 1, RecordPermission.class.getName());
    	
    	// 记录开发者和开发目录、角色组等关系
    	Param env = new Param();
    	env.setCode( user.getLoginName() );
    	env.setType(ParamConstants.SIMPLE_PARAM_MODE);
    	env.setHidden(ParamConstants.TRUE);
    	env.setValue(group1.getId() + "," + group2.getId() + "," + myRole.getId() + "," + rGroup.getId());
    	env.setParentId(ParamConstants.DEFAULT_PARENT_ID);
    	env.setSeqNo(1);
    	groupDao.createObject(env);
    	
    	// 其它：开发者只能在自己的目录下上传定制Html页面，在DMConstants.getReportTLDir()控制
    	
    	Context.destroyIdentityCard(token);
    }
}
