/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ddl;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.Record;

public class _H2Test {

	@Test
	public void test1() {
		Record record = new Record();
		_Database h2 = new _H2( record );
		
		List<Map<String, Object>> list = h2.parseJson(null);
		Assert.assertTrue(list.isEmpty());
		
		h2.createTable();
		new _Oracle( record ).createTable();
		
		try {
			h2.parseJson("{ wrong json, ]");
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) { }
		
		try {
			h2.dropTable("X_T_1", DMConstants.LOCAL_CONN_POOL);
		} 
		catch(Exception e) { }
		
		try {
			new _Oracle(record).dropTable("X_T_2", DMConstants.LOCAL_CONN_POOL);
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) { }
		
		try {
			_Database.getDBType("not exists ds");
			Assert.fail("should throw exception but didn't.");
		} 
		catch(Exception e) { }
		
		Assert.assertNotNull( h2.getRcEvent() );
		h2.remark = "rcEventClass:=com.boudata.wmsx.ev.WRecordEvent";
		Assert.assertNotNull( h2.getRcEvent() );
		h2.remark = "rcEventClass:=com.boudata.wmsx.ev.XXX";
		Assert.assertNotNull( h2.getRcEvent() );
	}

}
