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
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.ParamConstants;

public class _ReporterTest extends AbstractTest4DM {
    
    @Autowired private ReportAction action;
    @Autowired private _Reporter _reporter;
    
    @Test
    public void testJson2CSV() {   
    	HttpServletResponse response = Context.getResponse();
    	MockHttpServletRequest  request = new MockHttpServletRequest();
    	
    	request.addParameter("name", "网页报表");
    	request.addParameter("data", "仓库,库存\nOFC1,100\nOFC2,200");
    	
    	String fileName = _reporter.data2CSV(request, response)[0];
    	
    	request.addParameter("filename", fileName);
    	_reporter.download(request, response);
    }

    @Test
    public void testShowAsJson() {        
        HttpServletResponse response = Context.getResponse();
        MockHttpServletRequest  request = new MockHttpServletRequest();
        
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(Report.DEFAULT_PARENT_ID);
        report1.setName("report-1");
        report1.setCode("report11");
        report1.setScript(" select id, name from dm_report " +
        		" where id > ? " +
        		"  <#if param2??> and type <> ? <#else> and type = 1 </#if> " +
        		"  and (createTime > ? or createTime > ?) " +
        		"  and name in (${param5})");
        
        String paramsConfig = "[ {'label':'报表ID', 'type':'Number', 'nullable':'false', 'jsonUrl':'../xxx/list', 'multiple':'true'}," +
        		"{'label':'报表类型', 'type':'String'}," +
        		"{'label':'起始时间', 'type':'date', 'nullable':'false', 'defaultValue':'today-10'}, " +
        		"{'label':'结束时间', 'type':'date', 'nullable':'false'}," +
        		"{'label':'组织列表', 'type':'String', 'nullable':'false'}," +
        		"{'label':'隐藏值', 'type':'hidden'}" +
        		"]";
        report1.setParam(paramsConfig);
        
        report1.setRemark("test report");
        action.saveReport(response, report1);
        
        action.startOrStop(response, report1.getId(), ParamConstants.FALSE);
        
        log.debug("开始测试报表展示：");
        request.addParameter("param1", "0");
        request.addParameter("param2", "0");
        request.addParameter("param3", "2013-10-01");
        request.addParameter("param4", "2013/10/01 11:11:11");
        request.addParameter("param5", "报表一,'report-1");
        request.addParameter("param6", "test hidden param");
        
        Long reportId = report1.getId();
        _reporter.showAsGrid(request, response, reportId, 1, 10);
        _reporter.showAsJson(request, response, reportId.toString());
        
        request.addParameter("page", "1");
        request.addParameter("rows", "3");
        _reporter.showAsJson(request, response, report1.getCode());
        
        _reporter.exportAsCSV(request, new MockHttpServletResponse(), reportId, 1, 0);  // 测试导出
        _reporter.exportAsCSV(request, new MockHttpServletResponse(), reportId, 1, 10); // 测试导出
        
        request.addParameter("email", "jinhetss@163.com");
        _reporter.exportAsCSV(request, response, reportId, 1, 0); // 测试邮件推送
        
        request = new MockHttpServletRequest();
        request.addParameter("param1", "10001");
        request.addParameter("param3", "today - 100");
        request.addParameter("param4", "today + 10");
        request.addParameter("param5", "report-1,report-1");
        _reporter.exportAsCSV(request, new MockHttpServletResponse(), reportId, 1, 0); // 测试导出数据为空
        
        request = new MockHttpServletRequest();
        request.addParameter("param1", "0");
        request.addParameter("param3", "today - 100");
        request.addParameter("param4", "today + 10");
        request.addParameter("param5", "report-1,report-1");
        _reporter.showAsJson(request, response, report1.getName());
        
        // test nocache
        request.addParameter("noCache", "true");
        _reporter.showAsJson(request, response, report1.getName());
        request.removeParameter("noCache");
        
        // test ucache
        request.addParameter("uCache", "true");
        _reporter.showAsJson(request, response, report1.getName());
        request.removeParameter("uCache");
        
        // test get param define
        Object[] paramDefine = (Object[]) _reporter.getReportDefine(request, reportId.toString());
        Assert.assertTrue( paramDefine.length >= 6);
        
        paramDefine = (Object[]) _reporter.getReportDefine(request, report1.getName());
        Assert.assertEquals( reportId, paramDefine[7] );
        
        request.addParameter("rpName", report1.getName());
        paramDefine = (Object[]) _reporter.getReportDefine(request, reportId.toString());
        Assert.assertEquals( reportId, paramDefine[7] );
        
        request.removeParameter("rpName");
        String notExsits = "notExsits";
        try {
        	_reporter.getReportDefine(request, notExsits);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.parse(EX.DM_18, notExsits), e.getMessage());
		}
        
