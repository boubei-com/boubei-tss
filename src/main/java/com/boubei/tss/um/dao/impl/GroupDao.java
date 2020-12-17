/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.TreeSupportDao;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.persistence.pagequery.PaginationQueryByHQL;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.entity.permission.GroupPermission;
import com.boubei.tss.um.helper.UMQueryCondition;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.util.EasyUtils;
 
@Repository("GroupDao")
public class GroupDao extends TreeSupportDao<Group> implements IGroupDao {

    public GroupDao() {
		super(Group.class);
	}

    public Group saveGroup(Group group) {
    	// 因 DecodeInterceptor 里保存了一次了，此时group已经是PO状态，再merge会报乐观锁
		return group;
	}
 
	public Group removeGroup(Group group) {
        List<?> groups = getChildrenById(group.getId()); //列表中包含了Group本身
        List<User> users = getUsersByGroupIdDeeply(group.getId());
        
        for(User user : users) {
            if(user.getLogonCount() > 0) {
                throw new BusinessException( EX.parse(EX.U_322, group.getName()) );
            }
            
            Object[] params = new Object[]{ user.getId() };
			deleteAll(getEntities("from GroupUser gu where gu.userId = ?", params));
            deleteAll(getEntities("from RoleUser ru where ru.userId = ? and ru.strategyId is null", params));
        }
        
        for(Iterator<?> it = groups.iterator(); it.hasNext();){
            Group temp = (Group) it.next();
            deleteAll(findGroup2UserByGroupId(temp.getId()));
            deleteAll(findGroup2RoleByGroupId(temp.getId()));
        }
        
        deleteAll(users);
        deleteAll(groups);
        return group;
	}
    
    public Group removeAssistmentGroup(Group group) {
        deleteAll(getChildrenById(group.getId()));
        return group;
    }

	public List<?> findRolesByGroupId(Long groupId) {
		String hql = "select distinct r from RoleGroup rg, Role r where rg.roleId = r.id and rg.groupId = ? and rg.strategyId is null ";
		return getEntities(hql, groupId);
	}

	public List<?> findGroup2UserByGroupId(Long groupId) {
        return getEntities("from GroupUser gu where gu.groupId = ?", groupId);
	}

	public List<?> findGroup2RoleByGroupId(Long groupId) {
        return getEntities("from RoleGroup rg where rg.groupId = ? and rg.strategyId is null ", groupId);
	}
 
	public Group findMainGroupByUserId(Long userId){
        // 如此取出来的组唯一，即用户所在的主用户组
		String hql = "select g from GroupUser gu, Group g " +
				" where gu.groupId = g.id and gu.userId = ?1 and g.groupType = ?2" +
				" order by g.decode ";
		List<?> list = getEntities(hql, userId, Group.MAIN_GROUP_TYPE);
        return list.size() > 0 ? (Group)list.get(0) : null;
	}
    
    public List<?> findGroupsByUserId(Long userId) {
        String hql = "select distinct g from GroupUser gu, Group g where gu.groupId = g.id and gu.userId = ? ";
        return getEntities(hql, userId);
    }
    
    public List<?> getFatherGroupsByUserId(Long userId){
        Group mainGroup = findMainGroupByUserId(userId);
        List<Group> parents = getParentsById(mainGroup.getId(), UMConstants.MAIN_GROUP_ID);
		return parents.subList(1, parents.size()); // 去掉"主用户组"
    }
 
	public List<?> getVisibleSubGroups(Long groupId){
		Group group = getEntity(groupId);
        
        String hql = PermissionHelper.permissionHQL(entityName, GroupPermission.class.getName(), " and o.decode like ? ", true);
        return getEntities(hql, Environment.getUserId(), UMConstants.GROUP_VIEW_OPERRATION, group.getDecode() + "%" );
	}
 
	public List<?> getMainAndAssistantGroups(Long operatorId) {
		List<Group> groups = new ArrayList<Group>(); 
	    groups.addAll(getGroupsByType(operatorId, UMConstants.GROUP_VIEW_OPERRATION, Group.MAIN_GROUP_TYPE));
	    groups.addAll(getGroupsByType(operatorId, UMConstants.GROUP_VIEW_OPERRATION, Group.ASSISTANT_GROUP_TYPE));
	    
	    // 可单独指定某个角色为系统管理员，默认为Admin
		Long adminRoleId = EasyUtils.obj2Long( ParamManager.getValue(PX.ADMIN_ROLE, "-1") );
		boolean isAdmin = Environment.getOwnRoles().contains(adminRoleId);
	    if( isAdmin ) {
        	return groups; // 设置为系统级管理员的，需要能看到全部被授权的用户组织
        }
        
        return filterGroups(operatorId, groups);
	}

