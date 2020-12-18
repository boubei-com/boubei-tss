/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.file.OrignUploadFile;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.web.HttpClientUtil;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IMessageService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.um.sso.online.DBOnlineUser;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.URLUtil;

@Controller
@RequestMapping("/si")
public class SystemInfo {
	
	static Logger log = Logger.getLogger(SystemInfo.class);
	
	@Autowired private ICommonService commonService;
	@Autowired private APIService apiService;
	@Autowired private IUserService userService;
	@Autowired private ILoginService loginService;
	@Autowired private IMessageService msgService;
	
	/**
	 * PC、PDA、WX等前端的时间查询条件，统一取后台接口提供的时间戳（登录时写到cookie里）
	 * 
	 * tssJS.get("/tss/si/time", {}, function(time) { console.log(time); });
	 * tssJS.Cookie.getValue("server_time")
	 */
	@RequestMapping(value = "/time", method = RequestMethod.GET)
	@ResponseBody
	public String systemTime(HttpServletResponse response) {
		HttpClientUtil.setCookie(response, RequestContext.SERVER_TIME, DateUtil.format( new Date() ) );
		return DateUtil.now();
	}
	
	@RequestMapping(value = "/version", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getVersion(HttpServletRequest req) {
		String packageTime = Config.getAttribute("last.package.time");
		String environment = Config.getAttribute("environment");
		
		Date d = (Date) EasyUtils.checkNull(DateUtil.parse(packageTime), new Date());
		d = DateUtil.addDays(d, 1d/3);  //  +8小时，变成北京时间
		packageTime = DateUtil.format(d, "yyyy-MM-dd HH:mm:ss");
 
		boolean online = !Environment.isAnonymous() || autoLogin(req, null, apiService);
		return new Object[] { 
					packageTime, 
					environment, 
					online, 
					Context.sessionMap.size(), 
					msgService.getUnReadHignLevelMsg(2),  // 取最新紧急程度【高】的站内信
					online ? systemTime(Context.getResponse()) : DateUtil.now()
				};
	}
	
	/** 切换用户 ， Admin/开发者(TODO 需维护一个开发者名单)可以任意切换，域管理员只能切本域下的账号 */
	@RequestMapping(value = "/su", method = RequestMethod.PUT)
	@ResponseBody
	public Object su(HttpServletRequest request, String target) {
		
		if("admin".equalsIgnoreCase(target)) return null;
		if( !Environment.isAdmin() && !Environment.isDomainAdmin() && !Environment.isDeveloper() ) return null;
		
		// Admin切换用户不踢人 
		HttpSession session = request.getSession();
		session.setAttribute("admin_su", ParamConstants.TRUE);
		String adminName = Environment.getUserName();
				
		// 先删除当前Admin账号的在线用户记录
		List<?> list = commonService.getList("from DBOnlineUser where sessionId = ?1", session.getId());
		for(Object o : list) {
			commonService.deleteWithLog(DBOnlineUser.class, ((DBOnlineUser)o).getId() );
		}
		
		Object userCode = SQLExcutor.queryVL("select loginName x from um_user where ? in (loginName, userName, telephone)", "x", target.trim());
		target = (String) EasyUtils.checkNull(userCode, target);
		
		if( Environment.isDomainAdmin() && !loginService.getUsersMap(Environment.getDomain()).containsKey(target) ) {
			return "Target user is not in your domain";
		}
		
		String token = apiService.mockLogin( target );
		
		// 记录切换日志
		BusinessLogger.log("用户登录（切换）", adminName, adminName + " >> " + target);
		
		return token;
	}
	
	public static boolean autoLogin(HttpServletRequest req, HttpServletResponse rep, APIService apiService) {
		if(apiService == null) {
			apiService = (APIService) Global.getBean("APIService");
		}
		
		String tokenInCookie = RequestContext.getValueFromCookie(req, "token");
		Object clientIp = EasyUtils.checkNull(URLUtil.getClientIp(req), "127.0.0.1");
		
		String sql = "select distinct u.loginName v from um_user u, online_user ou where u.id = ou.userId and ou.token = ? and ou.clientIp = ?  ";
		String userCode = (String) SQLExcutor.queryVL(sql, "v", tokenInCookie, clientIp);
		if( userCode == null ) {
			log.debug( tokenInCookie + ", " + clientIp );
			return false;
		}
		
		if(rep != null) {
			Context.initRequestContext(req);
			Context.setResponse(rep); 
		}
		String token = apiService.autoLogin(userCode);
		log.info(userCode + " auto login, req path: " + req.getServletPath());
		
		Map<Integer, Object> params = new HashMap<Integer, Object>();
		params.put(1, token);
		params.put(2, Environment.getUserId());
		SQLExcutor.excute("delete from online_user where token <> ? and userId = ? ", params, DMConstants.LOCAL_CONN_POOL);
		
		return true;
	}
	
	/**
	 * 转移域管理员 ： https://wms.boudata.com/tss/si/transDomain/D063/715/718
	 * 			$.post('/tss/si/transDomain/D015/106404/107200', {}, function(){})
	 *  
	 * 1、Admin先授予toUser域管理员角色
	 * 2、执行本方法
	 * 3、停用 或 删除 fromUser
	 */
	@RequestMapping(value = "/transDomain/{domain}/{userId}/{toUserId}")
	@ResponseBody
	public Object transDomainAdmin(@PathVariable String domain, 
			@PathVariable Long userId, 
			@PathVariable Long toUserId) {
		
		if( !Environment.isAdmin() ) return "failed";
		
		User fromUser = userService.getUserById(userId);
		User toUser = userService.getUserById(toUserId);
		String fromCode = fromUser.getLoginName();
		String toCode = toUser.getLoginName();
		
		SQLExcutor.excute("update um_sub_authorize set buyerId = " +toUserId+ " where buyerId = " + userId, DMConstants.LOCAL_CONN_POOL);
		SQLExcutor.excute("update um_sub_authorize set ownerId = " +toUserId+ " where ownerId = " + userId, DMConstants.LOCAL_CONN_POOL);
		SQLExcutor.excute("update um_roleuser set userId = " +toUserId+ " where userId = " + userId, DMConstants.LOCAL_CONN_POOL);
		SQLExcutor.excute("update cloud_account set belong_user_id = " +toUserId+ " where belong_user_id = " + userId, DMConstants.LOCAL_CONN_POOL);
		
		SQLExcutor.excute("update cloud_module_user set userId = " +toUserId+ " where userId = " + userId, DMConstants.LOCAL_CONN_POOL);
		SQLExcutor.excute("update cloud_module_order set creator = '" +toCode+ "' where creator = '" +fromCode+ "'" , DMConstants.LOCAL_CONN_POOL);
		SQLExcutor.excute("update x_domain set creator = '" +toCode+ "' where domain = '" +domain+ "'", DMConstants.LOCAL_CONN_POOL);
		
		return "success";
	}
	
	/**
	 * 测试登录用户的session里属性值，便于调试
	 * $.post("/tss/si/sessionVal", {"attr": "<#if GROUP_LEVEL=='2'>true</#if>"}, function(v) { console.log(v) } )
	 */
	@RequestMapping(value = "/sessionVal")
	@ResponseBody
	public Object[] testSessionVal(String attr) {
		attr = (String) EasyUtils.checkNull(attr, DMConstants.USER_CODE);
		Object result = Context.getRequestContext().getSession().getAttribute(attr);
		Object result2 = DMUtil.fmParse( attr );
		return new Object[] { result, result2 };
	}
	
	@RequestMapping(value = "/ui/{sessionAttr}", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getLoginUserInfo(@PathVariable String sessionAttr) {
		sessionAttr = (String) EasyUtils.checkNull(sessionAttr, DMConstants.USER_CODE);
		String result = DMUtil.fmParse( MacrocodeCompiler.createMacroCode(sessionAttr));
		return new Object[] { result };
	}
	
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	@ResponseBody
	public Object[] getThreadInfos() {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);  
		Long random = null;
		try {
			random = SecureRandom.getInstance("SHA1PRNG").nextLong();
		} catch (NoSuchAlgorithmException e) {
		}
		return new Object[] { threadMXBean.getThreadCount(), threadInfos, random };
	}
	
	@RequestMapping("/download/{id}")
	public void downloadAttach(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") Long id) throws IOException {
		if( !Environment.isAdmin() ) return;

		OrignUploadFile ogf = (OrignUploadFile) commonService.getEntity(OrignUploadFile.class, id);
		FileHelper.downloadFile(response, ogf.getPath(), (String) EasyUtils.checkNull(ogf.getOld(), ogf.getName()));
	}
}
