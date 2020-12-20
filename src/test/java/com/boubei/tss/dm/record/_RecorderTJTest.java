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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamConstants;

import org.junit.Assert;

public class _RecorderTJTest extends AbstractTest4DM {
	
	@Autowired LogService logService;
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	
	Long recordId;
	
	private void initTable(String tableName) {
		super.init();
		
		String tblDefine = "[ {'label':'门店', 'code':'f1', 'type':'string'}," +
        		"	{'label':'日期', 'code':'f2', 'type':'date'}," +
        		"	{'label':'金额', 'code':'f3', 'type':'number'}," +
        		"	{'label':'提成', 'code':'f4', 'type':'int', 'role2': '老板'}," +
        		"	{'label':'客户', 'code':'f5', 'type':'string'}" +
        		"]";
		
		Record record = new Record();
		record.setName("record" + tableName);
		record.setType(1);
		record.setParentId(0L);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable(tableName);
		record.setDefine(tblDefine);
		record.setShowCreator(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		recordId = record.getId();
	}

	@Test
	public void test() {
		initTable("tj_13");
 
		for(int i = 1; i <= 12; i++) {
			request = new MockHttpServletRequest();
			request.addParameter("_version", String.valueOf(i + 2));
			request.addParameter("f2", "2015-04-05");
			request.addParameter("f3", "12.0");
			request.addParameter("f4", "" + i);
			
			recorder.create(request, response, recordId);
		}
		recorder.export(request, new MockHttpServletResponse(), recordId);
		
		// test 透析，权限过滤掉 f4 字段
		request.removeAllParameters();
		request.addParameter("fields", "f1,f2,sum(f3) as f3, sum(f4) f4, count(*) zhs");
		request.addParameter("groupby", "f1,f2");
		request.addParameter("sortField", "f1,f5,f6");
		recorder.showAsGrid(request, response, recordId, 1);
		recorder.showAsJSON(request, recordId, 1);
		recorder.export(request, response, recordId);
		
		request.addParameter("debugSQL", "true");
		Object sql = recorder.showAsJSON(request, recordId, 1);
		log.info(sql);
		Assert.assertEquals("select f1,f2,sum(f3) as f3, sum(f4) f4, count(*) zhs from  tj_13 where  '000' <> ?  and ( (  1=1  and domain = '无域'  )  or -1 = -1  )  group by f1,f2  order by f1", 
				sql.toString().trim());
	}
}
