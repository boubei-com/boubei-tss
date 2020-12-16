/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.dml;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record._Recorder;

public class MultiSQLExcutorTest extends AbstractTest4DM {
	
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	@Autowired MultiSQLExcutor mx;
	
	Record record1;
	
	public void setUp() throws Exception {
		super.setUp();
		
		record1 = new Record();
        record1.setType(Record.TYPE1);
        record1.setParentId(Record.DEFAULT_PARENT_ID);
        record1.setName("x_tbl_jx");
        record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
        record1.setTable("x_tbl_jx");
        record1.setDefine("[ " +
        		"{'label':'name', 'code':'name', 'type':'string'}," +
        		"{'label':'score', 'code':'score', 'type':'number'}," +
        		"{'label':'day', 'code':'day', 'type':'date'}" +
        		"]");
        recordService.createRecord(record1); 
		
		//  create sql def
		SQLDef s1 = new SQLDef();
		s1.setCode("s1");
		s1.setScript("insert into x_tbl_jx(name,score,day,createtime,creator,version) values ('${name}', '${score}', '${day}', '${day}', 'Admin', 0)");
		s1.setRemark("test");
		commonDao.create(s1);

		SQLDef s2 = new SQLDef();
		s2.setCode("s2");
		s2.setScript("select nvl(max(id), 1) as maxid from x_tbl_jx");
		commonDao.create(s2);

		SQLDef s3 = new SQLDef();
		s3.setCode("s3");
		s3.setScript("update x_tbl_jx t set t.score = ${score} where t.id = ${maxid}");
		commonDao.create(s3);

		SQLDef s4 = new SQLDef();
		s4.setCode("s4");
		s4.setScript("delete from x_tbl_jx  where id = ${maxid}");
		commonDao.create(s4);

		SQLDef s5 = new SQLDef();
		s5.setCode("s5");
		s5.setScript("select id from x_tbl_jx where id=-1");
		commonDao.create(s5);
		
		SQLDef s6 = new SQLDef();
		s6.setCode("s6");
		s6.setScript("update x_tbl_jx t set t.score = ${score}+1 where t.id = ${id}");
		commonDao.create(s6);
		
		SQLDef s7 = new SQLDef();
		s7.setId(null);
		s7.getCode();
		s7.getRemark();
	}
	
	@Test
	public void test() throws Exception {
		
		String json = "[";
		json += "{ \"sqlCode\": \"s1\", \"data\": {\"name\": \"JK\", \"score\": \"59\", \"day\": \"2017-01-01\"} },";
		json += "{ \"sqlCode\": \"s1\", \"data\": {\"name\": \"Jane\", \"score\": \"99\", \"day\": \"2017-11-01\"} },";
		json += "{ \"sqlCode\": \"s2\", \"data\": {} },";
		json += "{ \"sqlCode\": \"s3\", \"data\": {\"score\": 100} },";
		json += "{ \"sqlCode\": \"s4\"} ]";
		
		Object result = mx.exeMultiSQLs(request, DMConstants.LOCAL_CONN_POOL, json);
		log.debug(result);
		
		// 新增记录后，附带自定义操作
		request.addParameter("name", "Jon");
		request.addParameter("score", "12");
		request.addParameter("day", "2018-05-01");
		request.addParameter("yy", "test");
		
		json = "[{ \"sqlCode\": \"s6\", \"data\": {\"xx\": \"test\"} } ]";
		request.addParameter("_after_", json);
		Long rcId = record1.getId();
		Long newID = (Long) recorder.createAndReturnID(request, rcId);
		Assert.assertEquals(13.0, recordService.getDB(rcId).get(newID).get("score"));
	}

	@Test
	public void testErr() throws Exception {
		try {
			request.setQueryString("json=[]");
			mx.exeMultiSQLs(request, "ERR-DS", "[]");
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals( EX.parse(EX.DM_02, "ERR-DS") , e.getMessage());
        }
		
		request.setQueryString("");
		String json = "[ { \"sqlCode\": \"s3\", \"data\": {} } ]";
		try {
			mx.exeMultiSQLs(request, DMConstants.LOCAL_CONN_POOL, json);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue( e.getMessage().indexOf("freemarker parse error") > 0 );
        }
		
		json = "[ { \"sqlCode\": \"notYet\", \"data\": {} } ]";
		try {
			mx.exeMultiSQLs(request, DMConstants.LOCAL_CONN_POOL, json);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue( e.getMessage().indexOf("SQLDef not exsit") > 0 );
        }
		
		json = "[ { \"sqlID\": \"-12\", \"data\": {} } ]";
		try {
			mx.exeMultiSQLs(request, DMConstants.LOCAL_CONN_POOL, json);
			request.removeParameter("json");
			request.addParameter("json", json);
			Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue( e.getMessage().indexOf("SQLDef not exsit") > 0 );
        }
		
		json = "[ { \"sqlCode\": \"s5\", \"data\": {} } ]";
		Object result = mx.exeMultiSQLs(request, DMConstants.LOCAL_CONN_POOL, json);
		log.debug(result);
	}
}
