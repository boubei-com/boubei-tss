/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.boubei.tss.PX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.SystemInfo;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.EasyUtils;

/**
 * 安全过滤器，防止无权限(匿名访问 或 自注册用户)直接访问后台服务地址。
 * 
 * 1、检查refer，防止跨域盗链，IP白名单配置的则不受此限制；
 * 2、依据安全级别：<3一律放行；>=3 检查URL白名单
 * 3、上面两步检测不通过的，则要求登录才能访问
 */
//@WebFilter(filterName = "Filter0Security", urlPatterns = {"/*"})
public class Filter0Security implements Filter {
	
    Logger log = Logger.getLogger(Filter0Security.class);
    
    public static final String THE_404_URL = "/tss/404.html";
    public static final String THE_PASSWD_URL = "/tss/modules/um/_password.htm";
 
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
    	
    	HttpServletRequest req = (HttpServletRequest) request;
    	HttpServletResponse rep = (HttpServletResponse) response;
    	
    	String servletPath = req.getServletPath();
    	String referer     = req.getHeader("referer");
        String serverName  = req.getServerName(); // 网站的域名
        
        /*0、防止同一IP多次请求攻击（多为匿名攻击），一分钟 > 180次 */
        SecurityUtil.denyMassAttack(req);
    	
        /* 忽略盗链检查 */
    	List<String> crossDomainIgnores = new ArrayList<String>();
    	crossDomainIgnores.add("report_portlet.html"); // 同时需加入到白名单里
    	crossDomainIgnores.add("404.html");
    	crossDomainIgnores.add(".portal");
    	for(String url : crossDomainIgnores) {
    		if(servletPath.indexOf(url) >= 0) {
    			referer = null;
    		}
    	}
    	
        /*1、 防止盗链 */
        if(referer != null && !referer.contains(serverName)) { // 如果是跨域访问了，则过滤ip白名单
        	List<String> whiteList = new ArrayList<String>();
        	String ipWhiteListConfig = ParamConfig.getAttribute(PX.IP_WHITE_LIST, "boubei.com");
        	whiteList.addAll( Arrays.asList( ipWhiteListConfig.split(",") ) );
        	
        	boolean pass = false;
        	for(String whiteip : whiteList) {
        		whiteip = whiteip.trim();
        		if( whiteip.length() > 0 && referer.indexOf( whiteip ) >= 0) {
        			pass = true;
        			break;
        		}
        	}
        	
        	if( !pass ) {
        		log.info("IP white list check denied! servletPath = " +servletPath+ ", referer = " + referer + ", whiteList = " + whiteList );
        		_404(rep, servletPath);
            	return;
        	}
        }      
        
        /* 2、检查是否忽略权限检查（依据URL白名单来判定）*/
        if( !isNeedPermission(servletPath, req) ) {
        	chain.doFilter(request, response);
            return;
        }
         
        /* 3、检测Session（判断用户是否登录，此时Filter3Context还没有执行，无法使用Environment）*/
        log.debug("checking permission: " + servletPath);
        HttpSession session = req.getSession(false);
        boolean isCacheUri = servletPath.indexOf("/cache/") >= 0; // 缓存监控自动刷新 不触发
		if ( !isCacheUri  && !checkSession(req, rep, session) ) {
        	log404Context(req, servletPath, session);
            _404(rep, servletPath);
            return;
        }
        
        /* 4、密码强度及有效期检测，安全等级 >= 4 */ 
        if( SecurityUtil.isSafeMode()
        		&& servletPath.indexOf(".htm") > 0 
        		&& servletPath.indexOf("_password.htm") < 0) {
        	
        	ILoginService loginService = (ILoginService) Global.getBean("LoginService");
        	session = req.getSession();
        	
        	boolean not_admin_su = session.getAttribute("admin_su") == null;
			Long userId = (Long) session.getAttribute(SSOConstants.USER_ID); 
			int flag = loginService.checkPwdSecurity(userId);
			
        	if(flag < 1 && userId != null && not_admin_su) {
        		log.debug(userId + "'s password is not safe, need to strengthen first.");
        		String originURI = req.getRequestURI().replaceFirst("//", "/");
				rep.sendRedirect(THE_PASSWD_URL + "?flag=" +flag+ "&origin=" +originURI);
        		return;
        	}
        }
        
