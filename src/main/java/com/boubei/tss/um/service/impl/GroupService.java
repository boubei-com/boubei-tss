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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.RoleGroup;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.permission.ResourcePermission;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.util.EasyUtils;
 
@Service("GroupService")
public class GroupService implements IGroupService {

	@Autowired private IGroupDao groupDao;
	@Autowired private IRoleDao  roleDao;
	@Autowired private ResourcePermission resourcePermission;

    public Group getGroupById(Long id) {
        Group entity = groupDao.getEntity(id);
        groupDao.evict(entity);
		return entity;
    }

    public List<User> getUsersByGroupId(Long groupId) {
        return groupDao.getUsersByGroupId(groupId);
    }

    public List<?> findRolesByGroupId(Long groupId) {
        return groupDao.findRolesByGroupId(groupId);
    }

    @SuppressWarnings("unchecked")
	public List<Role> findEditableRoles() {
        List<Role> list = (List<Role>) roleDao.getEditableRoles();
        Set<Role> set = new LinkedHashSet<Role>(list);
        
        /* 1、加上因【域管理员】选用功能模块而得来的模块角色，以用来给域内用户定岗
         * （注：只适合定价为0的免费模块，付费的需要域管理员通过使用转授策略把购买的模块角色赋予域用户）
         */
        String hql = "select o from ModuleDef o, ModuleUser mu where mu.moduleId = o.id and mu.userId = ?1 and o.status in ('opened') ";
        Long userId = Environment.getNotnullUserId();
		List<ModuleDef> modules = (List<ModuleDef>) roleDao.getEntities(hql, userId);  // 非域管理员获取不到此处角色
        for(ModuleDef module : modules ) {
        	List<String> freeRoles = EasyUtils.toList( module.getRoles_free() ); // 模块的免费角色
        	
        	Double price = EasyUtils.obj2Double(module.getPrice());
        	boolean moduleFreeUse = price <= 0; 
        	if( moduleFreeUse ) { 
        		freeRoles.addAll( EasyUtils.toList( module.getRoles() ) );  // 免费模块的收费角色
        	}
        	
    		for(String role : freeRoles) {
				Role r = roleDao.getEntity( EasyUtils.obj2Long(role) );
				if(r != null && r.getId() != UMConstants.ANONYMOUS_ROLE_ID) {
					roleDao.evict(r);
    				r.setName( r.getName() + " - " + module.getCode() );
    				set.add( r );
				}
    		}
        }
        
        /* 2、域用户（人事等非域管理员角色）可以自行设置自己或别人的角色（freeUse），需要用户所在域选择的模块包含有此角色
         * （ 注：免费角色允许域管理员设置给其它成员，不允许其它成员自主获得；其它成员自主只能获取标记为freeUse的免费角色 ） */
        Object domainUserIds = Environment.getInSession(SSOConstants.USERIDS_OF_DOMAIN);
		hql = "select o from ModuleDef o, ModuleUser mu where mu.moduleId = o.id " +
        		" and mu.userId in (" +EasyUtils.checkNull(domainUserIds, Anonymous._ID)+ ") and o.status in ('opened') ";
		modules = (List<ModuleDef>) roleDao.getEntities(hql);
        for(ModuleDef module : modules ) {
        	List<String> roles = EasyUtils.toList( module.getRoles() );
        	roles.addAll( EasyUtils.toList( module.getRoles_free() ) );
        	
    		for(String role : roles) {
				Role r = roleDao.getEntity( EasyUtils.obj2Long(role) );
				if(r != null && r.getDescription().indexOf("freeUse") >= 0) {
					roleDao.evict(r);
    				r.setName( r.getName() + " - " + module.getCode() );
    				set.add( r );
				}
    		}
        }
 
		return new ArrayList<Role>(set);
    }

    public List<?> findGroups() {
        return groupDao.getMainAndAssistantGroups( Environment.getUserId() );
    }
    
