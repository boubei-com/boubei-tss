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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IResourceTypeDao;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.entity.ResourceType;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.RoleGroup;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.permission.ResourcePermission;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.util.EasyUtils;
 
@Service("RoleService")
public class RoleService implements IRoleService {

	@Autowired private IRoleDao roleDao;
	@Autowired private IGroupDao groupDao; 
	@Autowired private IResourceTypeDao resourceTypeDao;
	@Autowired private ResourcePermission resourcePermission;
    
    public Role getRoleById(Long id) {
        return (Role) roleDao.getEntity(id);
    }
    
    public List<?> getAllVisiableRole() {
        return roleDao.getEntities("from Role r where r.id <> -1 order by r.decode");    
    }

    public List<?> getPlatformApplication() {
		String hql = "from Application o where o.applicationType = ?1 order by o.id desc";
		return roleDao.getEntities(hql, UMConstants.PLATFORM_SYSTEM_APP);
    }

    public List<?> getResourceTypeByAppId(String applicationId) {
		String hql = "select resourceTypeId as id, name from ResourceType a where a.applicationId = ?1 order by a.seqNo desc";
		return resourceTypeDao.getEntities(hql, applicationId);
    }
    
    public List<?> getAddableRoleGroups() {
        return roleDao.getEntities("from Role r where r.id > 0 and r.isGroup = 1 order by r.decode");      
    }
 
	public void delete(Long roleId) {
        // 如果将要操作的数量==能够操作的数量, 说明对所有组都有操作权限 
        String applicationId  = UMConstants.TSS_APPLICATION_ID;
        String resourceTypeId = UMConstants.ROLE_RESOURCE_TYPE_ID;
        String operationId    = UMConstants.ROLE_EDIT_OPERRATION;
        Long operatorId = Environment.getUserId();
        
        List<?> permitedList = resourcePermission.getSubResourceIds(applicationId, resourceTypeId, roleId, operationId, operatorId);
        List<?> allSubNodes = roleDao.getChildrenById(roleId);
        PermissionHelper.vsSize(permitedList, allSubNodes, "没有删除角色组权限，不能删除此节点！");
		
		 // 删除角色（组）及其所有子节点
		List<Role> subRoles = roleDao.removeRole(roleDao.getEntity(roleId));
		
		// Portal、CMS、DMS等其他基于平台应用的相关授权也需一并删除
		String hql = "from ResourceType o where o.applicationId = ?1 order by o.seqNo";
        for( Role temp : subRoles ){
            if(ParamConstants.TRUE.equals(temp.getIsGroup())){
               continue;
            }
            
            Long childRoleId = temp.getId();
			List<?> resourceTypes = resourceTypeDao.getEntities(hql, UMConstants.TSS_APPLICATION_ID);    
    		for(Object obj : resourceTypes) {
    			ResourceType rt = (ResourceType) obj;
				List<?> permissions = roleDao.getEntities("from " +rt.getPermissionTable()+ " where roleId = ?1 ", childRoleId);
				roleDao.deleteAll(permissions);
    		}
        }
	}
    
	public void disable(Long id, Integer disabled) {
        String appId = UMConstants.TSS_APPLICATION_ID;
        String resourceTypeId = UMConstants.ROLE_RESOURCE_TYPE_ID;
        Long operatorId = Environment.getUserId();
        
        List<Role> list;
        List<?> temp;
        String msg;
        if (disabled.equals(ParamConstants.FALSE)) {  
	        list = roleDao.getParentsById(id); // 启用一个节点上的所有父节点
	        
	        // 如果将要操作的数量 == 能够操作的数量, 说明对所有组都有操作权限
	        temp = resourcePermission.getParentResourceIds(appId, resourceTypeId, id, UMConstants.ROLE_EDIT_OPERRATION, operatorId);
	        msg = "对某个父节点没有启用操作权限，不能启用此节点！";
		} 
		else { 
            list = roleDao.getChildrenById(id); // 停用一个节点下的所有节点
            
            // 如果将要操作的数量==能够操作的数量, 说明对所有组都有操作权限,则返回true
            temp = resourcePermission.getSubResourceIds(appId, resourceTypeId, id, UMConstants.ROLE_EDIT_OPERRATION, operatorId);
            msg = "对部分子节点没有停用操作权限，不能停用此节点！";
		}
        PermissionHelper.vsSize(temp, list, msg);
		
        for (Role role : list) {
            if(role.getDisabled().equals(disabled)) continue;
            
			role.setDisabled(disabled);
			roleDao.update(role);
        }
	}
 
