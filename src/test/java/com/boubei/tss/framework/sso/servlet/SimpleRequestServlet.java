/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p> SimpleRequestServlet.java </p>
 * do nothing，简单的返回请求地址
 */
public class SimpleRequestServlet extends HttpServlet {
    private static final long serialVersionUID = -6852492423340779582L;
    
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
        response.getWriter().println("<Response><url>" + request.getServletPath() + "</url></Response>");
    }
}
