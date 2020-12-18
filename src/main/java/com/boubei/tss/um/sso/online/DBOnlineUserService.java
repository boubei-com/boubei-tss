/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso.online;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.PX;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.URLUtil;

/**
 * 在线用户库（数据库）
 */
@Service("DBOnlineUserService")
public class DBOnlineUserService implements IOnlineUserManager {
	
	Logger log = Logger.getLogger(this.getClass());
	
	@Autowired private IUserDao dao;
	
    /*     
     * 如果在线用户库中没有相同的用户存在， 则在在线用户库中添加此记录。
     * 
     * 1、同一IP，同一用户，只能有一个登录session（开多个浏览器不被支持？）
     * 2、同一用户，只能在PC、微信、H5上分别登录一个session
     */
    public void register(String token, String appCode, String sessionId, Long userId, String userName) {
    	
    	boolean isMobile = URLUtil.isMobile();
    	boolean isAPICall = Context.getRequestContext().isApiCall() && !isMobile;
    	
    	// session 还没有加入domain信息
    	String domain = queryDomain(userId);
    	Object multilogin = SQLExcutor.queryVL("select multilogin x from x_domain where domain = ?", "x", EasyUtils.checkNull(domain, UMConstants.DEFAULT_DOMAIN));
    	Object admin_su = Environment.getInSession("admin_su");
    	
        List<?> list = queryExists(userId, userName, isMobile, EasyUtils.checkNull(multilogin, admin_su), isAPICall);
        
        DBOnlineUser ou;
        if( list.isEmpty() ) {
        	userName += EasyUtils.checkTrue(ParamConstants.TRUE.equals( admin_su ), " < Admin", "");
        	ou = new DBOnlineUser(userId, sessionId, appCode, token, userName);
        	dao.createObject(ou);
        	
        	dao.setLastLoginTime(userId);
        } 
        else {
        	ou = (DBOnlineUser) list.get(0);
        	dao.evict(ou);
        	
        	// 移动端登录/API访问, 不干扰PC端
        	HttpSession session = Context.sessionMap.get(ou.getSessionId());
        	if( session != null && !isMobile && !isAPICall ) {
        		/*  销毁当前用户已经登录的session（登录在其它电脑上的），控制账号在多地登录. */
        		try {
        			session.invalidate(); 
        			dao.executeHQL("delete from DBOnlineUser where id = ?1", ou.getId());
        			
        			// 销毁session时，ou也被删除了；此处重新保存
        			ou.setId(null);
        			dao.createObject(ou);
        		} 
        		catch(Exception e) {
        			// java.lang.IllegalStateException: invalidate: Session already invalidated
        		}
        		
        		dao.setLastLoginTime(userId);
        	}
        	
        	if( isAPICall && !sessionId.equals(ou.getSessionId()) ) {
        		Context.sessionMap.remove(ou.getSessionId());
        	}

        	int loginCount = EasyUtils.obj2Int(ou.getLoginCount()) + 1 ;
        	dao.executeHQL("update DBOnlineUser set token = ?1, sessionId = ?2, loginCount = ?3, loginTime = ?4, clientIp = ?5, origin = ?6 where id = ?7", 
        			token, sessionId, loginCount, new Date(), Environment.getClientIp(), Environment.getOrigin(), ou.getId());
        }
        
        // 设置域信息（每次登陆domain可能已经发生了变化，重新设置）
    	if( domain != null && !domain.equals(ou.getDomain()) ) {
    		dao.executeHQL("update DBOnlineUser set domain = ?1 where id = ?2", domain, ou.getId());
    	}
    }
    
    private String queryDomain(Long userId) {
    	String hql = "from Group where id in (select groupId from GroupUser where userId = ?1) and groupType = ?2";
    	List<?> groups = dao.getEntities(hql, userId, Group.MAIN_GROUP_TYPE);
    	
    	String domain = null;
    	if( groups.size() > 0 ) {
    		Group group = (Group) groups.get(0);
        	domain = group.getDomain();
    	}
    	return domain;
    }
    
    List<?> queryExists(Long userId, String userName, boolean isMobile, Object multilogin, boolean isAPICall) {
    	// 对系统级账号 及 体验账号(PX.SYS_TEST_USER) 不做限制，允许小程序多处登录，不互踢
    	if( userId < 0 || userName.equals(ParamConfig.getAttribute(PX.SYS_TEST_USER))) {
            return new ArrayList<Object>(); 
    	}
    	
    	/* 
    	 * 1、移动端登录不干扰PC端；
    	 * 2、检查域信息配置，判断当前用户所在域是否支持一个账号多地同时登陆；
    	 * 3、API call 也不踢人;
    	 */
    	String hql = " from DBOnlineUser o where o.userId = ?1 and o.origin = ?2 ";
    	String origin = Environment.getOrigin();
		if( isMobile  ) {
            return dao.getEntities(hql, userId, origin); 
    	}
    	if( ParamConstants.TRUE.equals( multilogin ) || isAPICall ) {
    		hql += " and clientIp = ?3 ";
            return dao.getEntities(hql, userId, origin, Environment.getClientIp());
    	}
    	
    	// 通常一个账号只能登录一台电脑（加上userName条件可以不踢Admin切换的记录）
    	hql = " from DBOnlineUser o where o.userId = ?1 and o.userName = ?2";
        return dao.getEntities(hql, userId, userName);
    }

    /*
     * 只在 SessionDestroyedListener.sessionDestroyed(ev) 里调用，session.invalidate()可能发生在：
     * 1、session超时销毁，自动 （此种情形，不会自动删除DBOnlineUser，要等12小时后，其它用户人为logout了被清理）
     * 2、register在线库时，发现有账号多地登录的，销毁前面登录的session
     * 3、用户手动登出logout
     * 4、被Admin踢下线
     * 
     * 注：Jetty关闭前会失效所有的session，要在Jetty下测试自动登录，需要在重启jetty前把online_user表里的sessionId值清空
     */
    public String logout(String appCode, String sessionId) {
    	String hql = " from DBOnlineUser o where o.appCode = ?1 and o.sessionId = ?2 ";
        List<?> list = dao.getEntities(hql, appCode, sessionId);
        
        String token = null;
    	for(Object entity : list) {
    		DBOnlineUser ou = (DBOnlineUser) dao.delete(entity);
        	token = ou.getToken();
    	}
    	
    	// 将系统重启前登录 或 重启后登录但在线超12小时 的在线信息删除（应该是重启后，除去自动登录之外的，残留于在线库的登录记录）
    	Date restartTime = Global.restartTime, now = new Date();
		long currTime = now.getTime();
		Double delta = (Double) EasyUtils.checkTrue(currTime - restartTime.getTime() < 1000*60*3, 0.1d, 0d);
		Date time1 = DateUtil.subDays(restartTime, delta); // 重启后3分钟内，删除2小时前登录的（允许近期登录的自动登录）；3分钟后，删除所有重启前的登录记录
		Date time2 = DateUtil.subDays(now, 0.5); // 12小时
    	dao.executeHQL("delete from DBOnlineUser where loginTime < ?1", EasyUtils.checkTrue(time1.after(time2), time1, time2));
    	
		return token;
    }
    
    public boolean isOnline(String token) {
        String hql = "from DBOnlineUser o where o.token = ?1 and clientIp = ?2 ";
		List<?> list = dao.getEntities(hql, token, Environment.getClientIp());
		return list.size() > 0;
    }
}
