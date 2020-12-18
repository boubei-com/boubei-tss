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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.entity.RoleGroup;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.service.ISubAuthorizeService;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

@Service("SubAuthorizeService")
public class SubAuthorizeService implements ISubAuthorizeService {

	@Autowired private IRoleDao  roleDao;
	@Autowired private IGroupDao groupDao;	
	
	public List<?> listMySubauth(Long creatorId) {
		String hql = " from SubAuthorize where ?1 in (creatorId, buyerId) and endDate >= ?2 and disabled = 0 ";
		return roleDao.getEntities(hql, creatorId, new Date());
	}
	
	// 检查当前操作人是否有权限
	private void checkPermission(SubAuthorize strategy) {
		if( !Environment.isAdmin() && !Environment.getUserId().equals(strategy.getCreatorId()) ) {
			throw new BusinessException(EX.U_48);
		}
	}
	
	// 检查转授人是否对角色拥有权限
	private void checkPermission(Long roleId) {
		List<?> ownerRoles = roleDao.getSubAuthorizeableRoles( Environment.getUserId() );
		List<Object> ownerRoleIds =  EasyUtils.objAttr2List(ownerRoles, "id");
		if( !ownerRoleIds.contains(roleId) ) {
			throw new BusinessException(EX.U_48);
		}
	}

	public void deleteSubauth(Long id) {
		SubAuthorize strategy = (SubAuthorize) roleDao.getEntity(SubAuthorize.class, id);
		checkPermission(strategy);
		roleDao.deleteStrategy(strategy);
	}

	public void disable(Long id, Integer disabled) {
		SubAuthorize strategy = (SubAuthorize) roleDao.getEntity(SubAuthorize.class, id);
		checkPermission(strategy);
		
		if( ParamConstants.TRUE.equals(disabled) ) {
			strategy.setEndDate( DateUtil.today() );
		}
		strategy.setDisabled(disabled);
		roleDao.update(strategy);
	}
 
	public Map<String, Object> getSubauthInfo4Create() {
	    Long operatorId = Environment.getUserId();
	    
		List<?> groups = groupDao.getMainAndAssistantGroups(operatorId); // 用户组
		
        Map<String, Object> map = new HashMap<String, Object>();
		map.put("Rule2GroupTree", groups);
		map.put("Rule2UserTree", groupDao.getVisibleMainUsers(operatorId));
		map.put("Rule2RoleTree", roleDao.getSubAuthorizeableRoles(operatorId));
		return map;
	}

	public Map<String, Object> getSubauthInfo4Update(Long strategyId) {
	    Long operatorId = Environment.getUserId();
	    
        Map<String, Object> map = new HashMap<String, Object>();
		map.put("RuleInfo", roleDao.getEntity(SubAuthorize.class, strategyId));
		map.put("Rule2RoleTree", roleDao.getSubAuthorizeableRoles(operatorId));
		map.put("Rule2GroupTree", groupDao.getMainAndAssistantGroups(operatorId)); // 主用户组
		map.put("Rule2GroupExistTree", roleDao.getGroupsByStrategy(strategyId));
		map.put("Rule2UserExistTree",  roleDao.getUsersByStrategy(strategyId));
		map.put("Rule2RoleExistTree",  roleDao.getRolesByStrategy(strategyId));
		
		return map;
	}

	public List<?> getStrategyByCreator() {
	    return roleDao.getEntities("from SubAuthorize o where ?1 in (o.creatorId, -1)" , Environment.getUserId());
	}

	public void saveSubauth(SubAuthorize strategy, String userIds, String groupIds, String roleIds) {
		if(strategy.getId() == null) {
			strategy.setOwnerId( Environment.getUserId() );
		    roleDao.createObject(strategy);
		} 
		else {
		    roleDao.update(strategy);
		}
		
		// 在角色用户关系表中保存 策略对用户，策略对角色的信息 在角色用户组关系表中保存 策略对用户组，策略对角色的信息
        saveRule2Group(strategy, roleIds, groupIds);
        saveRule2User(strategy, roleIds, userIds);
	}
    
