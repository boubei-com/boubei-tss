/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.dao;

import java.util.List;

import com.boubei.tss.framework.persistence.IDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.User;

public interface IUserDao extends IDao<User>{

	/**
     * 删除用户并删除用户对组，用户对角色信息
	 * @param id
	 */
	User removeUser(User user);
	
	/**
	 * 设置用户最后登录系统的时间
	 */
	void setLastLoginTime(Long userId);

	/**
	 * 根据用户的id获取该用户拥有的所有角色
	 * (包括转授策略,但是转授策略不能删除,只能在权限转授中删除对应关系)
	 *
	 * @param userId
	 * @return List
	 */
	List<?> findRolesByUserId(Long userId);
    
    /**
     * 批量初始化用户密码的时候使用，防止做为一个动态属性实体来保存，从而提高性能
     * @param obj
     * @return
     */
    User initUser(User obj);

	/**
	 * 根据用户的id获取用户对组的信息
	 *
	 * @param userId
	 * @return List
	 */
	List<?> findUser2GroupByUserId(Long userId);

	/**
	 * 根据用户的id获取用户对角色的信息
	 *
	 * @param userId
	 * @return List
	 */
	List<?> findRoleUserByUserId(Long userId);

	/**
	 * 获取组和用户的关系
	 *
	 * @param groupId
	 * @param userId
	 * @return List
	 */
    GroupUser getGroup2User(Long groupId, Long userId);

    /**
     * 根据用户登录名获取用户实体
     * 
     * @param loginName 登录名
     * @return  User 用户实体对象
     */
    User getUserByAccount(String account, boolean vaildate);
 
    /**
     * 检查新增或修改用户时，用户账号、Email、phone是否有重复。
     * @param user
     */
    void checkUserAccout(User user);
    
    void recordUserLog(User user, Group group, String origin);

    String mockLogin(String userCode);
}
