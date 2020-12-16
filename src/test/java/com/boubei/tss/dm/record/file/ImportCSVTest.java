/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.file;

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
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.Excel;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record._Recorder;
import com.boubei.tss.dm.report._Reporter;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.ProgressPool;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.FileHelper;

public class ImportCSVTest extends AbstractTest4DM {
	
	static String UPLOAD_PATH = FileHelper.ioTmpDir() + "/upload/record/";
	static int SIZE = 10 * 10000;  // 10万 12秒
	
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	@Autowired _Reporter reporter;
	
	AfterUpload upload = new ImportCSV();
    HttpServletRequest mockRequest;
	
	@Test
	public void test() {
		String tblDefine = 
				"[ " +
					"{'label':'编码', 'defaultValue':'SOyyMMddxxxx', 'nullable':'false'}," +
					"{'label':'类型', 'type':'number', 'nullable':'false', 'defaultValue':'1'}," +
	        		"{'label':'名称', 'type':'string'}," +
	        		"{'label':'时间', 'type':'datetime', 'nullable':'true'}" +
        		"]";
		
		Record record = new Record();
		record.setName("record-1-csv");
		record.setType(1);
		record.setParentId(0L);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable("x_tbl_icsv_1");
		record.setDefine(tblDefine);
		record.setNeedLog(ParamConstants.FALSE);
		
		recordService.createRecord(record);
		Long recordId = record.getId();
		
		importCSVData(recordId);
		importCSVDataError(recordId);
		
		_Database _db = _Database.getDB(record);
		SQLExcutor ex = _db.select(1, 100, null);
		Assert.assertEquals( SIZE + 1, ex.count );
		
		importXLSData(recordId);
		
		ex = _db.select(1, 100, null);
		Assert.assertEquals( SIZE + 1 + 6, ex.count );
		
		// 测试单行插入时自动取号
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("f2", "12.12");
		valuesMap.put("f4", "2018-03-20 9:33:12");
		Long id = _db.insertRID(valuesMap);
		Assert.assertNotNull( _db.get(id).get("f1") );
		
		// 下载导入模板
		recorder.getImportTL(response, recordId);
		
		response = new MockHttpServletResponse();
		record.setRemark(DMConstants.IMPORT_TL_FIELDS + ":=类型,名称\n" + DMConstants.IMPORT_TL_IGNORES + ":=编码 名称");
		commonDao.update(record);
		CacheHelper.getLongCache().destroyByKey( _Database._CACHE_KEY( record.getId() ) );
		recorder.getImportTL(response, recordId);
	}
	
