/* ==================================================================   
 * Created [2018-05-13] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.file;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.etl.ByIDETLJob;
import com.boubei.tss.dm.etl.Task;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record._Recorder;
import com.boubei.tss.dm.report._Reporter;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.ProgressPool;
import com.boubei.tss.modules.timer.JobDef;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

public class ValidCSVTest extends AbstractTest4DM {
	
    public static void main(String[] args) {
    	String regExp = "^[1][3,4,5,6,7,8,9][0-9]{9}$".replaceAll("\\\\","\\\\\\\\");  // JS 正则转换为 JAVA正则
        Pattern p = Pattern.compile(regExp);  
        System.out.println( regExp + " " + p.matcher("13588833834").matches() );
    }
	
	static String UPLOAD_PATH = FileHelper.ioTmpDir() + "/upload/record/";
	
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	@Autowired _Reporter reporter;
	
	AfterUpload upload = new ImportCSV();
    HttpServletRequest mockRequest;
    
    JobDef job1;
    Task task;
    
	@Test
	public void test() {
		job1 = new JobDef();
		job1.setCode("JOB-123");
		job1.setName("JOB-123");
		job1.setJobClassName(ByIDETLJob.class.getName());
		job1.setTimeStrategy("0 0 1 * * ?");
		commonDao.createObject(job1);
		
		task = new Task();
		task.setName("ETL-TASK-123");
		task.setType("byID");
		task.setJobId(job1.getId());
		task.setJobName(job1.getName());
		task.setSourceDS(getDefaultSource());
		task.setSourceScript("select id from um_user where id > ?");
		task.setStartID(0L);
		task.setStatus("opened");
		task.setTargetDS(getDefaultSource());
		task.setTargetScript( "insert into TBL_TEMP_(id) values(?) " );
		task.setApplier("JK");
		task.setApplyDay(new Date());
		commonDao.createObject(task);
		
		String tblDefine = 
				"[ " +
					"{'label':'编码', 'defaultValue':'SOyyMMddxxxx', 'nullable':'false'}," +
					"{'label':'类型', 'type':'int', 'nullable':'false'}," +
	        		"{'label':'姓名', 'unique': 'true'}," +
	        		"{'label':'工号', 'type':'string', 'nullable':'false', 'valSQL':'select loginName as f4 from um_user where userName like ^%${f3}%^ '}," +
	        		"{'label':'邮箱', 'type':'string', 'checkReg': ''}," +  // \w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*
	        		"{'label':'手机', 'type':'string', 'checkReg': '^[1][3,4,5,6,7,8,9][0-9]{9}$', 'errorMsg':'手机格式错误'}," + 
	        		"{'label':'时间', 'type':'datetime', 'nullable':'true', 'defaultValue':'2018-01-01'}," +
	        		"{'label':'工号2', 'type':'string', 'nullable':'true', 'valSQL':'select loginName as f4 from um_user where userName like ^%${f3}%^ '}" +
        		"]";
		
		Record record = new Record();
		record.setName("record-2-csv");
		record.setType(1);
		record.setParentId(0L);
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable("x_tbl_icsv_2");
		record.setDefine(tblDefine);
		record.setBatchImp(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		Long recordId = record.getId();
 
		String filename = "2.csv";
		String filepath = UPLOAD_PATH + "/" + filename;
		
		StringBuffer sb = new StringBuffer("编码,类型,姓名,邮箱,手机,时间\n");
		
		sb.append(",,min,,,2015-10-01\n");    // 必填字段f2为空
		sb.append(",1,Admin,jk@boubei.com,13588833834,\n");
		sb.append(",1,Admin,boubei.com,12588833834,2015-10-19\n");   // 违反姓名唯一性
		sb.append(",1,A,boubei.com,12588833834,2015-10-19\n");       // 匹配多个工号
		
		sb.append(",2,王五,xx@boubei.com,12588833834,2015-10-19,x,y,z\n"); // 列数大于表头
		sb.append(",2,李四,,,2015-1001\n");    // 日期格式异常
		sb.append(",,,,,\n");                 // 空行
		sb.append(",2,张\n三,,,2015-10-19\n"); // 值存在换行
		
		FileHelper.writeFile(new File(filepath), sb.toString()); 
		
	    try {
	    	String headerTL = "编码:编码,类型:类型,姓名:姓名,邮箱:邮箱,手机:手机,时间:时间";
			mockRequest = mockRequest(recordId, null, null, "false", "UTF-8", headerTL);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			
			mockRequest = mockRequest(recordId, "f3", null, "true", "UTF-8", null);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			
			
			sb = new StringBuffer("编码,XXX类型,姓名,邮箱,手机,时间\n"); // XXX类型 为错误列名
			sb.append(",1,Admin,jk@boubei.com,13588833834,\n");
			FileHelper.writeFile(new File(filepath), sb.toString());
			
			mockRequest = mockRequest(recordId, null, null, "false", "UTF-8", null);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			
			record.setBatchImp(ParamConstants.FALSE);
			commonDao.update(record);
			MockHttpServletRequest request2 = new MockHttpServletRequest();
			request2.addParameter("record", record.getTable());
			String rt = upload.processUploadFile(request2, filepath, filename);
			Assert.assertEquals("parent.alert('【record-2-csv】不允许批量导入');", rt);
		} 
	    catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
	    
		_Database _db = _Database.getDB(record);
		SQLExcutor ex = _db.select(1, 100, null);
		Assert.assertEquals( 1, ex.count );
		Assert.assertEquals( "13588833834", ex.result.get(0).get("f6") );
		
		new EmptyDataVaild().vaild(_db, null, null, null, null, null, null);
		
		// 下载异常文件
//		reporter.download(response, fileName);
    }
    
    private HttpServletRequest mockRequest(Long recordId, String uniqueCodes, String ignoreExist, String together, String charSet, String headerTL) {
    	IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    
	    EasyMock.expect(mockRequest.getParameter("vailderClass")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("record")).andReturn("x_tbl_icsv_1");
    	EasyMock.expect(mockRequest.getParameter("recordId")).andReturn(recordId.toString());
	    EasyMock.expect(mockRequest.getParameter("petName")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("ignoreExist")).andReturn(ignoreExist);
	    EasyMock.expect(mockRequest.getParameter("uniqueCodes")).andReturn(uniqueCodes).anyTimes();
	    EasyMock.expect(mockRequest.getParameter(DataExport.CHARSET)).andReturn(charSet);
	    EasyMock.expect(mockRequest.getParameter("together")).andReturn(together);
	    EasyMock.expect(mockRequest.getParameter("callback")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("headerTL")).andReturn(headerTL);
	    
	    EasyMock.expect(mockRequest.getParameter("jobAfterImport")).andReturn(job1.getCode());
	    EasyMock.expect(mockRequest.getParameter("etlAfterImport")).andReturn(task.getName());
	    EasyMock.expect(mockRequest.getParameter("pgCode")).andReturn(null).anyTimes();;
	    
	    EasyMock.expect(mockRequest.getHeader("http-client")).andReturn(null);
	    
	    HashMap<String, String[]> params = new HashMap<String, String[]>();
		EasyMock.expect(mockRequest.getParameterMap()).andReturn(params);
	    EasyMock.expect(mockRequest.getQueryString()).andReturn("x=123"+EasyUtils.checkTrue(headerTL != null, "&headerTL=" + headerTL, ""));
	    
	    mocksControl.replay();
	    return mockRequest;
    }
}