	public Map<String, Object> getInfo4CreateNewRole() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Role2UserTree", groupDao.getVisibleMainUsers(Environment.getUserId()));
		map.put("Role2GroupTree", getVisibleGroups());
		return map;
	}

	public Map<String, Object> getInfo4UpdateExistRole(Long roleId) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("RoleInfo", getRoleById(roleId));
	    map.put("Role2UserTree", groupDao.getVisibleMainUsers(Environment.getUserId()));
	    map.put("Role2GroupTree", getVisibleGroups());
		map.put("Role2GroupExistTree", roleDao.getGroupsByRoleId(roleId));
		map.put("Role2UserExistTree", roleDao.getUsersByRoleId(roleId));
		return map;
	}

    private List<?> getVisibleGroups() {
        return groupDao.getMainAndAssistantGroups(Environment.getUserId());
    }
    
    public void sortRole(Long id, Long targetId, int direction) {
        roleDao.sort(id, targetId, direction);
    }

	public void move(Long id, Long targetId) {
		Role movedRole = roleDao.getEntity(id);
		
		// 向自己的父节点移动，等于没有移动
		if (targetId.equals(movedRole.getParentId())) return;  
		
		// 节点向自己或者自己的子节点
        if(roleDao.getParentsById(targetId).contains(movedRole)) {
            throw new BusinessException(EX.U_28); 
        }
        
        movedRole.setSeqNo(roleDao.getNextSeqNo(targetId));
        movedRole.setParentId(targetId);
        
        roleDao.moveEntity(movedRole); //被拦截调整整个移动枝的decode值, 同时拦截资源补齐调整
        
        // 如果移动到的组是停用状态，那么被移动的角色（组）也需要停用
        Role target  = roleDao.getEntity(targetId);
        Integer targetState = target.getDisabled();
		if(target != null && ParamConstants.TRUE.equals(targetState)) {
			List<?> list = roleDao.getChildrenById(id);
            for (Object temp : list) {
                ((Role) temp).setDisabled(ParamConstants.TRUE);
            }
        }
	}

    public Role saveRoleGroup(Role entity) {
        if (entity.getId() == null) {
            entity.setSeqNo(roleDao.getNextSeqNo(entity.getParentId()));
            return roleDao.create(entity);
        }
            
        roleDao.refreshEntity(entity);
        return entity;
    }

    public void saveRole2UserAndRole2Group(Role role, String userIds, String groupIds) {
        if (role.getId() == null) { // 新建
        	Long parentId = (Long) EasyUtils.checkNull(role.getParentId(), UMConstants.ROLE_ROOT_ID);
            role.setParentId(parentId);
            role.setSeqNo(roleDao.getNextSeqNo(parentId));
            role = roleDao.create(role);
        } 
        else {
            roleDao.refreshEntity(role);
        }
        
        saveRole2User(role.getId(), userIds);   // 角色对用户
        saveRole2Group(role.getId(), groupIds); // 角色对组
    }

	private void saveRole2Group(Long roleId, String groupIdsStr) {
	    // 根据角色id找到角色-用户组的list（只涉及授权信息，不涉及转授）
		List<?> roleGroups = roleDao.getEntities("from RoleGroup o where o.roleId = ?1 and o.strategyId is null", roleId);
		Map<Long, RoleGroup> historyMap = new HashMap<Long, RoleGroup>(); // 以"groupId"为key
		for (Object temp : roleGroups) {
		    RoleGroup roleGroup = (RoleGroup) temp;
            historyMap.put(roleGroup.getGroupId(), roleGroup);
        }
 
        if ( !EasyUtils.isNullOrEmpty(groupIdsStr) ) {
            String[] groupIds = groupIdsStr.split(",");
            for (String temp : groupIds) {
                // 如果historyMap里面没有，则新增用户组对用户的关系; 有则从historyMap里remove掉，historyMap剩下的将被delete掉
                Long groupId = Long.valueOf(temp);
                if (historyMap.remove(groupId) == null) { 
                    RoleGroup roleGroup = new RoleGroup();
                    roleGroup.setRoleId(roleId);
                    roleGroup.setGroupId(groupId);
                    roleDao.createObject(roleGroup);
                } 
            }
        }
        
        // historyMap中剩下的就是该删除的了
        roleDao.deleteAll(historyMap.values());
        for(RoleGroup roleGroup : historyMap.values()) {
        	// 用户组已经不再拥有该角色，继承自该组而获得该角色的用户，如有转授该角色出去，则要收回
        	roleDao.deleteGroupSubAuthorizeInfo(roleGroup.getGroupId(), roleId);
        }
	}

	private void saveRole2User(Long roleId, String userIdsStr) {
	    // 根据角色id找到角色-用户的list（只涉及授权信息，不涉及转授
		List<?> roleUsers = roleDao.getEntities("from RoleUser o where o.roleId = ?1 and o.strategyId is null", roleId);
		Map<Long, RoleUser> historyMap = new HashMap<Long, RoleUser>(); // 以"groupId"为key
        for (Object temp : roleUsers) {
            RoleUser roleUser = (RoleUser) temp;
            historyMap.put(roleUser.getUserId(), roleUser);
        }
 
        if ( !EasyUtils.isNullOrEmpty(userIdsStr) ) {
            String[] userIds = userIdsStr.split(",");
            for (String temp : userIds) {
                // 如果historyMap里面没有，则新增用户组对用户的关系; 有则从historyMap里remove掉，historyMap剩下的将被delete掉
                Long userId = Long.valueOf(temp);
                if (historyMap.remove(userId) == null) { 
                    RoleUser roleUser = new RoleUser();
                    roleUser.setRoleId(roleId);
                    roleUser.setUserId(userId);
                    roleDao.createObject(roleUser);
                } 
            }
        }
		
        // historyMap中剩下的就是该删除的了
        roleDao.deleteAll(historyMap.values());
        for(RoleUser roleUser : historyMap.values()) {
			// 用户已经不再拥有该角色，如有转授该角色出去，则要收回
        	roleDao.deleteUserSubAuthorizeInfo(roleUser.getUserId(), roleId);
        }
	}
    
    // ===========================================================================
    // 展示外部资源的授权信息时需要的操作
    // 1.从um中取到当前用户的角色信息
    // ===========================================================================
 
    @SuppressWarnings("unchecked")
	public List<Long[]> getRoles4Permission(){
        String hql = "select distinct t.id.userId, t.id.roleId from ViewRoleUser t where t.id.userId = ?1";
        return (List<Long[]>) roleDao.getEntities(hql, Environment.getUserId() );  
    }
}