    private void importCSVData(Long recordId) {
	    try {
	    	String filename = "1.csv";
			String filepath = UPLOAD_PATH + "/" + filename;
			
			StringBuffer sb = new StringBuffer("*编码*,类型,名称,时间\n");

			// 压力测试 一次导入10万
			for(int i = 3; i <= SIZE-2; i++) {
				sb.append("," + i + ",heihei,2015-11-27\n");
			}
			
			sb.append(",1,哈哈,2015-10-29\n");
			sb.append(",2,hehe,2015-10-19\n");
			sb.append(",,,\n"); // 测试是否自动过滤空行
			
			FileHelper.writeFile(new File(filepath), sb.toString()); 
	        
			mockRequest = mockRequest(recordId, null, null, null, "UTF-8", null);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			
			List<Map<String, Object>> data = SQLExcutor.queryL("select * from x_tbl_icsv_1");
			Assert.assertEquals(SIZE - 2, data.size());
			Map<String, Object> lastItem = data.get(SIZE - 3);
			Assert.assertTrue( lastItem.get("f1").toString().endsWith("" + (SIZE-2) ) );
			System.out.println( lastItem );
			
			// 测试部分导入 和 覆盖式导入
			sb = new StringBuffer("编码,类型,时间\n");
			sb.append(lastItem.get("f1") + ",1," +lastItem.get("f4")+ "\n");  // 覆盖
			sb.append(lastItem.get("f1") + ",1,2018-08-19\n");  // 新增
			sb.append(",1,2015-10-19\n");  // 新增
			sb.append(",2,\n");  // 新增
			
			FileHelper.writeFile(new File(filepath), sb.toString()); 
	        
	        mockRequest = mockRequest(recordId, "f1,f4", null, null, null, null);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			
			// 测试忽略已经存在的导入
			sb = new StringBuffer("编码,类型,时间\n");
			sb.append(lastItem.get("f1") + ",1," +lastItem.get("f4")+ "\n");  // 忽略
			
			FileHelper.writeFile(new File(filepath), sb.toString()); 
	        
	        mockRequest = mockRequest(recordId, "f1,f4", "true", null, "UTF-8", null);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			
			data = SQLExcutor.query(DMConstants.LOCAL_CONN_POOL, "select * from x_tbl_icsv_1");
			Assert.assertEquals(SIZE + 1, data.size());
//			Assert.assertEquals( DateUtil.format((Date) data.get(SIZE - 3).get("f4")), "2018-10-29" );
			System.out.println( lastItem );
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
    }
    
    private void importXLSData(Long recordId) {
	    try {
	    	String filename = "11.csv";
			String filepath = UPLOAD_PATH + "/" + filename;
			
			StringBuffer sb = new StringBuffer("多余列1,编码哈,名称哈,多余列2,时间哈,,多余列3\n");
			for(int i = 0; i < 5; i++) {
				sb.append(",,heihei,多余,2015-11-27,多余值1,多余值2,多余值3,多余值4\n");  // 行数据列多于表头列
			}
			sb.append(",,heihei\n"); // 行数据列小于表头列
			sb.append(",,,,,\n"); // 测试是否自动过滤空行
			
			// csv 
			FileHelper.writeFile(new File(filepath), sb.toString()); 
						
			// 自定义表头转换
			String headerTL = "编码:编码哈,类型:{123},名称:名称哈,时间:时间哈";
			mockRequest = mockRequest(recordId, null, null, null, "UTF-8", headerTL);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) ); 
			/* 导入失败: 6,数据异常，数据列和表头列数量不等。请用记事本打开导入文件，检查此行数据里是否存在换行符,,heihei,123 */
			
			headerTL = "编码:编码哈,类型:{123},名称:名称哈,时间:时间哈,创建人:${userCode}";
			mockRequest = mockRequest(recordId, null, null, null, "UTF-8", headerTL);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) ); 
			/* 导入失败: 列【创建人】名称在数据表定义里不存在 */
			
			try {
				headerTL = "编码:编码哈,类型{123},名称:名称哈,时间:时间哈,创建人:${userCode}";
				mockRequest = mockRequest(recordId, null, null, null, "UTF-8", headerTL);
				upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) ); 
				Assert.fail("");
			} catch(Exception e) {
				Assert.assertTrue( e.getMessage().indexOf("附近配置有误") > 0 );
			}
			
			try {
				headerTL = "编码:编码哈,类型:{123},名称:名称哈,时间:时间哈,创建人:${userCode},addr:地址,tel:电话|手机,sex:性别:{女}"; // sex:性别:{女} : “女”为默认值，当Excel里无“性别”列时
				mockRequest = mockRequest(recordId, null, null, null, "UTF-8", headerTL);
				upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) ); 
				Assert.fail("");
			} catch(Exception e) {
				Assert.assertTrue( e.getMessage().indexOf("导入文件里数据列[地址, 电话|手机]缺失") >= 0 );
			}
			
			// excel
			headerTL = "编码:编码哈|编码呵,类型:{123},名称:名称哈,时间:时间哈";
			filepath = Excel.csv2Excel(filepath, "UTF-8");
			mockRequest = mockRequest(recordId, null, null, null, "UTF-8", headerTL);
			upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			
			// check result
			List<Map<String, Object>> data = SQLExcutor.queryL("select * from x_tbl_icsv_1 order by id desc");
			Map<String, Object> lastItem = data.get(0);
			Assert.assertEquals(123.0, lastItem.get("f2"));
			System.out.println( lastItem );
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
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
	    EasyMock.expect(mockRequest.getParameter(DataExport.CHARSET)).andReturn(charSet).anyTimes();
	    EasyMock.expect(mockRequest.getParameter("together")).andReturn(together);
	    EasyMock.expect(mockRequest.getParameter("callback")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("headerTL")).andReturn(headerTL);
	    EasyMock.expect(mockRequest.getParameter("etlAfterImport")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("jobAfterImport")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("pgCode")).andReturn(null).anyTimes();;
	    EasyMock.expect(mockRequest.getHeader("http-client")).andReturn(null);
	    
	    HashMap<String, String[]> params = new HashMap<String, String[]>();
	    params.put("headerTL", new String[] {headerTL});
		EasyMock.expect(mockRequest.getParameterMap()).andReturn(params);
	    EasyMock.expect(mockRequest.getQueryString()).andReturn("x=123");
	    
	    mocksControl.replay();
	    return mockRequest;
    }
	    
    // 导入Excel出错
    private void importCSVDataError(Long recordId) {

	    try {
	    	String filename = "2.csv";
			String filepath = UPLOAD_PATH + "/" + filename;
			
			StringBuffer sb = new StringBuffer("编码,类型,名称,时间\n");
			sb.append(",1,ha\nha,2015-10-29\n"); // 值内存在换行符 \n，致使表头列和数据列不等
//			sb.append(",haha,2015-10-29\n");    // Test number类型为空
			
			FileHelper.writeFile(new File(filepath), sb.toString()); 
	        
			mockRequest = mockRequest(recordId, null, null, "true", "UTF-8", null);
			String result = upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			Assert.assertTrue(result.indexOf("导入失败，其中有2行数据校验出异常") > 0 );
			
			// 导入空模板
			sb = new StringBuffer("编码,类型,名称,时间\n");
			FileHelper.writeFile(new File(filepath), sb.toString()); 
	        
			mockRequest = mockRequest(recordId, null, null, "true", "UTF-8", null);
			result = upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			Assert.assertTrue(result.indexOf("导入文件没有数据") >= 0 );
			
			// 没有权限
			logout();
			SecurityUtil.LEVEL_6 = 2;
			mockRequest = mockRequest(recordId, null, null, "true", "UTF-8", null);
			result = upload.processUploadFile(mockRequest, filepath, filename); ProgressPool.finish( new Progress(100) );
			Assert.assertTrue(result.indexOf("批量导入失败，权限不足") >= 0 );
			SecurityUtil.LEVEL_6 = 6;
			login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
	    } 
	    catch (Exception e) {
	    	log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
	    }
    }
}
