/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.mock.dao._IGroupDAO;
import com.boubei.tss.framework.mock.dao._IGroupRoleDAO;
import com.boubei.tss.framework.mock.dao._IRoleDAO;
import com.boubei.tss.framework.mock.dao._IUserDAO;
import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.mock.model._GroupRole;
import com.boubei.tss.framework.mock.model._Role;
import com.boubei.tss.framework.mock.model._User;

@Service("_UMService")
public class _UMService implements _IUMSerivce {
    
    @Autowired private _IUserDAO  userDAO;
    @Autowired private _IGroupDAO groupDAO;
    @Autowired private _IRoleDAO  roleDAO;
    @Autowired private _IGroupRoleDAO groupRoleDAO;

    public void createUser(_User user) {
        userDAO.create(user);
    }

    public void deleteUser(_User user) {
        userDAO.deleteById(user.getId());
    }

    @SuppressWarnings("unchecked")
    public List<_User> queryAllUsers() {
        return (List<_User>) userDAO.getEntities("from _User");
    }

    public void updateUser(_User user) {
        userDAO.update(user);
    }

    public _User getUser(Long id) {
        return userDAO.getEntity(id);
    }

    @Override
    public _Group createGroup(_Group group) {
        Long parentId = group.getParentId();
        Integer nextSeqNo = groupDAO.getNextSeqNo(parentId);
        group.setSeqNo(nextSeqNo);
        return groupDAO.create(group);
    }

    @Override
    public void createGroupRole(_GroupRole gr) {
        groupRoleDAO.create(gr);
    }

    @Override
    public void createRole(_Role role) {
        roleDAO.create(role);
    }

    @Override
    public void deleteGroup(_Group group) {
        groupDAO.deleteGroup(group);
    }

    @Override
    public void deleteGroupRole(_GroupRole gr) {
        groupRoleDAO.delete(gr);
    }

    @Override
    public void deleteRole(_Role role) {
        roleDAO.delete(role);
    }

    @Override
    public _Group getGroup(Long id) {
        return groupDAO.getEntity(id);
    }

    @Override
    public _Role getRole(Long id) {
        return roleDAO.getEntity(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<_Group> queryAllGroups() {
        return (List<_Group>) groupDAO.getEntities("from _Group");
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<_Role> queryAllRoles() {
        return (List<_Role>) roleDAO.getEntities("from _Role");
    }

    @Override
    public List<?> queryGroupRole() {
        return groupRoleDAO.getEntities("from _GroupRole");
    }

    @Override
    public void updateGroup(_Group group) {
        groupDAO.update(group);
    }

    @Override
    public void updateRole(_Role role) {
        roleDAO.update(role);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<_Group> queryGroups(String hql, Object... args) {
        return (List<_Group>) groupDAO.getEntities(hql, args);
    }

    @Override
    public List<_Group> getChildrenByDecode(String decode) {
        return groupDAO.getChildrenByDecode(decode);
    }

    @Override
    public List<_Group> getChildrenById(Long id) {
        return groupDAO.getChildrenById(id);
    }

    @Override
    public List<_Group> getParentsById(Long id) {
        return groupDAO.getParentsById(id);
    }

    @Override
    public List<_Group> getRelationsNodeWhenSort(Long parentId, Integer sourceOrder, Integer targetOrder) {
        return groupDAO.getRelationsNodeWhenSort(parentId, sourceOrder, targetOrder);
    }

}
