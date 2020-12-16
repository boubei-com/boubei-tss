/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.boubei.tss.EX;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.web.display.SuccessMessageEncoder;
import com.boubei.tss.framework.web.display.XmlPrintWriter;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.PasswordRule;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.EasyUtils;

/**
 * <p> 修改密码Servlet </p>
 * <p>
 * 规则：<br>
 * 1、先验证旧密码是否正确（和主用户组密码），不相等则抛出异常结束修改密码流程；<br>
 * 2、修改主用户组里用户密码。（LoginName + Password MD5加密）<br>
 * </p>
 * 
 * request.getParameter("type")
 * 1、verify: 修改密码。需要正确输入旧密码 
 * 2、reset ：根据密码提示重置密码。只需UserID不为空且对应的用户存在
 */
@WebServlet(urlPatterns="/resetPassword.in")
public class ResetPassword extends HttpServlet {

	private static final long serialVersionUID = -740569423483772472L;
    
    IUserService userService = (IUserService) Global.getBean("UserService");
    ILoginService loginService = (ILoginService) Global.getBean("LoginService");
 
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
    	
		String userId    = request.getParameter("userId");
		String password  = request.getParameter("password");
		String ckcode    = request.getParameter("ckcode");
		HttpSession session = request.getSession();
		
		User user;
    	if( userId == null ) { 
			Object ckcodeInSession = session.getAttribute(SSOConstants.RANDOM_KEY);
			if( ckcode == null || !EasyUtils.obj2Int(ckcode).equals(ckcodeInSession) ) {
            	throw new BusinessException(EX.U_45);
            }
			
			String loginName = (String) session.getAttribute(SSOConstants.USER_ACCOUNT);
			user = userService.getUserByLoginName(loginName);
    	} 
    	else {
    		Long id = (Long) session.getAttribute(SSOConstants.USER_ID);
    		user = userService.getUserById(id);
            if(user == null) {
                return; // 账号已被删除，或伪造的userId
            }
    	}
		
        String newPassword;
        String verifyOrReset = request.getParameter("type");
        if( "reset".equals(verifyOrReset) ) { // 根据 密码问题重置密码 & 注册邮箱重置密码
        	newPassword = password;
        }
        else { // 正常修改密码
        	String oldMD5Password = user.encodePassword(password);
			if( !user.getPassword().equals(oldMD5Password) ) {
            	throw new BusinessException(EX.U_35);
            }
			
			newPassword = request.getParameter("newPassword");
			if( EasyUtils.isNullOrEmpty(newPassword) || newPassword.equals(password) ) {
            	throw new BusinessException(EX.U_36);
            }
        }
        
		// 更新密码
		if( SecurityUtil.isSafeMode() ) {
			int level = PasswordRule.getStrengthLevel(newPassword, user.getLoginName());
			if( level < PasswordRule.MEDIUM_LEVEL ) {
				throw new BusinessException(EX.U_37);
			}
		}
        loginService.resetPassword(user.getId(), newPassword);
        
		SuccessMessageEncoder encoder = new SuccessMessageEncoder(EX.U_38);
		encoder.print(new XmlPrintWriter(response.getWriter()));
    }
}

	