        // 权限检测通过
        chain.doFilter(request, response);
    }

	protected void log404Context(HttpServletRequest req, String servletPath, HttpSession session) {
		log.info("checking permission failed, servletPath = " + servletPath + 
				", session is null = " + (session == null) + 
	    		", params" + DMUtil.parseRequestParams(req, false));
	}

	protected void _404(HttpServletResponse rep, String servletPath) throws IOException {
		if( servletPath.indexOf(".htm") > 0 ) {
			rep.sendRedirect(THE_404_URL);
		} else {
			rep.setContentType("application/json;charset=UTF-8");
			rep.getWriter().println("{\"code\": \"TSS-404\", \"errorMsg\": \"资源不存在或限制访问: " +servletPath+ "\"}");
		}
	}
 
    /**
     * 检查session是否存在，机器重启后，session消失，此时用token做下重新注册在线库（更新session信息）
     * 此处一定是非remote API（带uName和uToken）访问，isNeedPermission() 里检测了 apiCall
     */
    private boolean checkSession(HttpServletRequest req, HttpServletResponse rep, HttpSession session) {
    	if( session == null || session.getAttribute(RequestContext.USER_TOKEN) == null ) {
    		try {
    			return SystemInfo.autoLogin(req, rep, null);
    		} 
    		catch(Exception e) { // 自动登录失败不影响安全校验
    			log.error("autoLogin 出错了", e);
    		}
    	}
    	return true;
	}
    
    private boolean isNeedPermission(String servletPath, HttpServletRequest req) {
    	// 1、安全级别 < 4, 全部放行
    	if( !SecurityUtil.isSafeMode() || EasyUtils.isNullOrEmpty(servletPath) ) {
    		return false;
    	}
    	
    	// 2、检查URL白名单，白名单内的，放行
    	String[] whiteList = ParamConfig.getAttribute(PX.URL_WHITE_LIST).split(",");
    	for( String whiteItem : whiteList ) {
    		whiteItem = whiteItem.trim();
    		if( whiteItem.length() >= 3 && servletPath.indexOf( whiteItem ) >= 0 ) {
    			return false;
    		}
    	}
    	
    	// 3、安全级别 >= 3, 限制对所有 htm、html、restful（部分除外）的访问
		if( servletPath.indexOf(".htm") > 0 ) {
    		return true;
    	}
		else if( servletPath.indexOf(".") < 0 ) { // 无后缀，一般restful地址 或 /download
			/* 
			 * 地址带/api/的apicall 在Filter8里检测,其它自定义的接口远程调用时需自己控制访问许可检测（eg: Servlet4Upload /remote/upload 和 xdata及data） 
			 * 要求uName和uSign在 request.paramters里传递，不能在body里
			 */
    		boolean apiCall = RequestContext.isApiCall(req); 
			boolean expCall = req.getHeader("referer")  != null && servletPath.indexOf("/data/export/") >= 0; // 跨机器数据导出请求 & 【接口】调用，放行
			if( expCall || apiCall ) {  
    			return false; 
    		}
    		
    		String requestType = req.getHeader(RequestContext.REQUEST_TYPE);
			if( servletPath.indexOf("/data/json/") >= 0 
					&& RequestContext.XMLHTTP_REQUEST.equals(requestType) ) {
				
    			return false; /* ajax json请求（匿名访问、网页本地打开调试访问），放行。（注：jQuery发ajax请求需要在header里加上此参数）*/
    		}
    		
    		return true;
    	}
    	
    	return false;
    }

	public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Filter0Security init in " + Config.getAttribute(PX.ENVIRONMENT));
    }
    
    public void destroy() { }
}