        // test jsonp
        request.addParameter("jsonpCallback", "func1");
        _reporter.showAsJsonp(request, response, report1.getName());
        request.removeParameter("jsonpCallback");
        
        // 测试导出超阀值(导出时，前台限定10万行，超过该值将提示要求缩短查询条件，分批导出)
        Report reportGruop = new Report();
        reportGruop.setName("reportGruop1");
        reportGruop.setParentId(Report.DEFAULT_PARENT_ID);
        reportGruop.setType(Report.TYPE0);
		action.saveReport(response, reportGruop );
		
		reportGruop = new Report();
        reportGruop.setName("reportGruop2");
        reportGruop.setParentId(Report.DEFAULT_PARENT_ID);
        reportGruop.setType(Report.TYPE0);
		action.saveReport(response, reportGruop );
		
		request = new MockHttpServletRequest();
		request.addParameter("param1", "0");
		request.addParameter("param2", "1");
        request.addParameter("param3", "2013-10-01");
        request.addParameter("param4", "2013-10-01");
        request.addParameter("param5", "reportGruop1,reportGruop2");
		_reporter.exportAsCSV(request, new MockHttpServletResponse(), reportId, 1, 1); // 阀值为1
		
		for(int i = 0; i < 30; i++) {
			_reporter.showAsJson(request, response, report1.getName());
		}
		
		 // test Customize report tree
        action.getMyReports(response, null);
        action.getMyReports(response, reportGruop.getId());
        
        report1.setName("报表一");
        commonDao.update(report1);
        action.getMyReports(response, null);
        action.getMyReports(response, reportGruop.getId());
    }
    
    /**
     * 执行SQL时出错了:Parameter "#4" is not set; SQL statement:
		 select id, name from dm_report  where id > ? and type <> ? and (createTime > ? or createTime > ?)   and name in ('report-1','report-1')
		   数据源：jdbc:h2:mem:h2db,
		   参数：{1=0, 2=2013-10-01 00:00:00.0, 3=2013-10-01 11:11:11.0},
     */
    @Test
    public void testShowAsJsonWithError() {        
        HttpServletResponse response = Context.getResponse();
        MockHttpServletRequest  request = new MockHttpServletRequest();
        
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(Report.DEFAULT_PARENT_ID);
        report1.setName("report-1");
        report1.setScript(" select id, name from dm_report " +
        		" where id > ? " +
        		"  <#if param2??> and type <> ? <#else> and type = 1 </#if> " +
        		"  and (createTime > ? or createTime > ?) " +
        		"  and name in (${param5})");
        
        String paramsConfig = "[ {'label':'报表ID', 'type':'Number', 'nullable':'false', 'jsonUrl':'../xxx/list', 'multiple':'true'}," +
        		"{'label':'报表类型', 'type':'String'}," +
        		"{'label':'起始时间', 'type':'date', 'nullable':'false'}, " +
        		"{'label':'结束时间', 'type':'date', 'nullable':'false'}," +
        		"{'label':'组织列表', 'type':'String', 'nullable':'false'}]"	;
        report1.setParam(paramsConfig);
        
        report1.setDisabled(ParamConstants.FALSE);
        report1.setRemark("test report");
        action.saveReport(response, report1);
        
        log.debug("开始测试报表展示：");
        request.addParameter("param1", "0");
        request.addParameter("param2", "0");
        request.addParameter("param4", "2013/10/01 11:11:11");
        request.addParameter("param5", "report-1,report-1");
        
        Long reportId = report1.getId();
        
		try {
			_reporter.showAsJson(request, response, reportId.toString());
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("参数【起始时间】不能为空。", true);
		}
		
		request = new MockHttpServletRequest();
		request.addParameter("param1", "0");
        request.addParameter("param2", "0");
        request.addParameter("param3", "2013-10-01");
        request.addParameter("param4", "2013/10/01 11:11:11");
        
        try {
			_reporter.showAsJson(request, response, reportId.toString());
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("参数【组织列表】不能为空。", true);
		}
        
        try {
			_reporter.showAsJson(request, response, "notExist");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("数据服务不存在", true);
		}
    }
}