    public Object[] getAssistGroupsByOperationId(String operationId) {
        return getGroupsByGroupTypeAndOperationId(Group.ASSISTANT_GROUP_TYPE, operationId);
    }
    
    public Object[] getMainGroupsByOperationId(String operationId) {
        return getGroupsByGroupTypeAndOperationId(Group.MAIN_GROUP_TYPE, operationId);
    }
    
    private Object[] getGroupsByGroupTypeAndOperationId(Integer groupType, String operationId) {
    	Long operatorId = Environment.getUserId();
    	
        List<?> groups = groupDao.getGroupsByType(operatorId, operationId, groupType);  
        List<Long> groupIds = new ArrayList<Long>();
        for( Object temp : groups ){
            Group group = (Group) temp;
            groupIds.add(group.getId());
        }
        List<?> parentGroups = groupDao.getParentGroupByGroupIds(groupIds, operatorId, UMConstants.GROUP_VIEW_OPERRATION);
        return new Object[] { groupIds, parentGroups };
    }
 
    public Group createDomainGroup(String domain, Long parent) {
    	parent = (Long) EasyUtils.checkNull(parent, UMConstants.DOMAIN_ROOT_ID);
		if (groupDao.getEntity(parent) == null ) {
			throw new BusinessException(EX.parse(EX.U_53, parent));
		}
		
		List<?> list = groupDao.getEntities("from Group where ?1 in (domain, name)", domain);
		if(list.size() > 0) {
			throw new BusinessException(EX.parse(EX.U_19, domain));
		}
		
		Group group = new Group();
		group.setGroupType(Group.MAIN_GROUP_TYPE);
		group.setDomainRoot(ParamConstants.TRUE);
		group.setName(domain);
		group.setParentId(parent);
		group.setSeqNo(groupDao.getNextSeqNo(parent));
		groupDao.saveGroup(group);
		
		fixDomain(domain, group, true);
		
		return group;
    }

	/**
	 * 创建时，OperateInfoInterceptor 会执行 IOperatable.setDomain(Environment.getDomain())；需要单独再保存一遍domain信息
	 */
	private void fixDomain(String domain, Group group, boolean isDomainGroup) {
		Integer groupType = group.getGroupType();
		if( domain == null && Group.MAIN_GROUP_TYPE.equals(groupType) ) return;
		
	    // 如果是辅助组，则取创建人的域作为辅助组所属域
		if ( Group.ASSISTANT_GROUP_TYPE.equals(groupType) ) {
	    	domain = Environment.getDomainOrign();
	    } 
		else {
	    	// 控制注册时域名必须为英文字母或数字，方便小程序传递域参数; 或者字符数 > 7
		    if( isDomainGroup && (!Pattern.compile("[a-z|A-Z|0-9]+").matcher(domain).matches() || domain.length() > 7) ) {
		    	String _domain = SerialNOer.get("Dxxxx", true);
		    	List<?> list = groupDao.getEntities("from Group where domain = ?1", _domain);
		    	domain = (String) EasyUtils.checkTrue(list.isEmpty(), _domain, "G" + group.getId());
		    }
	    }
	        
		group.setDomain(domain);
		groupDao.saveGroup(group);
	}
    
	public void createNewGroup(Group group, String userIdsStr, String roleIdsStr) {
		Long parentId = group.getParentId();
		Group parent = groupDao.getEntity(parentId);
		
		group.setDomain(parent.getDomain());
		group.setDomainRoot( ParamConstants.FALSE );
		group.setSeqNo(groupDao.getNextSeqNo(parentId));
		group.setDisabled(parent.getDisabled());
		groupDao.saveGroup(group);
		
		fixDomain( parent.getDomain(), group, false);
        
		saveGroupToUser(group.getId(), userIdsStr);
		saveGroupToRole(group.getId(), roleIdsStr);
	}
    
