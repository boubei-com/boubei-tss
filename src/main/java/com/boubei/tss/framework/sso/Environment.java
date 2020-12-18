/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.boubei.tss.MatrixUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.URLUtil;

/**
 * <p>
 * 环境变量对象：利用线程变量，保存运行时用户信息等参数
 * </p>
 */
public class Environment {
	
	/**
	 * 如果是集群环境，多台应用服务器，threadID可能会冲突; 加上IP
	 */
	public static long threadID() {
		String ip = MatrixUtil.getIpAddress();
		long threadID = Thread.currentThread().getId();
		return Math.abs( (ip + "_" + threadID).hashCode() );
	}
	
	public static boolean isAnonymous() {
		Long userId = getUserId();
		return Anonymous.one.getId().equals( userId ) || userId == null;
	}
	
	public static boolean isAdmin() {
		return UMConstants.ADMIN_USER_ID.equals( Environment.getUserId() );
	}
	
	public static boolean isRobot() {
		return UMConstants.ROBOT_USER_ID.equals( Environment.getUserId() );
	}
	
	public static boolean isDomainAdmin() {
		return getOwnRoles().contains(UMConstants.DOMAIN_ROLE_ID);
	}
	
	public static boolean isDeveloper() {
		return getOwnRoles().contains(UMConstants.DEV_ROLE_ID);
	}
	
	public static boolean isFirstTimeLogon() {
		return getUserInfo("lastLogonTime") == null;
	}
	
    /**
     * 获取用户ID信息
     */
    public static Long getUserId() {
        IdentityCard card = Context.getIdentityCard();
        if (card == null) {
            return null;
        }
        return card.getOperator().getId();
    }

    /**
     * 获取用户账号（LoginName）
     */
    public static String getUserCode() {
        IdentityCard card = Context.getIdentityCard();
        if (card == null) {
            return null;
        }
        return card.getLoginName();
    }
    
    public static Long getNotnullUserId() {
    	return (Long) EasyUtils.checkNull( getUserId(), Anonymous._ID);
    }
    
    public static String getNotnullUserCode() {
    	return (String) EasyUtils.checkNull(Environment.getUserCode(), Anonymous._CODE);
    }
    
    /**
     * 获取用户姓名
     */
    public static String getUserName() {
        IdentityCard card = Context.getIdentityCard();
        if (card == null) {
            return null;
        }
        return card.getUserName();
    }
    
    public static String getDomainCN() {
        String domain = EasyUtils.obj2String(getInSession(SSOConstants.USER_DOMAIN_CN));
		return domain.substring(0, Math.min(domain.length(), 4));
    }
    
    public static Object getUserInfo(String field) {
        IdentityCard card = Context.getIdentityCard();
        if ( card != null && card.getOperator() != null ) {
        	return card.getOperator().getAttributesMap().get(field);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
	public static List<Long> getOwnRoles() {
    	List<Long> list = new ArrayList<Long>();
    	list.add(UMConstants.ANONYMOUS_ROLE_ID);
    	
    	return (List<Long>) EasyUtils.checkNull(getInSession(SSOConstants.USER_RIGHTS_L), list);
    }
    
    @SuppressWarnings("unchecked")
	public static List<String> getOwnRoleNames() {
    	List<String> list = new ArrayList<String>();
    	list.add(Anonymous._NAME);
    	
    	return (List<String>) EasyUtils.checkNull(getInSession(SSOConstants.USER_ROLES_L), list);
    }
    
    public static String getUserGroup() {
    	return (String) Environment.getInSession(SSOConstants.USER_GROUP);
    }
    
    public static Long getUserGroupId() {
    	return (Long) Environment.getInSession(SSOConstants.USER_GROUP_ID);
    }
    
    public static boolean inGroup(String group, boolean up) {
    	if( up ) {
    		List<?> fatherGroups = (List<?>) Environment.getInSession("GROUPS_MAIN_NAME");
    		return fatherGroups.contains(group);
    	}
		return group.equals( getUserGroup() );
    }
    
    public static Long getUserOrg() {
    	return EasyUtils.obj2Long( getInSession(SSOConstants.USER_ORG) ) ;
    }

    public static String getDomainOrign() {
    	return (String) getInSession(SSOConstants.USER_DOMAIN);
    }
    
    public static String getDomain() {
    	return (String) EasyUtils.checkNull(getDomainOrign(), UMConstants.DEFAULT_DOMAIN);
    }
    
    public static Object getDomainInfo(String attr) {
    	return getInSession("domain_" + attr);
    }
    
    public static Object getInSession(String attrName) {
    	RequestContext requestContext = Context.getRequestContext();
        if (requestContext == null) {
            return null;
        }
        
        Object result = null;
        try {
	    	HttpSession session = requestContext.getSession();
	    	result = session.getAttribute(attrName);
        } 
        catch(Exception e) { }
        
        return result;
    }

    /**
     * 获取当前SessionID
     */
    public static String getSessionId() {
        RequestContext requestContext = Context.getRequestContext();
        if (requestContext == null) {
            return null;
        }
        return requestContext.getSessionId();
    }

    /**
     * 获取用户客户端IP
     */
    public static String getClientIp() {
        RequestContext requestContext = Context.getRequestContext();
        if (requestContext == null) {
            return null;
        }
        return requestContext.getClientIp();
    }
    
    /**
     * 获取应用系统上下文根路径(即发布路径)，一般为"/tss"
     */
    public static String getContextPath(){
        return Context.getApplicationContext().getCurrentAppServer().getPath();
    }
 
    public static String getOrigin() {
    	RequestContext rc = Context.getRequestContext();
        if(rc == null || rc.getRequest() == null) {
        	return "unknown";
        }
        
        String browser = rc.getRequest().getHeader("USER-AGENT");
    	return URLUtil.parseBrowser(browser);
    }
}
