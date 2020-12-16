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
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.web.display.ErrorMessageEncoder;
import com.boubei.tss.framework.web.display.XmlPrintWriter;
import com.boubei.tss.framework.web.display.xmlhttp.XmlHttpEncoder;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.EasyUtils;

/**
 * <p>
 * 密码忘记时根据密码提示问题或答案重新设置密码。成功的话则将【用户ID】返回前台。
 * </p>
 */
@WebServlet(urlPatterns="/checkAnswer.in")
public class CheckAnswer extends HttpServlet {
 
	private static final long serialVersionUID = -740569423483772472L;
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
 
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String loginName        = request.getParameter("loginName");
		String passwordAnswer   = request.getParameter("passwordAnswer");
		String passwordQuestion = request.getParameter("passwordQuestion");
		
		IUserService service = (IUserService) Global.getBean("UserService");
		User user = service.getUserByLoginName(loginName);
		
		response.setContentType("text/html;charset=UTF-8");
		if ( user == null ) {
			ErrorMessageEncoder encoder = new ErrorMessageEncoder( EX.parse(EX.U_41, loginName) );
			encoder.print(new XmlPrintWriter(response.getWriter()));
		} 
		else {
            String userPasswordAnswer = user.getPasswordAnswer();
            String userPasswordQuestion = user.getPasswordQuestion();
            if ( EasyUtils.isNullOrEmpty(userPasswordQuestion) || EasyUtils.isNullOrEmpty(userPasswordAnswer)) {
                ErrorMessageEncoder encoder = new ErrorMessageEncoder( EX.parse(EX.U_42, loginName) );
                encoder.print(new XmlPrintWriter(response.getWriter()));
            } 
            else if (passwordAnswer.equals(userPasswordAnswer) && passwordQuestion.equals(userPasswordQuestion)) {
            	Long userId = user.getId();
				XmlHttpEncoder encoder = new XmlHttpEncoder();
				encoder.put("UserId", userId);
				encoder.print(new XmlPrintWriter(response.getWriter()));
				
				HttpSession session = request.getSession(true);
    			session.setAttribute(SSOConstants.USER_ID, userId);
			} 
			else {
				ErrorMessageEncoder encoder = new ErrorMessageEncoder( EX.U_44 );
				encoder.print(new XmlPrintWriter(response.getWriter()));
			}
		}
	}
}

	