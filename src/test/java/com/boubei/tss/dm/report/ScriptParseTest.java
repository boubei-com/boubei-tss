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

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.sso.context.Context;

public class ScriptParseTest extends AbstractTest4DM {
    
    @Autowired private ReportAction action;
    @Autowired private _Reporter display;
    
    @Test 
    public void testScriptParserFactory() {
    	Assert.assertNull(ScriptParserFactory.getParser());
    	ScriptParserFactory.instance = null;
    	Config.setProperty("script_precheator", "com.boubei.tss.dm.report.XScriptParser");
    	ScriptParser sp = ScriptParserFactory.getParser();
    	Assert.assertNotNull(sp);
    	Assert.assertEquals(sp, ScriptParserFactory.getParser());
    }

    @Test
    public void test1() {      
    	
        HttpServletResponse response = Context.getResponse();
        MockHttpServletRequest  request = new MockHttpServletRequest();
        
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(Report.DEFAULT_PARENT_ID);
        report1.setName("report-1");
        report1.setScript(" select id, name from dm_report where 1=1 and createTime > ? " +
        		" <#if param2 != '-1'> and 1=1 </#if> ");
        
        String paramsConfig = "[ " +
        		"	{'label':'起始时间', 'type':'date', 'nullable':'false'}, " +
        		"	{'label':'分公司'} " +
        		"]"	;
        report1.setParam(paramsConfig);
        
        action.saveReport(response, report1);
        Long reportId = report1.getId();
        
        request.addParameter("param1", "2013-10-01");
        request.addParameter("param2", "-1");
        display.showAsJson(request, response, reportId.toString());
        
        request.removeParameter("param2");
        request.addParameter("param2", "阿斯达");
        display.showAsJson(request, response, reportId.toString());
        
        request.addParameter("debugSQL", "true");
        Object sql = display.showAsJson(request, response, reportId.toString());
        Assert.assertEquals(" select id, name from dm_report where 1=1 and createTime > ?   and 1=1  ", sql);
    }
    
}