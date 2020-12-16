/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.InfoEncoder;

public class _ReporterAPITest extends AbstractTest4DM {
    
    @Autowired private ReportAction action;
    @Autowired private _Reporter _reporter;
    
    @Test
    public void testReportAPI() {        
        HttpServletResponse response = Context.getResponse();
        MockHttpServletRequest  request = new MockHttpServletRequest();
        
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(Report.DEFAULT_PARENT_ID);
        report1.setName("API-1");
        report1.setCode("API-1");
        report1.setScript(" select id, name from dm_report where id > ?");
        
        String paramsConfig = "[ " +
        			"{'label':'报表ID', 'type':'Number', 'nullable':'false'}" +
        		"]";
        report1.setParam(paramsConfig);
        report1.setRemark("test report data api");
        report1.setDatasource( DMConstants.LOCAL_CONN_POOL );
        report1.setNeedLog( ParamConstants.FALSE );
        action.saveReport(response, report1);
        Long reportId = report1.getId();
        
        // test call report api 
        request.addParameter("param1", "0");
        request.addParameter("uName", UMConstants.ADMIN_USER);
        String uToken = InfoEncoder.string2MD5(reportId + ":" + UMConstants.ADMIN_USER);
		request.addParameter("uToken", uToken);
        
        Object[] params = new Object[] { "Admin", reportId.toString(), "D1", uToken, DateUtil.parse("2099-01-01"), "test", "Admin", new Date() };
        SQLExcutor.excuteInsert("insert into um_user_token (user,resource,type,token,expireTime,remark,creator,createTime,version) " +
				"values (?,?,?,?,?,?,?,?,0)", params , getDefaultSource());
        
        _reporter.showAsJson(request, response, report1.getName()); 
        
        // test call report api with error uToken
        request.removeParameter("uToken");
        request.addParameter("uToken", InfoEncoder.string2MD5(reportId + ":JK"));
        request.addParameter("uCache", "true");
        _reporter.showAsJson(request, response, report1.getName());  
        /*
    	try {
    		_reporter.showAsJson(request, response, report1.getName());  
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.DM_11+Environment.getUserCode(), e.getMessage());
		}
		*/
    	
    	DMUtil.getRequestMap(request, true);
    }
    
    // test api call ( local java call jetty/tomcat, set securityLevel = 5 ) 
    public static void main(String[] args) throws HttpException, IOException {
    	String uToken = InfoEncoder.string2MD5("499:Admin");  /* 令牌：md5(id|name:loginName) 大写  */
    	System.out.println(uToken);
    	
    	String url = "http://localhost:9000/tss/data/json/499";
    	callAPI(url, "Admin", uToken);
    	callAPI(url, "wrong-user", uToken);   // wrong user
    	callAPI(url, "Admin", "wrong-token"); // wrong token
    	callAPI(url, "", "");
    }
}
