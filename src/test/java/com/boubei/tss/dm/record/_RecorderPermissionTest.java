/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import java.io.File;
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
import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.file.CreateAttach;
import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

public class _RecorderPermissionTest extends AbstractTest4DM {
	
	@Autowired LogService logService;
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	
	Long recordId;
	
	private void initTable(String tableName) {
		super.init();
		
		String tblDefine = "[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'f2', 'type':'string'}," +
        		"{'label':'附件', 'code':'f4', 'type':'file'}," +
        		"{'label':'时间', 'code':'f3', 'type':'datetime', 'nullable':'false'}]";
		
		Record record = new Record();
		record.setName("record" + tableName);
		record.setType(1);
		record.setParentId(0L);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable(tableName);
		record.setDefine(tblDefine);
		
		record.setNeedLog(ParamConstants.TRUE);
		record.setNeedFile(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		recordId = record.getId();
	}

	@Test
	public void test() {
		initTable("x_tbl_14");
		
		SecurityUtil.LEVEL_6 = 2;
		runTest();
		SecurityUtil.LEVEL_6 = 6;
	}
	
	public void runTest() {
		Assert.assertNotNull(recorder.getDefine(request, recordId));
		
		request.addHeader(RequestContext.REQUEST_TYPE, RequestContext.XMLHTTP_REQUEST);
		
		request.addParameter("f1", "10.9");
		request.addParameter("f2", "just test");			
		request.addParameter("f3", "2015-04-05");
		recorder.create(request, response, recordId);
		List<Map<String, Object>> result = recorder.getDB(recordId).select(1, 100, null).result;
		Assert.assertTrue(result.size() == 1);
		
		Map<String, Object> recordItem = result.get(0);
		Long itemId = EasyUtils.obj2Long(recordItem.get("id"));
		uploadDocFile(recordId, itemId);
		
		Long itemId2 = recorder.createAndReturnID(request, recordId);
		uploadDocFile(recordId, itemId2);
		
		List<?> attachList = recorder.getAttachList(request, recordId, itemId);
		Assert.assertTrue(attachList.size() == 1);
		
		RecordAttach ra = (RecordAttach) attachList.get(0);
		
		try {
			recorder.getAttachList(request, recordId, 0L);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(EX.DM_08, e.getMessage());
		}
		
		// starting...............
		logout();
		
		try {
			recordService.getRecordID("x_tbl_14", 1, true);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertEquals(EX.parse(EX.DM_14, "x_tbl_14"), e.getMessage());
		}
		
		try {
			recorder.create(request, response, recordId);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		
		try {
			recorder.createAndReturnID(request, recordId);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		
		try {
			_TestUtil.mockPermission("dm_permission_record", "rc1", recordId, -10000L, Record.OPERATION_CDATA, 1, 0, 0);
			recorder.update(request, response, recordId, 1L);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		} finally {
			SQLExcutor.excute("delete from dm_permission_record where roleId=-10000", getDefaultSource());
		}
		
		
		try {
			recorder.updateBatch(request, response, recordId, "1,2", "f1", "1212");
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		
		// test upload record attach	
		try {
			recorder.getAttachList(request, recordId, itemId);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		try {
			recorder.getAttachListXML(request, response, recordId, itemId);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		
		try {
			recorder.downloadAttach(request, new MockHttpServletResponse(), ra.getId(), null);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		
		try {
			recorder.deleteAttach(request, response, ra.getId());
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		
		try {
			ra.setItemId(System.currentTimeMillis());
			commonDao.update(ra);
			recorder.downloadAttach(request, new MockHttpServletResponse(), ra.getId(), null);
		} 
		catch(Exception e) {
			Assert.fail(); // 该附件不存在，可能已被删除!
		}

		// test delete record
		try {
			recorder.delete(request, response, recordId, 1L);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
		try {
			recorder.deleteBatch(request, response, recordId, "1,2");
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) {
			Assert.assertTrue("权限不足", true);
		}
	}
	
	static String UPLOAD_PATH = FileHelper.ioTmpDir() + "/upload/record/";
	
	 // 上传附件
    private void uploadDocFile(Long recordId, Object itemId) {
    	AfterUpload upload = new CreateAttach();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    
	    EasyMock.expect(mockRequest.getParameter("record")).andReturn(recordId.toString());
	    EasyMock.expect(mockRequest.getParameter("recordId")).andReturn(recordId.toString());
	    EasyMock.expect(mockRequest.getParameter("itemId")).andReturn(itemId.toString());
	    EasyMock.expect(mockRequest.getParameter("type")).andReturn(RecordAttach.ATTACH_TYPE_DOC.toString());
	    EasyMock.expect(mockRequest.getParameter("petName")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("uploadField")).andReturn("f4");
	    EasyMock.expect(mockRequest.getParameter("refreshGrid")).andReturn( EasyUtils.obj2Int(itemId) % 2 == 1 ? null : "true" );
	    
	    try {
	    	String filename = "123.txt";
			String filepath = UPLOAD_PATH + "/" + filename;
			FileHelper.writeFile(new File(filepath), "卜贝求真。");
	        
	        mocksControl.replay(); 
			upload.processUploadFile(mockRequest, filepath, filepath);
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
	    
	    _TestUtil.printEntity(super.permissionHelper, "RecordAttach"); 
    }
	
}
