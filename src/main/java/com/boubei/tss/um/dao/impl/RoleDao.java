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

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.TreeSupportDao;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.dao.IRoleDao;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.SubAuthorize;
 
@Repository("RoleDao")
public class RoleDao extends TreeSupportDao<Role> implements IRoleDao {
	
	public RoleDao() {
		super(Role.class);
	}
 
    public List<Role> removeRole(Role role){
        List<Role> roles = getChildrenById(role.getId());
        for( Role temp : roles ){
            if(ParamConstants.FALSE.equals(temp.getIsGroup())){
                Long roleId = temp.getId();
                deleteAll(getEntities("from RoleUser  ru where ru.roleId = ?1 ", roleId));
                deleteAll(getEntities("from RoleGroup rg where rg.roleId = ?1 ", roleId));
            }
        }
        deleteAll(roles);
        
        return roles;
    }
 
	public List<?> getUsersByRoleId(Long roleId) {
		String hql = "select distinct u from RoleUser ru, User u where ru.userId = u.id and ru.roleId = ?1 and ru.strategyId is null ";
		return getEntities( hql, roleId );
	}

	public List<?> getGroupsByRoleId(Long roleId) {
		String hql = "select distinct g from RoleGroup rg, Group g where rg.groupId = g.id and rg.roleId = ?1 and rg.strategyId is null order by g.decode";
		return getEntities( hql, roleId );
	}
 
	public List<?> getEditableRoles() {
	    return getEntities("from Role r where r.id not in (-10000,-1,-6) order by r.decode");      
	}
 
	// ===========================================================================================================
    // 按策略转授角色的相关数据库操作
    // ===========================================================================================================

    public void deleteStrategy(SubAuthorize strategy) {
        delete(strategy);
        
        //清除RoleUser， RoleGroup中的记录
        deleteAll(getRoleUserByStrategy(strategy.getId()));
        deleteAll(getRoleGroupByStrategy(strategy.getId()));
    }

    public List<?> getRoleUserByStrategy(Long strategyId){
        return getEntities("from RoleUser o where o.strategyId = ?1", strategyId);
    }
    
    public List<?> getRoleGroupByStrategy(Long strategyId){
        return getEntities("from RoleGroup o where o.strategyId = ?1", strategyId);  
    }
    
    public List<?> getUsersByStrategy(Long strategyId) {
        String hql = "select distinct u from RoleUser ru, User u where ru.userId = u.id and ru.strategyId = ?1 ";
        return getEntities(hql, strategyId);
    }

    public List<?> getGroupsByStrategy(Long strategyId) {
        String hql = "select distinct g from RoleGroup rg, Group g " +
        		" where rg.groupId = g.id and rg.strategyId = ?1 order by g.levelNo, g.seqNo";
        return getEntities(hql, strategyId);
    }
    
    public List<?> getRolesByStrategy(Long strategyId) {
        String hql = "select distinct r from RoleUser o, Role r where o.roleId = r.id and o.strategyId = ?1";
        return getEntities(hql, strategyId);
    }
    
    public List<?> getSubAuthorizeableRoles(Long userId){
        String hql = "select distinct r from Role r, ViewRoleUser4SubAuthorize ru " +
        		" where r.id = ru.id.roleId and ru.id.userId = ?1 order by r.decode";
        return getEntities(hql, userId);
    }
    
    // ===========================================================================================================
    // 用户的授权信息变动时，拦截器需要调用来收回转授权限的方法
    // ===========================================================================================================
    public void deleteGroupSubAuthorizeInfo(Long groupId, Long roleId){
        String hql = "select distinct userId from GroupUser where groupId = ?1 ";
        List<?> userIds = getEntities( hql, groupId );
        for( Object userId : userIds ) {
            /* 判断用户是否直接被授予该角色（非继承自组，也非转授所得，直接角色关联用户）。
             * 是的话即使组不再拥有该角色，用户继续单独拥有该角色，则无需删除转授； 否则需删除该角色的转授信息 */
            hql = "from RoleUser ru where ru.userId = ?1 and ru.roleId = ?2 and ru.strategyId is null";
            if( getEntities( hql, userId, roleId ).isEmpty() ) { // 为空说明用户没有被直接授予该角色
            	deleteSubAuthorizeInfo((Long) userId, roleId);
            }
        }
    }
    
    private void deleteSubAuthorizeInfo(Long userId, Long roleId) {
    	/* 根据创建者获取转授策略ID集合 */
    	List<?> strategyIds = getEntities( "select id from SubAuthorize where creatorId = ?1 ", userId ); 
        for(Object strategyId : strategyIds) {
            /* 删除转授给用户或用户组的角色关系 */
            executeHQL( "delete RoleUser o where o.roleId = ?1 and o.strategyId = ?2", roleId, strategyId ); 
            executeHQL( "delete RoleGroup o where o.roleId = ?1 and o.strategyId = ?2", roleId, strategyId ); 
        }
    }

    // 当用户不再拥有的某个角色，则收回这个用户转授出去的授权信息 
    public void deleteUserSubAuthorizeInfo(Long userId, Long roleId) { 
    	// 判断用户所在用户组是否还拥有该角色，如果是，则无需删除转授。
    	String hql = " select rg from GroupUser gu, RoleGroup rg " +
    			" where gu.groupId = rg.groupId and gu.userId = ?1 and rg.roleId = ?2 and rg.strategyId is null";
    	if( getEntities( hql, userId, roleId ).isEmpty() ) {
    		deleteSubAuthorizeInfo(userId, roleId);
    	}
    }
}