    public void editExistGroup(Group group, String userIdsStr, String roleIdsStr) {
        groupDao.refreshEntity(group);
        
        saveGroupToRole(group.getId(), roleIdsStr);
        
        /* 主用户组中组对用户的关系不能随便更改，所以不能调用saveGroupToUser
         * 只有辅助用户组可以选择组对应的用户(group2UserExistTree有效)，可以调用saveGroupToUser方法 。
         */
        Integer groupType = group.getGroupType();
		if (Group.ASSISTANT_GROUP_TYPE.equals(groupType)) {
			saveGroupToUser(group.getId(), userIdsStr);
		}
    }
    
    /** 组对用户：先把该组对应的用户都找到，再把提交上来的用户和找到的数据比较，多的做增加操作。 */
    private void saveGroupToUser(Long groupId, String userIdsStr) {
        List<?> group2Users = groupDao.findGroup2UserByGroupId(groupId);
        
        Map<Long, Object> historyMap = new HashMap<Long, Object>(); //把老的组对用户记录做成一个map，以"userId"为key
        for (Object temp : group2Users) {
            GroupUser groupUser = (GroupUser) temp;
            historyMap.put(groupUser.getUserId(), groupUser);
        }
        
        if ( !EasyUtils.isNullOrEmpty(userIdsStr) ) {
            String[] userIds = userIdsStr.split(",");
            for (String temp : userIds) {
                // 如果historyMap里面没有，则新增用户组对用户的关系；如果historyMap里面有，则从历史记录中移出；剩下的将被删除
                Long userId = Long.valueOf(temp);
                if (historyMap.remove(userId) == null) { 
                    GroupUser group2User = new GroupUser(userId, groupId);
                    groupDao.createObject(group2User);
                } 
            }
        }
        
        // historyMap中剩下的就是该删除的了
        groupDao.deleteAll(historyMap.values());
    }
    
    /** 组对角色。先把该组对应的角色都找到，再把提交上来的用户和找到的数据比较，多的做增加操作 */
    private void saveGroupToRole(Long groupId, String roleIdsStr) {
        List<?> group2Roles = groupDao.findGroup2RoleByGroupId(groupId);
        
        Map<Long, Object> historyMap = new HashMap<Long, Object>();// 把老的组对角色记录做成一个map，以"roleId"为key
        for (Object temp : group2Roles) {
            RoleGroup roleGroup = (RoleGroup) temp;
            historyMap.put(roleGroup.getRoleId(), roleGroup);
        }
        
        // 检查操作人是否对角色有管理权限
        List<?> editableRoles = findEditableRoles();
        List<Long> editableRoleIds = new ArrayList<Long>();
        for(Object o : editableRoles) {
        	editableRoleIds.add( ((Role)o).getId() );
        }
        
        if ( !EasyUtils.isNullOrEmpty(roleIdsStr) ) {
            String[] roleIds = roleIdsStr.split(",");
            for (String temp : roleIds) {
                // 如果historyMap里面没有，则新增用户组对角色的关系; 如果historyMap里面有，则从历史记录中移出；剩下的将被删除
                Long roleId = Long.valueOf(temp);
                if (historyMap.remove(roleId) == null && editableRoleIds.contains(roleId)) {
                    RoleGroup role2Group = new RoleGroup();
                    role2Group.setRoleId(roleId);
                    role2Group.setGroupId(groupId);
                    groupDao.createObject(role2Group);
                } 
            }
        }
        
        // historyMap中剩下的就是该删除的了
        groupDao.deleteAll(historyMap.values());
    }

    public void sortGroup(Long groupId, Long toGroupId, int direction) {
        groupDao.sort(groupId, toGroupId, direction);
    }
    
    public void move(Long id, Long toGroupId) {
        Group group = groupDao.getEntity(id);
        Group target = groupDao.getEntity(toGroupId);
        if( !group.getGroupType().equals(target.getGroupType()) ) {
        	throw new BusinessException(EX.U_24);
        }
        
        group.setParentId(toGroupId);
        group.setSeqNo(groupDao.getNextSeqNo(toGroupId));
                   
        groupDao.moveEntity(group);
    }

