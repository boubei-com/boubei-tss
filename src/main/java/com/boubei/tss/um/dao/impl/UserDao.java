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

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Repository;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.framework.sso.online.OnlineUserManagerFactory;
import com.boubei.tss.framework.web.HttpClientUtil;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.GroupUser;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.entity.UserLog;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.util.EasyUtils;

@Repository("UserDao")
public class UserDao extends BaseDao<User> implements IUserDao {

    public UserDao() {
		super(User.class);
	}
    
    public User initUser(User obj) {
		return create(obj);
	}
	
	public User removeUser(User user) {
		delete(user);

        Long deletedUserId = user.getId();
        deleteAll(findUser2GroupByUserId(deletedUserId)); // 用户对组
        deleteAll(findRoleUserByUserId(deletedUserId));  // 用户对角色
		
		return user;
	}
	
	public void setLastLoginTime(Long userId) {
		try {
			if( Context.getRequestContext().isApiCall() ) return;

			String hql = "update User set lastLogonTime = ?1, logonCount = logonCount+1 where id = ?2";
			this.executeHQL(hql, new Date(), userId);
		} 
    	catch( Exception e ) { }
	}
	
    public void checkUserAccout(User user) {
    	boolean isNew = user.getId() == null;
    	
        String loginName = user.getLoginName();
        if( EasyUtils.isNullOrEmpty(loginName) ) {
        	throw new BusinessException( EX.U_50 );
        }
        
		User existUser = this.getUserByAccount(loginName, false);
		if( existUser != null && ( isNew || !existUser.equals(user) ) ) {
            throw new BusinessException( EX.parse(EX.U_29, loginName) );
        }
		
        String eamil = user.getEmail();
        if( !EasyUtils.isNullOrEmpty(eamil) ) {
        	existUser = this.getUserByAccount(eamil, false);
        	if( existUser != null && ( isNew || !existUser.equals(user) ) ) {
        		throw new BusinessException( EX.parse(EX.U_30, eamil) );
        	}
        }
        
        String mobile = user.getTelephone();
        if( !EasyUtils.isNullOrEmpty(mobile) ) {
        	existUser = this.getUserByAccount(mobile, false);
        	if( existUser != null && ( isNew || !existUser.equals(user) ) ) {
        		throw new BusinessException( EX.parse(EX.U_31, mobile));
        	}
        }
    }
 
	public User getUserByAccount(String account, boolean vaildate) {
		account = account.trim();
	    List<?> users = getEntities("from User o where o.loginName = ?1 ", account);
	    if( users.isEmpty() ) {
	    	users = getEntities("from User o where ?1 in (o.telephone, o.email) ", account);
	    }
	    
        User user = users.size() > 0 ? (User) users.get(0) : null;
        
        if(vaildate) {
        	if (user == null) {
                throw new BusinessException( EX.parse(EX.U_00, account) );
            } 
            else if (ParamConstants.TRUE.equals(user.getDisabled())) {
                throw new BusinessException(EX.U_26);
            } 
            else {
    			Date accountLife = user.getAccountLife();
    			if (accountLife !=  null && new Date().after(accountLife) ) {
    			    throw new BusinessException(EX.U_27);
    			}
    		}
        }
        
        return user;
	}

	public List<?> findUser2GroupByUserId(Long userId) {
		return getEntities("from GroupUser o where o.userId = ?1 ", userId);
	}

	public List<?> findRoleUserByUserId(Long userId) {
		return getEntities("from RoleUser o where o.userId = ?1 and o.strategyId is null", userId);
	}

	public List<?> findRolesByUserId(Long userId) {
		String hql = "select distinct r, ru.strategyId from RoleUser ru, Role r where ru.roleId = r.id and ru.userId = ?1 order by r.decode ";
		return getEntities(hql, userId);
	}
 
	public GroupUser getGroup2User(Long groupId, Long userId) {
        List<?> list = getEntities("from GroupUser o where o.groupId = ?1 and o.userId = ?2", groupId, userId);
		return !list.isEmpty() ? (GroupUser)list.get(0) : null;
	}
	
	public void recordUserLog(User user, Group group, String origin) {
		UserLog log = new UserLog();
		log.setUserCode( user.getLoginName() );
		log.setDisabled( user.getDisabled() );
		
		if( group != null ) {
			log.setGroupId(group.getId());
			log.setGroupName(group.getName());
			log.setDomain( group.getDomain() );
		}
		
		String hql = "select distinct r from RoleUser ru, Role r where ru.roleId = r.id and ru.userId = ?1";
		List<?> roles = getEntities(hql, user.getId());
		log.setRoleIds( EasyUtils.objAttr2Str(roles, "id") );
		log.setRoleNames( EasyUtils.objAttr2Str(roles, "name") );
		
		log.setOperator( Environment.getUserCode() );
		log.setOperateTime( new Date() );
		log.setRemark( origin + " " + Environment.getOrigin() );
		
		createObject(log);
	}
	
	public String mockLogin(String userCode) {
    	User user = getUserByAccount(userCode, true);
		Long userId = user.getId();
		String sessionId = Context.getRequestContext().getSessionId();
		sessionId = (String) EasyUtils.checkNull(sessionId, userCode); // WFJob等没有session，不适合用mockLogin
		
		// 设置令牌到Session，使Environment生效
		String token = TokenUtil.createToken(sessionId, userId);
		IdentityCard card = new IdentityCard(token, new OperatorDTO(user));
		Context.initIdentityInfo(card); 
		
		// 生成用户级别锁（系统重启后，防止多线程各自生成了锁）
		String cKey = "synchronized-" + userCode; 
		Pool cache = CacheHelper.getNoDeadCache();
		Cacheable obj;
		synchronized(this) {
			obj = cache.getObject(cKey);
			if( obj == null ) {
				obj = cache.putObject(cKey, new Object());
			}
		}
		synchronized ( obj ) {
	        LoginCustomizerFactory.instance().getCustomizer().execute(); // saveUserRolesAfterLogin 及 设置session信息，获取用户域、组织、角色等信息
	        
	        // 注册在线用户库
	        IOnlineUserManager onlineUserManager = OnlineUserManagerFactory.getManager();
			String appCode = Config.getAttribute(PX.APPLICATION_CODE);
			onlineUserManager.register(token, appCode, sessionId, userId, user.getUserName());
			
			if(obj.getHit() == 0) {
				cache.destroyByKey(cKey);
			}
		}
        
        // 设置Cookie
        HttpServletResponse response = Context.getResponse();
		HttpClientUtil.setCookie(response, RequestContext.USER_TOKEN, token);
        HttpClientUtil.setCookie(response, RequestContext.JSESSIONID, sessionId);
        
		return token;
    }
}