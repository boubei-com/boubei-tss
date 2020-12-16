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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._MySQL;
import com.boubei.tss.dm.dml.SQLExcutor;

public class BuildTableTest {
	
	@Test
	public void test() {
		String tblDefine = "[ {'label':'类型', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'type':'string'}," +
        		"{'label':'时间', 'type':'datetime', 'nullable':'false'}]";
		
		Record record = new Record();
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable("x_tbl_1");
		record.setDefine(tblDefine);
		
		_Database _db = new _MySQL(record);
		_db.createTable();
		
   		SQLExcutor.excute("insert into x_tbl_1(f1,f2,f3,createtime,creator,updatetime,updator,version) values (1.1, 'test', now(), now(), 'JK', now(), 'JK2', 0)", DMConstants.LOCAL_CONN_POOL);
   		
   		String insertSQL = "insert into x_tbl_1(f1,f2,f3,createtime,creator,updatetime,updator,version) values (?, ?, ?, ?, ?, ?, ?, ?)";
   		for(int i=0; i < 10; i++) {
   			Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
   			paramsMap.put(1, 10.2 + i);
   			paramsMap.put(2, "just test");
   			paramsMap.put(3, new Date());
   			paramsMap.put(4, new Date());
   			paramsMap.put(5, "JK" + i);
   			paramsMap.put(6, new Date());
   			paramsMap.put(7, "JK2" + i);
   			paramsMap.put(8, 0);
   			SQLExcutor.excute(insertSQL, paramsMap, DMConstants.LOCAL_CONN_POOL);
   		}
   		
   		SQLExcutor ex = new SQLExcutor();
   		ex.excuteQuery("select f1, id from x_tbl_1", null, 1, 10, DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() > 0);
		
		for(Map<String, Object> row : ex.result) {
			System.out.println(row.get("id"));
		}
	}

}