	/* 
	 * 按操作用户对结果集进行过滤，只留下自己所在主组及其子组。 注：#自注册用户组#放开
     * eg:浙江分公司的用户只能看到浙分下面的组织，哪怕给他授权了其它分公司
     */
	private List<Group> filterGroups(Long operatorId, List<Group> groups) {
        Group mainGroup = getMainGroup(operatorId);
        List<Group> result = new ArrayList<Group>();
        for(Object temp : groups) {
			Group group = (Group) temp;
        	Integer groupType = group.getGroupType();
			if( Group.MAIN_GROUP_TYPE.equals(groupType) 
					&& !group.getDecode().startsWith(mainGroup.getDecode())
					&& !UMConstants.SELF_REGISTER_GROUP_ID.equals(group.getId())) {
        		continue;
        	}
			
			String userDomain = Environment.getDomainOrign();
			if( userDomain == null || userDomain.equals(group.getDomain()) 
					|| UMConstants.ASSISTANT_GROUP_ID.equals(group.getId())) {
				result.add(group);
			}
        }
		return result;
	}
	
	private Group getMainGroup(Long userId) {
        String hql = "select distinct g from Group g, GroupUser gu " +
        		" where g.id = gu.groupId and gu.userId = ? and g.groupType = ?";
        List<?> list = getEntities(hql, userId, Group.MAIN_GROUP_TYPE);
        
        return (Group) list.get(0);
	}
	
	public List<Group> getGroupsByType(Long operatorId, String operationId, Integer groupType) {
        String hql = PermissionHelper.permissionHQL(entityName, GroupPermission.class.getName());
        List<?> allGroups = getEntities(hql, operatorId, operationId);
        
        List<Group> resultList = new ArrayList<Group>();
        for( Object temp : allGroups ){
            Group group = (Group) temp;
            if( groupType.equals(group.getGroupType()) ) {
            	resultList.add(group);
            }
        }
        
        return filterGroups(operatorId, resultList);
	}
	
	@SuppressWarnings("unchecked")
	public List<?> getParentGroupByGroupIds(List<Long> groupIds, Long operatorId, String operationId) {
        if( EasyUtils.isNullOrEmpty(groupIds)) return new ArrayList<Group>();
        
		String hql = "select distinct o " + PermissionHelper.formatHQLFrom(entityName, GroupPermission.class.getName()) + " , Group child " +
		        PermissionHelper.permissionConditionII() + 
		        " and child.id in (:groupIds) and child.decode like o.decode||'%' and o.id not in (-1, -7)" + 
		        PermissionHelper.ORDER_BY;
		
		List<?> pgroups = getEntities(hql, 
				new String[]{"operatorId", "operationId", "groupIds"}, 
				new Object[]{operatorId, operationId, groupIds} );
		
		return filterGroups(operatorId, (List<Group>) pgroups);
	}

	public List<?> getVisibleMainUsers(Long operatorId) {
	    // 先查出有查看权限的用户组
	    List<Group> groups = new ArrayList<Group>(); 
	    groups.addAll(getGroupsByType(operatorId, UMConstants.GROUP_VIEW_OPERRATION, Group.MAIN_GROUP_TYPE));
	    groups.addAll(getGroupsByType(operatorId, UMConstants.GROUP_VIEW_OPERRATION, Group.ASSISTANT_GROUP_TYPE));
	    
	    List<Long> groupIds = new ArrayList<Long>();
	    for(Object temp : groups) {
	        groupIds.add(((Group) temp).getId());
	    }
	    
	    if(groupIds.isEmpty()) {
	        return new ArrayList<User>();
	    }
	    
        String hql = " select distinct u from User u, GroupUser gu where u.id = gu.userId  and gu.groupId in (:groupIds) ";
        return getEntities(hql, new String[] {"groupIds"}, new Object[]{ groupIds });
	}