    public void startOrStopGroup(Long groupId, Integer disabled) {
        String applicationId = UMConstants.TSS_APPLICATION_ID;
        if (ParamConstants.TRUE.equals(disabled)) { // 停用            
            String operationId = UMConstants.GROUP_EDIT_OPERRATION;
            checkSubGroupsPermission(groupId, operationId, EX.U_22);
            
            stopGroup(groupId);
        } 
        else { // 启用一个组,该组的父节点也得全部启用
            List<?> groups = groupDao.getParentsById(groupId);
            String operationId = UMConstants.GROUP_EDIT_OPERRATION;
            List<?> canDoGroups = resourcePermission.getParentResourceIds(applicationId, UMConstants.GROUP_RESOURCE_TYPE_ID, groupId, operationId, 
                    Environment.getUserId());
 
            PermissionHelper.vsSize(canDoGroups, groups, EX.U_23);
            
            for(Iterator<?> it = groups.iterator();it.hasNext();){
                Group group = (Group) it.next();
                if(group.getDisabled().equals(ParamConstants.TRUE)) {
                    group.setDisabled(ParamConstants.FALSE);
                    groupDao.update(group); 
                }
            }
        }
    }
    
    // 停用组以及组下的子组和所有的用户
    private void stopGroup(Long groupId) {
        Group group = groupDao.getEntity(groupId);
        groupDao.executeHQL("update Group set disabled = ?1 where decode like ?2", ParamConstants.TRUE, group.getDecode() + "%");
       
        /* 
         * 停用主用户组，需要停用组下的用户。
         * 停用辅助用户组，不停用用户，因为辅助用户组当中的用户是从主用户组中选取的.
         */
        Integer groupType = group.getGroupType();
        if ( Group.MAIN_GROUP_TYPE.equals(groupType) ) {
            List<User> users = groupDao.getUsersByGroupIdDeeply(groupId);
            for( User user : users) {
                if(!ParamConstants.TRUE.equals(user.getDisabled())){
                    user.setDisabled(ParamConstants.TRUE);
                }
            }       
        }
    }

    public void deleteGroup(Long groupId) {
        if( groupDao.isOperatorInGroup(groupId, Environment.getUserId()) ) {
            throw new BusinessException(EX.U_20);
        }
        
        Group group = groupDao.getEntity(groupId);
        String operationId = UMConstants.GROUP_EDIT_OPERRATION;
        checkSubGroupsPermission(groupId, operationId, EX.U_21);
        
        // 辅助用户组里面的用户都是从主用户组选过来的,所以删除的时候只是删除辅助用户组的结构，里面的用户是不删的
        if ( Group.ASSISTANT_GROUP_TYPE.equals(group.getGroupType()) ) { // 辅助用户组
            groupDao.removeAssistmentGroup(group);
        } 
        else {// 删除主用户组和其他用户组
            groupDao.removeGroup(group);
        }
    }
    
    // 判断对所有子节点是否都拥有指定的操作权限
    private void checkSubGroupsPermission(Long groupId, String operationId, String msg) {
        String applicationId = UMConstants.TSS_APPLICATION_ID;
        List<?> allGroups = groupDao.getChildrenById(groupId);
        List<?> canDoGroups = resourcePermission.getSubResourceIds(applicationId, UMConstants.GROUP_RESOURCE_TYPE_ID, 
                groupId, operationId, Environment.getUserId());
        
        //如果将要操作的数量==能够操作的数量,说明对所有组都有操作权限,则返回true
        PermissionHelper.vsSize(canDoGroups, allGroups, msg);
    }
  
	public List<?> getVisibleSubGroups(Long groupId){
		return groupDao.getVisibleSubGroups(groupId);
	}
}