    /**
     * <p>
     * 在角色用户关系表中保存 策略对用户，策略对角色的信息。
     * 策略可以授予用户、用户组、也可以授予角色，或者三者兼有。
     * </p>
     * @param sa
     * @param roleIdsStr
     * @param userIdsStr
     */
    private void saveRule2User(SubAuthorize sa, String roleIdsStr, String userIdsStr) {
        Long subauthId = sa.getId();
		List<?> roleUsers = roleDao.getRoleUserByStrategy(subauthId);
        Map<String, RoleUser> historyMap = new HashMap<String, RoleUser>(); // 把老的转授记录放入一个map, 以"roleId_userId"为key
        for (Object temp : roleUsers) { 
            RoleUser roleUser = (RoleUser) temp;
            historyMap.put(roleUser.getRoleId() + "_" + roleUser.getUserId(), roleUser);
        }
        
        if ( !EasyUtils.isNullOrEmpty(roleIdsStr) && !EasyUtils.isNullOrEmpty(userIdsStr)) {
        	String[] roleIds = roleIdsStr.split(",");
            String[] userIds = userIdsStr.split(",");
            for (String roleId : roleIds) {
                for (String userId : userIds) {
                    saveRoleUser(historyMap, roleId, userId, sa);
                }
            }
        } 
        
        //老的转授记录中剩下的就是该删除的了
        roleDao.deleteAll(historyMap.values());
    }
    
    private void saveRoleUser(Map<String, RoleUser> historyMap, String roleId, String userId, SubAuthorize sa) {
        // 如果老的转授记录里面有，则从历史记录中移出
        RoleUser roleUser = historyMap.remove(roleId + "_" + userId); 
        
        // 如果老的转授记录里面没有，则新增 (注：如果是购买产生的策略，则不再创建新的roleUser)
        if (roleUser == null && sa.getBuyerId() == null) { 
            roleUser = new RoleUser();
            roleUser.setRoleId(Long.valueOf(roleId));
            roleUser.setUserId(Long.valueOf(userId));
            roleUser.setStrategyId(sa.getId());
            
            checkPermission( roleUser.getRoleId() );
            roleDao.createObject(roleUser);
        } 
    }

	/**
	 * <p>
	 * 在角色用户组关系表中保存 策略对用户组，策略对角色的信息
	 * </p>
	 * @param strategy
	 * @param roleIdsStr
	 * @param groupIdsStr
	 */
	private void saveRule2Group(SubAuthorize strategy, String roleIdsStr, String groupIdsStr) {
		List<?> roleGroups = roleDao.getRoleGroupByStrategy(strategy.getId());
		Map<String, RoleGroup> historyMap = new HashMap<String, RoleGroup>(); // 把老的转授记录做成一个map， 以"roleId_groupId"为key
		 for (Object temp : roleGroups) { 
			RoleGroup roleGroup = (RoleGroup) temp;
			historyMap.put(roleGroup.getRoleId() + "_" + roleGroup.getGroupId(), roleGroup);
		}
		 
        if ( !EasyUtils.isNullOrEmpty(roleIdsStr) && !EasyUtils.isNullOrEmpty(groupIdsStr)) {
        	String[] roleIds  = roleIdsStr.split(",");
            String[] groupIds = groupIdsStr.split(",");
            for (String roleId : roleIds) {
                for (String groupId : groupIds) {
                    saveRoleGroup(historyMap, roleId, groupId, strategy);
                }
            }
        } 
 
        // 老的转授记录中剩下的就是该删除的了
        roleDao.deleteAll(historyMap.values());
	}
    
    /** roleId, groupId 之一有可能为null */
    private void saveRoleGroup(Map<String, RoleGroup> historyMap, String roleId, String groupId, SubAuthorize strategy){
        // 如果老的转授记录里面有，则从老的转授记录中移出
        RoleGroup roleGroup = historyMap.remove(roleId + "_" + groupId); 
        
        //如果老的转授记录里面没有，则新增
        if (roleGroup == null) { 
            roleGroup = new RoleGroup();
            roleGroup.setRoleId(Long.valueOf(roleId));
            roleGroup.setGroupId(Long.valueOf(groupId));
            roleGroup.setStrategyId(strategy.getId());
            
            checkPermission( roleGroup.getRoleId() );
            roleDao.createObject(roleGroup);
        } 
    }
}
