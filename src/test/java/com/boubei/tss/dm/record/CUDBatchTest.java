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

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamConstants;

public class CUDBatchTest extends AbstractTest4DM {
	
	@Autowired LogService logService;
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	
	Long recordId;
	
	private void initTable(String tableName) {
		super.init();
		
		String tblDefine = "[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'f2', 'type':'string'}," +
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
	public void test1() {
		initTable("x_tbl_0525");
 
		SecurityUtil.LEVEL_6 = 2;
		runTest();
		SecurityUtil.LEVEL_6 = 6;
	}
	
	public void runTest() {
		Assert.assertNotNull(recorder.getDefine(request, recordId));
		
		recorder.getCustomizeFields(request, "wms_sku", null);
		
		request.addParameter("f1", "10.1");
		request.addParameter("f2", "test1");
		request.addParameter("f3", "2017-05-01");
		recorder.createAndReturnID(request, recordId);
		
		request = new MockHttpServletRequest();
		request.addParameter("f1", "20.1");
		request.addParameter("f2", "test2");
		request.addParameter("f3", "2017-05-02");
		recorder.createAndReturnID(request, recordId);
		
		List<Map<String, Object>> result = recorder.getDB(recordId).select(1, 100, null).result;
		Assert.assertTrue(result.size() == 2);	
		
		// test csv
		String data = "id,f1,f2,f3\n";
		data += ",30.1,test3,2017-05-03\n";
		data += ",40.1,test4,2017-05-04\n";
		data += "1,11.1,test-u,2016-05-01\n";
		data += "2,,,";
		log.debug(data);
		
		request = new MockHttpServletRequest();
		request.addParameter("csv", data);
		try {
			Object msg = recorder.cudBatch(request, recordId);
			log.debug("------------------------ " + msg);
		} catch (Exception e) {
			log.error("recorder.cudBatch error", e);
			Assert.fail(e.getMessage());
		}
		
		// test json
		String json = "[";
		json += "{\"f1\": 50.1,\"f2\": \"test5\",\"f3\": \"2018-08-26\"}," ;
		json += "{\"f1\": 50.2,\"f2\": \"test6\",\"f3\": \"2018-08-27\"}," ;
		json += "{\"id\": 3, \"f1\": \"60.2\",\"f2\": \"test-updae\",\"f3\": \"2018-08-26\"},";
		json +=  "{\"id\": \"1\"}" ;
		json += "]";
		
		request = new MockHttpServletRequest();
		request.addParameter("json", json);
		try {
			Object msg = recorder.cudBatchJSON(request, recordId);
			log.debug("------------------------ " + msg);  // 新增两行、修改一行、删除一行
		} catch (Exception e) {
			log.error("recorder.cudBatchJSON error", e);
			Assert.fail(e.getMessage());
		}
		
		result = recorder.getDB(recordId).select(1, 100, null).result;
		Assert.assertTrue(result.size() == 4);	
		
		log.debug(result);
	}
}
