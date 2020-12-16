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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.report.permission.ReportResource;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;
import com.boubei.tss.modules.cloud.ModuleAction;
import com.boubei.tss.modules.log.LogQueryCondition;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

public class ReportActionTest extends AbstractTest4DM {
    
	@Autowired private ReportService service;
    @Autowired private ReportAction action;
    @Autowired private LogService logService;
    @Autowired private ModuleAction moduleAction;
    
    @Test
    public void testReportService() {
    	Long reportId = -92L;
    	try {
    		service.getReport(reportId, false);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.DM_18, reportId) , e.getMessage());
		}
    	
    	try {
    		service.getReport(reportId, true);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.U_13, reportId), e.getMessage());
		}
    	
    	Assert.assertNull(service.getReportId("name", "not exists", Report.TYPE1));
    	
    	Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(Report.DEFAULT_PARENT_ID);
        report1.setName("report-1-s");
        report1.setParam(null); // error
        report1.setDisabled(ParamConstants.FALSE);
        report1.setNeedLog(ParamConstants.FALSE);
        report1.setMailable(ParamConstants.TRUE);
        report1.setIcon("1.png");
        report1.setColDefs("50,100,110,80");
        report1.setMobilable(ParamConstants.TRUE);
        service.createReport(report1);
        System.out.println( EasyUtils.obj2Json(report1) );
        reportId = report1.getId();
        TreeAttributesMap map = report1.getAttributes();
        Assert.assertEquals("false", map.get("hasScript"));
        Assert.assertEquals(ParamConstants.TRUE, report1.getMailable());
        
    	Map<String, String> requestMap = new HashMap<String, String>();
		Assert.assertTrue( service.queryReport(reportId, requestMap, 1, 100, -1L).result.isEmpty() ); // test no script
		
		report1.setScript("select * from dm_report where id > 0");
		service.updateReport(report1);
		Assert.assertTrue( !service.queryReport(reportId, requestMap, 1, 100, System.currentTimeMillis()).result.isEmpty() ); // test no param
		
		Report report2 = new Report();
		report2.setType(Report.TYPE1);
		report2.setParentId(Report.DEFAULT_PARENT_ID);
		report2.setScript("select * from dm_report where id > 0");
		report2.setName("report-2-s");
		report2.setCode("report-2s");
		report2.setDatasource("ds-x");
        service.createReport(report2);
		try {
			service.queryReport(report2.getId(), requestMap, 1, 100, -3L); // test not exsits ds
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.parse(EX.DM_02, report2.getDatasource()), e.getMessage());
		}
		
		Report report3 = new Report();
        report3.setType(Report.TYPE1);
        report3.setParentId(Report.DEFAULT_PARENT_ID);
        report3.setScript("select * from dm_report where id > 0");
        report3.setParam("{]");
        report3.setName("report-3-s");
        report3.setCode("report-3s");
        report3.setDatasource("ds-x");
        service.createReport(report3);
        try {
        	service.queryReport(report3.getId(), new HashMap<String, String>(), 1, 1, Environment.getUserId());
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue( e.getMessage().indexOf("JSON格式存在错误") >= 0 );
	    }
        
        // 创建同Code report
        try {
        	Report report3_2 = new Report();
        	report3_2.setType(Report.TYPE1);
        	report3_2.setParentId(Report.DEFAULT_PARENT_ID);
        	report3_2.setName("report-3-s");
        	report3_2.setCode("report-3s");
        	report3_2.setDatasource("ds-x");
			service.createReport(report3_2);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.parse(EX.DM_33, report3.getCode()), e.getMessage());
		}
        
		try {
        	logout();
	        service.delete(report1.getId());
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("权限不足，删除失败", e.getMessage().indexOf("权限") >= 0 ); 
	    }
        
		String token = TokenUtil.createToken("1234567890", UMConstants.ROBOT_USER_ID);
		IdentityCard card = new IdentityCard(token, new OperatorDTO(UMConstants.ROBOT_USER_ID, "Job.Robot"));
		Context.initIdentityInfo(card); 
        Assert.assertNotNull( service.getReport(report1.getId()) );
    }
    
    @Test
    public void testReportCRUD() {
        
        HttpServletResponse response = Context.getResponse();
        MockHttpServletRequest  request = new MockHttpServletRequest();
        
        request.addParameter("parentId", "_root");
        action.getReport(request, response, Report.TYPE0);
        
        request.removeParameter("parentId");
        request.addParameter("parentId", Report.DEFAULT_PARENT_ID.toString());
        action.getReport(request, response, Report.TYPE0);
        
        Report group1 = new Report();
        group1.setType(Report.TYPE0);
        group1.setParentId(Report.DEFAULT_PARENT_ID);
        group1.setName("report-group-1");
        action.saveReport(response, group1);
        
        Report group2 = new Report();
        group2.setType(Report.TYPE0);
        group2.setParentId(Report.DEFAULT_PARENT_ID);
        group2.setName("report-group-2");
        group2.setRemark("open it");
        action.saveReport(response, group2);
        action.startOrStop(response, group2.getId(), 0); // 启用组
        action.saveReport(response, group2); // update
        
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(group1.getId());
        report1.setName("report-1");
        report1.setScript("select * from dm_report where id = ?");
        String paramsConfig = "[ {'label':'报表ID', 'type':'Number', 'jsonUrl':'../xxx/list', 'multiple':'true'}," +
        		"{'label':'组织列表', 'type':'String', 'nullable':'false'}]"	;
        report1.setParam(paramsConfig);
        report1.setDatasource(PX.DEFAULT_CONN_POOL);
        report1.setDisplayUri("template/ichart.html");
        report1.setParamUri("more/bi_condition.html");
        report1.setRemark("test report");
        report1.setDisabled(ParamConstants.FALSE);
        action.saveReport(response, report1);
        Assert.assertEquals(report1.getResourceType(), new ReportResource().getResourceType());
        
        action.getAllReport(response);
        action.getAllReportGroups(response);
        Assert.assertNull( action.getReportJob(response, report1.getId(), true) );
        
        request.addParameter("reportId", report1.getId().toString());
        action.getReport(request, response, Report.TYPE1);
        
        action.startOrStop(response, group1.getId(), 1);
        
        action.startOrStop(response, report1.getId(), 1);
        action.startOrStop(response, report1.getId(), 0);
        
        action.subscribe(response, report1.getId(), 1);
        action.subscribe(response, report1.getId(), 0);
        try {
        	action.subscribe(response, group1.getId(), 1);
        	Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.DM_19, e.getMessage());
		}
        
        action.startOrStop(response, group2.getId(), 1);
        action.copy(response, report1.getId(), group2.getId());
        action.sort(response, group1.getId(), group2.getId(), 1);
        action.move(response, report1.getId(), group2.getId());
        action.move(response, group2.getId(), group1.getId());
        action.startOrStop(response, report1.getId(), 0);
        
        action.getAllReport(response);
        moduleAction.listResource("dm_report");
        
        // test permission 
        action.getOperations(response, report1.getId());
        
        // test get my reports
        List<Long> myreportIds = action.getMyReportIds();
        Assert.assertTrue(myreportIds.size() > 1);
        
        List<Object> myreports = action.getReportsByGroup(group1.getId());
        Assert.assertTrue(myreports.size() > 1);
        
        myreports = action.getReportsByGroup(Report.DEFAULT_PARENT_ID);
        Assert.assertTrue(myreports.size() > 1);
        
        List<Report> lastest = new ArrayList<Report>();
		lastest.add(report1);
		lastest.add(group1);
		ReportQuery.sortLastest(new ArrayList<Report>(), lastest);
		lastest.add(group2);
		lastest.add(group2);
		ReportQuery.sortLastest(new ArrayList<Report>(), lastest);
        
        // test get dataService list
        action.getDataServiceList(response);
        
        // test report collect
        action.collectReport(response, report1.getId(), true);
        action.collectReport(response, report1.getId(), true);
        List<?> collection = action.queryCollectReports();
		Assert.assertEquals(1, collection.size());
        Assert.assertEquals(report1.getId(), ((Object[])collection.get(0))[0]);
        action.collectReport(response, report1.getId(), false);
        collection = action.queryCollectReports();
		Assert.assertEquals(0, collection.size());
		
		ReportUser ru = new ReportUser(1L, 1L);
		ru.setId(null);
		Assert.assertEquals(2L, ru.getUserId() + ru.getReportId());
		Assert.assertNull( ru.getType() );
		
		action.zanReport(response, report1.getId(), 2);
		action.zanReport(response, report1.getId(), 2);
		Assert.assertEquals("1", action.countZan(report1.getId(), 2).toString());
        
        // test report schedule
        Long reportId = report1.getId();
        String jobConfig = " 0 36 10 * * ? | " +
        		reportId + ":" + report1.getName() + ":lovejava@163.com,BL00618:param1=1";
        action.saveReportJob(response, report1.getId(), false, jobConfig);
        action.saveReportJob(response, report1.getId(), true, jobConfig);
        action.saveReportJob(response, report1.getId(), true, jobConfig);
        
        Object[] result = action.getReportJob(response, reportId, false);
        Assert.assertEquals("0 36 10 * * ?", result[1].toString().trim());
        
        result = action.getReportJob(response, reportId, true);
        Assert.assertEquals("0 36 10 * * ?", result[1].toString().trim());
        
        action.delReportJob(response, group2.getId(), true);
        action.delReportJob(response, reportId, true);
        result = action.getReportJob(response, reportId, true);
        Assert.assertNull(result);
        // test report schedule end
        
        action.delete(response, report1.getId());
        action.delete(response, group1.getId());
        action.getAllReport(response);
        
        try {
            Thread.sleep(1000); // 等待日志异步输出完毕
        } catch (InterruptedException e) {
        }
        
        LogQueryCondition condition = new LogQueryCondition();
        condition.setOperateTimeFrom(new Date(System.currentTimeMillis() - 1000*3600*3));
        PageInfo logsInfo = logService.getLogsByCondition(condition);
        List<?> logs = logsInfo.getItems();
        for(Object temp : logs) {
            log.debug(temp);
        }
    }
    
    @Test
    public void testReportTLs() {
    	String rtd = DMConstants.getReportTLDir();
 		File reportTLDir = new File(URLUtil.getWebFileUrl(rtd).getPath());
 		File newFile = new File(reportTLDir.getPath() + "/1.html");
 		FileHelper.writeFile(newFile, "111111111111");
 		
 		File tempDir1 = FileHelper.createDir(reportTLDir.getPath() + "/temp1");
 		newFile = new File(tempDir1.getPath() + "/2.html");
 		FileHelper.writeFile(newFile, "222222222222");
        
        reportTLDir = new File(URLUtil.getWebFileUrl("modules/" + DMConstants.REPORT_TL_DIR_DEFAULT).getPath());
        newFile = new File(reportTLDir.getPath() + "/3.html");
        FileHelper.writeFile(newFile, "333333333333");
 		
    	action.getReportTLs(response);
    }
    
    @Test
    public void testDevloperReportTLs() {
    	HttpSession session = request.getSession();
        Object userRights = session.getAttribute(SSOConstants.USER_RIGHTS_L);
        
        @SuppressWarnings("unchecked")
		List<Long> ownRoles = (List<Long>) EasyUtils.checkNull(userRights, new ArrayList<Long>());
        ownRoles.add( UMConstants.DEV_ROLE_ID );
        
        session.setAttribute(SSOConstants.USER_RIGHTS_L, ownRoles);
        
        String rtd = DMConstants.getReportTLDir();
        Assert.assertTrue( rtd.endsWith(Environment.getUserCode()) );
    }
}
