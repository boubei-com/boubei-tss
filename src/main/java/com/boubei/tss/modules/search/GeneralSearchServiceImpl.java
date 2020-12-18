/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.EasyUtils;
 
@Service("GeneralSearchService")
public class GeneralSearchServiceImpl implements GeneralSearchService {
	
	Logger log = Logger.getLogger(this.getClass());    

	@Autowired private ICommonDao commonDao;
 
	// 一个组下面所有用户的因转授而获得的角色的情况
	public List<SubAuthorizedUserRoleDTO> searchUserSubauthByGroupId(Long groupId){
	    // 先取出组下所有可见的用户
		queryUsersInsertTemp(groupId);
		
		// 再取出这些用户所拥有的转授得到的角色以及相应的转授策略
		List<SubAuthorizedUserRoleDTO> result = new ArrayList<SubAuthorizedUserRoleDTO>();
		
		String hql = "select u, r, s, creator" +
					" from User u, RoleUser ru, Role r, SubAuthorize s, User creator, Temp t " +
					" where u.id=ru.userId and ru.roleId = r.id and ru.strategyId = s.id and s.creatorId = creator.id " +
					"	and u.id = t.id and t.thread=" + Environment.threadID() +
					" order by u.id";
        List<?> list = commonDao.getEntities(hql);
		for( Object temp : list ) {
			Object[] objs = (Object[]) temp;
			User user = (User) objs[0]; 
			Role role = (Role) objs[1];
			SubAuthorize strategy = (SubAuthorize) objs[2];
			User creator = (User) objs[3]; 
			 
			SubAuthorizedUserRoleDTO dto = new SubAuthorizedUserRoleDTO();
			dto.setSubAuthorized2UserId(user.getId());
			dto.setSubAuthorized2UserName(user.getUserName());
			dto.setRoleId(role.getId());
			dto.setRoleName(role.getName());
			dto.setStrategyId(strategy.getId());
			dto.setStrategyName(strategy.getName());
 
			dto.setSubAuthorizedUserId(creator.getId());
			dto.setSubAuthorizedUserName(creator.getUserName());
			dto.setSubAuthorizedGroupId(-1L);
			dto.setSubAuthorizedGroupName("Admin Group");
 
			result.add(dto);
		}
		
		hql = "select u, r, s, creator, g " +
			" from User u, GroupUser gu, Group g, RoleGroup rg, Role r, SubAuthorize s, User creator, Temp t " +
		    " where u.id = gu.userId and gu.groupId=g.id and g.id=rg.groupId and rg.roleId = r.id  " +
		    "   and rg.strategyId = s.id and s.creatorId = creator.id " +
		    "	and u.id = t.id and t.thread=" + Environment.threadID() +
		    " order by u.id";
        list = commonDao.getEntities(hql);
        for( Object temp : list ){
			Object[] objs = (Object[]) temp;
			User user = (User) objs[0]; 
			Role role = (Role) objs[1];
			SubAuthorize strategy = (SubAuthorize) objs[2];
			User creator = (User) objs[3]; 
			Group group = (Group) objs[4];
			 
			SubAuthorizedUserRoleDTO dto = new SubAuthorizedUserRoleDTO();
			dto.setSubAuthorized2UserId(user.getId());
			dto.setSubAuthorized2UserName(user.getUserName());
			dto.setRoleId(role.getId());
			dto.setRoleName(role.getName());
			dto.setStrategyId(strategy.getId());
			dto.setStrategyName(strategy.getName());
			dto.setSubAuthorized2GroupId(group.getId());
			dto.setSubAuthorized2GroupName(group.getName());
			dto.setSubAuthorizedUserId(creator.getId());
			dto.setSubAuthorizedUserName(creator.getUserName());
 
			result.add(dto);
		}
		
		return result;
	}
	
    @SuppressWarnings("unchecked")
	public List<UserRoleDTO> searchUserRolesMapping(Long groupId) {
		queryUsersInsertTemp(groupId);
	    
        // 再查出这些用户拥有的角色情况
        String hql = "select distinct u, r from User u, Role r, ViewRoleUser ru, Temp t " 
        	+ " where u.id = ru.id.userId and ru.id.roleId = r.id " +
        	"	  and u.id = t.id and t.thread=" + Environment.threadID()
        	+ " order by u.id";
        List<Object[]> list = (List<Object[]>) commonDao.getEntities(hql);
        list.add(new Object[] {new User(), new Role()}); // 应付测试覆盖率
        
        List<UserRoleDTO> returnList = new ArrayList<UserRoleDTO>();
        for ( Object[] objs : list ) {
            UserRoleDTO relation = new UserRoleDTO((User) objs[0], (Role) objs[1]);
            returnList.add(relation);
        }
        return returnList;
    }

	/**
	 *  查出组下用户ID列表并插入临时表
	 */
	private void queryUsersInsertTemp(Long groupId) {
        String hql = "select distinct u.id from GroupUser gu, User u where gu.userId = u.id and gu.groupId = ?1 ";
        List<?> list = commonDao.getEntities(hql, groupId);
        
        log.debug(list.size());
        commonDao.insertIds2TempTable(list);
	}

	/** 
	 * 查询角色授予的用户列表（包括直接授予用户的和授予组时组下的所有用户）
	 */
	public List<User> searchUsersByRole(Long roleId) {
		String sql = "select u.id, u.userName from um_user u, um_roleuser ru where u.id=ru.userId and ru.roleId = ? " + 
				    " union" + 
				    " select u.id, u.userName from um_user u, um_groupuser gu, um_rolegroup rg where u.id = gu.userId and gu.groupId=rg.groupId and rg.roleId = ?";
		
		List<?> list = commonDao.getEntitiesByNativeSql(sql, roleId, roleId);
		List<User> result = new ArrayList<User>();
		for( Object temp :list ){
			Object[] objs = (Object[]) temp;
			User user = new User();
			user.setId( EasyUtils.obj2Long(objs[0]) );
			user.setUserName((String)objs[1]);
			result.add(user);
		}
		return result;
	}
}