	public List<User> getUsersByGroupIdDeeply(Long groupId){
		List<Group> sonGroups = this.getChildrenById(groupId);
		List<Long> sonGroupIds = new ArrayList<Long>();
		for(Group son : sonGroups) {
			sonGroupIds.add(son.getId());
		}
		
        return getUsersByGroupIds(sonGroupIds);
	}

	public List<User> getUsersByGroupIds(Collection<Long> groupIds){
        if( EasyUtils.isNullOrEmpty(groupIds) ) return new ArrayList<User>();
        
        String hql = "select distinct u, g.id as groupId, g.name as groupName,g.domain " +
        		" from User u, GroupUser gu, Group g " + 
        		" where u.id = gu.userId and gu.groupId = g.id and g.id in (:groupIds) ";

        List<?> list = getEntities(hql, new String[]{"groupIds"}, new Object[]{groupIds});
		return fillGroupInfo2User(list);
	}

	public List<User> getUsersByGroupId(Long groupId) {
		String hql = "select distinct u, g.id as groupId, g.name as groupName,g.domain" +
				" from User u, GroupUser gu, Group g " +
                " where u.id = gu.userId and gu.groupId = g.id and g.id = ? ";
		return fillGroupInfo2User(getEntities(hql, groupId));
	}

    /**
     * 将用户所属的用户组ID，NAME设置到用户对象中
     * @param users
     * @return
     */
    private List<User> fillGroupInfo2User(List<?> users){
        List<User> result = new ArrayList<User>();
        if( EasyUtils.isNullOrEmpty(users) ) return result;
        
        for (Object temp : users) {
            Object[] objs = (Object[]) temp;
            User user = (User) objs[0];
            user.setGroupId( EasyUtils.obj2Long(objs[1]) );
            user.setGroupName( (String) objs[2] );
            user.setDomain( (String) objs[3] );
            result.add(user);
        }    
        return result;
    }

	private List<Long> getChildrenGroupIds(Long groupId){
    	List<?> groups = getChildrenById(groupId);
    	List<Long> groupIds = new ArrayList<Long>();
        for( Object temp : groups ){
            Group group = (Group) temp;
            groupIds.add(group.getId());
        }
        return groupIds;
	}

    public boolean isOperatorInGroup(Long groupId, Long operatorId) {
        List<Long> groupIds = getChildrenGroupIds(groupId);
        if( EasyUtils.isNullOrEmpty(groupIds) ) return false;
        
        String hql = "select distinct gu.id from GroupUser gu where gu.groupId in (:groupIds) and gu.userId = :userId";
        List<?> list = getEntities(hql, new String[]{"groupIds", "userId"}, new Object[]{groupIds, operatorId});
        return !list.isEmpty();
    }
    
    //******************************************* 按组或按查询条件查询用户 *******************************************

    public PageInfo getUsersByGroup(Long groupId, Integer pageNum, String...orderBy) {
        String hql = "select distinct u, g.id as groupId, g.name as groupName, g.domain "
        		+ " from User u, GroupUser gu, Group g" 
        		+ " where u.id = gu.userId and gu.groupId = g.id and g.id = :groupId ";
        UMQueryCondition condition = new UMQueryCondition();
        condition.setGroupId(groupId);
        condition.getPage().setPageNum(pageNum);
        condition.getPage().setPageSize(50);
        condition.getOrderByFields().addAll( Arrays.asList(orderBy) );

        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
        PageInfo page = pageQuery.getResultList();
        page.setItems( fillGroupInfo2User(page.getItems()) );
        return page;
    }
    
    public PageInfo searchUser(UMQueryCondition condition) {
        List<Long> groupIds = getChildrenGroupIds(condition.getGroupId());
        if( EasyUtils.isNullOrEmpty(groupIds) ) {
        	return null;
        } else {
        	 condition.setGroupIds(groupIds);
        }
        
        String hql = "select distinct u, g.id as groupId, g.name as groupName, g.domain "
            + " from User u, GroupUser gu, Group g " 
            + " where u.id = gu.userId and gu.groupId = g.id and g.id in (:groupIds) " 
            + condition.toConditionString();
        
        Set<String> set = condition.getIgnoreProperties();
        set.add("groupId");
        
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
        PageInfo page = pageQuery.getResultList();
        page.setItems(fillGroupInfo2User(page.getItems()));
        return page;
    }
}
