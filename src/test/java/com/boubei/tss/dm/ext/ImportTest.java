/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ext;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportAction;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

public class ImportTest extends AbstractTest4DM {
	
	@Autowired ReportService reportService;
	@Autowired RecordService recordService;
	@Autowired Export export;
	@Autowired ReportAction reportAction;

	@Test
	public void testImportReport() {
		Report group1 = new Report();
        group1.setType(Report.TYPE0);
        group1.setParentId(Report.DEFAULT_PARENT_ID);
        group1.setName("report-group-1");
        reportService.createReport(group1);
        
        Report group2 = new Report();
        group2.setType(Report.TYPE0);
        group2.setParentId(Report.DEFAULT_PARENT_ID);
        group2.setName("report-group-2");
        reportService.createReport(group2);
        
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(group1.getId());
        report1.setName("report-222");
        report1.setCode("report-222");
        report1.setScript("select * from dm_report where id = ?");
        String paramsConfig = "[ {'label':'报表ID', 'type':'Number', 'jsonUrl':'../xxx/list', 'multiple':'true'}," +
        		"{'label':'组织列表', 'type':'String', 'nullable':'false'}]"	;
        report1.setParam(paramsConfig);
        report1.setDatasource(PX.DEFAULT_CONN_POOL);
        report1.setDisplayUri("template/ichart.html");
        report1.setRemark("test report");
        report1.setDisabled(ParamConstants.FALSE);
        reportService.createReport(report1);
        
        reportService.sort(group1.getId(), group2.getId(), -1);
        reportService.sort(group1.getId(), group2.getId(), 1);
        
        export.exportReport(response, group1.getId());
        
        List<?> sList = generalSearcher.searchResource("Report", "report");
		Assert.assertEquals(1, sList.size() );
		Assert.assertEquals("report-group-1", ((Object[])sList.get(0))[2] );
        
        List<Report> list = reportService.getReportsByGroup(group1.getId(), Environment.getUserId());
		String json = EasyUtils.obj2Json(list).replaceAll("report-222", "report-223");
		String exportPath = DataExport.getExportPath() + "/" + group1.getName() + ".json";
		FileHelper.writeFile(exportPath, json, false);
        
		// test import report define json
        AfterUpload servlet = new ImportReport();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    EasyMock.expect(mockRequest.getParameter("groupId")).andReturn("_root");
	    EasyMock.expect(mockRequest.getParameter("dataSource")).andReturn(DMConstants.LOCAL_CONN_POOL);
	    
	    try {
	        mocksControl.replay(); 
	        servlet.processUploadFile(mockRequest, exportPath, null);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
		 
	    Assert.assertTrue( reportService.getAllReport().size() >= 4 );
	}
	
	@Test
	public void testImportRecord() {
		Record group1 = new Record();
        group1.setType(Record.TYPE0);
        group1.setParentId(Record.DEFAULT_PARENT_ID);
        group1.setName("record3-group-1");
        recordService.createRecord(group1);
        
        Record group2 = new Record();
        group2.setType(Record.TYPE0);
        group2.setParentId(Record.DEFAULT_PARENT_ID);
        group2.setName("record3-group-2");
        recordService.createRecord(group2);
        
        Record record1 = new Record();
        record1.setType(Record.TYPE1);
        record1.setParentId(group1.getId());
        record1.setName("record3-1");
        record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
        record1.setTable("t_" + System.currentTimeMillis());
        record1.setDefine("[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'f2', 'type':'string'}," +
        		"{'label':'时间', 'code':'f3', 'type':'datetime', 'nullable':'false'}]");
        record1.setCustomizePage("../xx.html");
        record1.setCustomizeJS(" function() f1() { } ");
        record1.setCustomizeGrid(" function() gf1() { } ");
        record1.setBatchImp(ParamConstants.TRUE);
        record1.setCustomizeTJ("");
        record1.setRemark("test record");
        recordService.createRecord(record1);
        
        export.exportRecord(response, group1.getId());
        
        List<?> sList = generalSearcher.searchResource("Record", "record3");
		Assert.assertEquals(1, sList.size() );
		Assert.assertEquals("record3-group-1", ((Object[])sList.get(0))[2] );
        
        List<Record> list = recordService.getRecordsByPID(group1.getId(), Environment.getUserId());
		String json = EasyUtils.obj2Json(list);
		String exportPath = DataExport.getExportPath() + "/" + group1.getName() + ".json";
		FileHelper.writeFile(exportPath, json, false);
		
		group1.setName(  group1.getName() + "--" );
		recordService.updateRecord(group1);
        
        AfterUpload servlet = new ImportRecord();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    EasyMock.expect(mockRequest.getParameter("groupId")).andReturn("_root");
	    EasyMock.expect(mockRequest.getParameter("dataSource")).andReturn(DMConstants.LOCAL_CONN_POOL);
	    
	    try {
	        mocksControl.replay(); 
	        servlet.processUploadFile(mockRequest, exportPath, null);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
		 
	    Assert.assertTrue( recordService.getAllRecords().size() >= 4 );
	    
	    export.recordAsReport(Report.DEFAULT_PARENT_ID, group2.getId() + "," + record1.getId());
	    reportAction.getAllReport( new MockHttpServletResponse() );
	}
	
	@Test
	public void testExcel2Record() {
		Record group1 = new Record();
        group1.setType(Record.TYPE0);
        group1.setParentId(Record.DEFAULT_PARENT_ID);
        group1.setName("record3-group-1");
        recordService.createRecord(group1);
        
        String path = URLUtil.getResourceFileUrl("testdata/test_sales.xlsx").getPath();
        callExcel2Record(group1, path);
		 
	    List<?> list = commonDao.getEntities("from Record where name = ?", "销售情况");
	    Assert.assertTrue(list.size() == 1);
	    Record rc = (Record) list.get(0);
	    
	    Map<String, String> params = new HashMap<String, String>();
	    List<Map<String, Object>> result = recordService.getDB(rc.getId()).select(1, 100, params ).result;
	    Assert.assertEquals(31, result.size());
	    log.debug(result.get(0));
	    
	    list = commonDao.getEntities("from Record where name = ?", "图书定价");
	    Assert.assertTrue(list.size() == 1);
	    rc = (Record) list.get(0);
	    
	    result = recordService.getDB(rc.getId()).select(1, 100, params ).result;
	    Assert.assertEquals(17, result.size());
	    log.debug(result.get(0));
	    
	    // 导入xls
	    path = URLUtil.getResourceFileUrl("testdata/test.xls").getPath();
        callExcel2Record(group1, path);
	    
	    // 导入csv
	    String filename = "jk.csv";
	    path = DMUtil.getAttachPath() + "/" + filename;
		
		StringBuffer sb = new StringBuffer("编码,类型,名称,时间\n");
		sb.append("001,1,JK,2015-10-29\n");
		sb.append("002,1,JK,2015-10-29\n");
		
		FileHelper.writeFile(new File(path), sb.toString()); 
		callExcel2Record(group1, path);
		
		// test error
		 try {
			 path = DMUtil.getAttachPath() + "/" + System.currentTimeMillis() + ".csv";
			 AfterUpload servlet = new Excel2Record();
				
		     IMocksControl mocksControl =  EasyMock.createControl();
		     HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
		     EasyMock.expect(mockRequest.getParameter("groupId")).andReturn(group1.getId() + "");
		     EasyMock.expect(mockRequest.getParameter("dataSource")).andReturn(DMConstants.LOCAL_CONN_POOL);
		     mocksControl.replay(); 
		     servlet.processUploadFile(mockRequest, path, null);
			    
			 Assert.fail("should throw ex");
		 } 
		 catch (Exception e) {
			 Assert.assertTrue(e.getMessage().indexOf("No such file") > 0);
		 }
	}

	protected void callExcel2Record(Record group1, String path) {
		AfterUpload servlet = new Excel2Record();
		
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    EasyMock.expect(mockRequest.getParameter("groupId")).andReturn(group1.getId() + "");
	    EasyMock.expect(mockRequest.getParameter("dataSource")).andReturn(DMConstants.LOCAL_CONN_POOL);
	    
	    try {
	        mocksControl.replay(); 
	        String result = servlet.processUploadFile(mockRequest, path, null);
	        log.debug(result);
		} 
	    catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
	}
	
}
