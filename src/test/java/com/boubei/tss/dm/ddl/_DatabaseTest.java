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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLDef;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.log.LogQueryCondition;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.EasyUtils;

public class _DatabaseTest extends AbstractTest4DM  {
	
	@Test
	public void testMySQL() {
		testDB("MySQL", DMConstants.LOCAL_CONN_POOL); // 暂通过H2模拟
	}
	
	@Test
	public void testPostgreSQL() {
		testDB("PostgreSQL", DMConstants.LOCAL_CONN_POOL); // 暂通过H2模拟
	}
	
	@Test
	public void testSQLServer() {
		testDB("SQL Server", DMConstants.LOCAL_CONN_POOL); // 暂通过H2模拟
		
		String tblDefine = "[ {'label':'域', 'code':'domain'} ]";
		
		Record record = new Record();
		record.setId(-2L);
		record.setName("SQLServer_tbl_22");
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable("SQLServer_tbl_22");
		record.setDefine(tblDefine);
		
		_Database _db = _Database.getDB("SQL Server", record);
		try {
			_db.createTable();
			Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("Duplicate column name 'domain'", true);
	    }
	}
	
	@Test
	public void testOracle() {
		testDB("Oracle", DMConstants.LOCAL_CONN_POOL); // 暂通过H2模拟
		
		String tblDefine = "[ {'label':'域', 'code':'domain'} ]";
		
		Record record = new Record();
		record.setId(-2L);
		record.setName("Oracle_tbl_22");
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable("Oracle_tbl_22");
		record.setDefine(tblDefine);
		
		_Database _db = _Database.getDB("Oracle", record);
		try {
			_db.createTable();
			Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("Duplicate column name 'domain'", true);
	    }
	}
	
	private void testDB(String type, String datasource) {
		// test 检查 label、code重名
		String tblDefine0 = "[ {'label':'字段一', 'code':'f1'}, {'label':'字段二', 'code':'f1'}]";
		
		Record record0 = new Record();
		record0.setId(-2L);
		record0.setName(type + "_tbl_222");
		record0.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record0.setTable(type + "_tbl_222");
		record0.setDefine(tblDefine0);
		
		try {
			_Database.getDB(type, record0);
			Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	String msg = EX.parse(EX.DM_25, "f1"); 
	    	Assert.assertEquals( EX.parse(EX.DM_15, record0.getName(), msg) , e.getMessage());
	    }
		
		tblDefine0 = "[ {'label':'字段一', 'code':'f1'}, {'label':'字段一', 'code':'f2'}]";
		record0.setDefine(tblDefine0);
		try {
			_Database.getDB(type, record0);
			Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	String msg = EX.parse(EX.DM_26, "字段一"); 
	    	Assert.assertEquals( EX.parse(EX.DM_15, record0.getName(), msg) , e.getMessage());
	    }
		
		tblDefine0 = "[ {'label':'ID', 'code':'id'}, {'label':'字段一', 'code':'f2'}]";
		record0.setDefine(tblDefine0);
		try {
			_Database.getDB(type, record0);
			Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	String msg = EX.parse(EX.DM_34, "id"); 
	    	Assert.assertEquals( EX.parse(EX.DM_15, record0.getName(), msg) , e.getMessage());
	    }
		
		// test normal
		String tblDefine = "[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false', 'sort': 'desc'}," +
        		"{'label':'名称', 'code':'f2', 'type':'string', 'height': '180px', 'cwidth': '10', 'unique': 'true', 'isparam': 'true'}," +
        		"{'label':'时间', 'type':'date', 'nullable':'true', 'role2':'12,13', 'cwidth': '0px'}," +
        		"{'label':'数量', 'code':'f4', 'type':'int', 'cwidth': '10px', 'options':{'codes':'1|2|3','names':'一|俩|仨'}} ]";
		
		Record record = new Record();
		record.setId(1L);
		record.setDatasource(datasource);
		record.setTable("tbl_2_" + type.split(" ")[0]);
		record.setName(record.getTable());
		record.setDefine(tblDefine);
		record.setNeedFile( ParamConstants.FALSE );
		
		_Database _db = _Database.getDB(type, record);
		_db.createTable();
		_db.createUniqueAndIndex();
		_db.createTable(); // 连续创建两次，第二次不会再创建新表，直接连接第一次已经创建的表
		_db.getGridTemplate();
		
		String[] objs = _db.createNames("f123456789123456789123456789");
		Assert.assertTrue( objs[0].length() == 24 );
		Assert.assertTrue( objs[1].length() == 24 );
		
		List<Map<String, Object>> fields = _db.getFields();
		Assert.assertEquals(4, fields.size());
		Assert.assertEquals("f3", fields.get(2).get("code"));
		Assert.assertEquals(2550, _Field.getVarcharLength(fields.get(1)));
		
		// test update table with change table name
		record = new Record();
		record.setId(2L);
		record.setDatasource(datasource);
		record.setTable( "tbl_3_" + type.split(" ")[0]);
		record.setDefine(tblDefine);
		record.setNeedFile( ParamConstants.TRUE );
		record.setNeedLog( ParamConstants.FALSE );
		record.setNeedQLog( ParamConstants.FALSE );
		record.setCustomizeTJ("1=1<#if 1=0>showCUV & ignoreDomain</#if>");
		
		_db.alterTable(record);
		_db.getGridTemplate();
		_db.select(1, 100, null);
		
		// test alter table with row = 0 （没有数据）
		tblDefine = "[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'true', 'sort': 'desc'}," + //  not null --> null
        		"{'label':'名称', 'code':'f2', 'type':'string', 'nullable':'false'}," +
        		"{'label':'时间', 'type':'datetime', 'unique': 'true', 'isparam': 'true'}," +
        		"{'label':'数量', 'code':'f6', 'type':'int'}," +
        		"{'label':'UDF', 'code':'f5', 'type':'string'}]";
		record = new Record();
		record.setId(3L);
		record.setDatasource(datasource);
		record.setTable("tbl_3_" + type.split(" ")[0]);
		record.setName(record.getTable());
		record.setDefine(tblDefine);
		record.setNeedQLog( ParamConstants.TRUE );
		record.setCustomizeTJ(" and creator = '${userCode}' ");
		
		_db.alterTable(record);
		
		// test insert
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("f1", "10.9");
		valuesMap.put("f2", "just test");
		valuesMap.put("f3", "2015-04-05");
		valuesMap.put("f6", "123");
		_db.insert(valuesMap);
		_db.insertBatch(null);
		
		List<Map<String, Object>> result = _db.select(1, 100, null).result;
		Assert.assertTrue(result.size() == 1);
		
		Map<String, Object> row = result.get(0);
		Long id = EasyUtils.obj2Long( row.get("id") );
		Assert.assertEquals(new Double(10.9), EasyUtils.obj2Double(row.get("f1")) );
		Assert.assertNotNull(row.get("createtime"));
		Assert.assertEquals(0, EasyUtils.obj2Int( row.get("version") ).intValue());
		
		Map<String, Object> old = _db.get(id);
		Assert.assertEquals(new Double(10.9), EasyUtils.obj2Double(row.get("f1")) );
		
		// test update
		valuesMap = new HashMap<String, String>();
		valuesMap.put("f1", "12");
		valuesMap.put("f2", "just test");
		valuesMap.put("f3", "2015-04-05");
		_db.update(id, valuesMap);
		
		Map<String, Object> item = _db.get(id);
		Assert.assertEquals(new Double(12.0), EasyUtils.obj2Double(item.get("f1")) );
		
		// rollback
		_db.rollback(id, old);
		item = _db.get(id);
		Assert.assertEquals(new Double(10.9), EasyUtils.obj2Double(item.get("f1")) );
		
		// 再改回去
		_db.update(id, valuesMap);
		
		// select
		result = _db.select(1, 100, null).result;
		Assert.assertTrue(result.size() == 1);
		row = result.get(0);
		Assert.assertEquals(new Double(12.0), EasyUtils.obj2Double(row.get("f1")) );
		Assert.assertEquals("just test", row.get("f2"));
		Assert.assertNotNull(row.get("updatetime"));
		Assert.assertEquals(1, EasyUtils.obj2Int( row.get("version") ).intValue());
		
		// test alter table with row > 0 （有数据）
		tblDefine = "[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false', 'sort': 'desc'}," + //  null --> not null
        		"{'label':'名称', 'code':'f2', 'type':'string', 'height':'100px'}," +
        		"{'label':'时间2', 'code':'f4', 'type':'datetime', 'nullable':'false'}," +
        		"{'label':'UDF2', 'code':'fx', 'type':'string', 'strictQuery':'true'}]";
		
		record = new Record();
		record.setId(4L);
		record.setDatasource(datasource);
		record.setTable("tbl_3_" + type.split(" ")[0]);
		record.setName(record.getTable());
		record.setDefine(tblDefine);
		
		_db.alterTable(record);
		result = _db.select(1, 100, null).result;
		Assert.assertTrue(result.size() == 1);
		
		// test insert
		valuesMap = new HashMap<String, String>();
		valuesMap.put("f1", "10.9");
		valuesMap.put("f2", "just test 2");
		valuesMap.put("f4", "2015-04-05");
		valuesMap.put("fx", "正常");
		_db.insert(valuesMap);
		
		// test select
		Map<String, String> params = new HashMap<String, String>();
		params.put("fx", "is null");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 1);
		Assert.assertNull(result.get(0).get("fx"));
		
		params.put("fx", "is not null");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 1);
		Assert.assertNotNull(result.get(0).get("fx"));
		
		params.put("fx", "正常");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 1);
		Assert.assertEquals("正常", result.get(0).get("fx"));
		
		params.put("fx", "正"); // fx字段不支持模糊查询
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 0);
		
		params = new HashMap<String, String>();
		params.put(_Field.STRICT_QUERY, "false");
		params.put("f2", "just test");
		params.put("creator", Environment.getUserCode());
		params.put("createTime", "[2018-01-01,2099-12-31]");
		
		params.put("sortField", "f4 desc,f1 asc");
		params.remove("sortType");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 2);
		
		params.put(_Field.STRICT_QUERY, "false");
		params.put("createTime", "2018-01-01");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 2);
		
		params.put(_Field.STRICT_QUERY, "false");
		params.put("sortField", "f4,f1");
		params.put("sortType", "desc");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 2);
		
		params.put("f2", "just test,abcd,1234");
		params.put("sortField", "f4");
		params.put("sortType", "onlynull");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 1);
		
		params.put("f2", "just test,just test 2");
		params.put("sortType", "notnull");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 1);
		
		params.remove("sortType");
		params.put("f4", "[2015-04-04,2015-04-06]");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 1);
		
		params.put(_Field.STRICT_QUERY, "true"); // 精确查询
		params.put("f2", "no test");
		params.put("updator", UMConstants.ADMIN_USER);
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 0);
		
		result = _db.select(1, 10, null).result;
		Assert.assertTrue(result.size() > 0);
		
		params = new HashMap<String, String>();
		params.put("othercondition", " and f4 between f4 and f4 "); // othercondition
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() >= 1);
		
		params = new HashMap<String, String>();
		params.put("f2", "");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() > 0);
			        
		params = new HashMap<String, String>();
		params.put("f4", "[2015-04-04,]");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 0);
			        
		params = new HashMap<String, String>();
		params.put("f4", "[,2015-04-06]");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 0);
			        
		params = new HashMap<String, String>();
		params.put("f4", "[,2015-04-05]");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() > 0);
		 	        
		params = new HashMap<String, String>();
		params.put("f4", "[,]");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() > 0);
		
		// or 查询
		params = new HashMap<String, String>();
		params.put("f2|f9", "test");
		params.put("strictQuery", "true");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() == 0);
		
		params.put("strictQuery", "false");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() > 0);
		
		// 按ID列表查找 
		params = new HashMap<String, String>();
		params.put("id", "1,3,4,5,6");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() > 0);
		
		// test macro fields
		params = new HashMap<String, String>();
		params.put("fields", "macro_xx");
		try {
			result = _db.select(1, 10, params).result;
			Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue(e.getMessage().indexOf("Column \"MACRO_XX\" not found") >= 0 );
	    }
		
		SQLDef sd = new SQLDef();
		sd.setCode("macro_xx");
		sd.setScript("*");
		commonDao.create(sd); // SQLExcutor.queryL("select * from dm_sql_def")
		params.put("fields", "macro_xx");
		result = _db.select(1, 10, params).result;
		Assert.assertTrue(result.size() > 0);
		
		// test delete
		result = _db.select(1, 100, null).result;
		Assert.assertTrue(result.size() == 2);
		id = EasyUtils.obj2Long( result.get(0).get("id") );
		
		_db.logicDelete(null);
		_db.logicDelete(id);
		Assert.assertNotNull( _db.get(null) );
		Assert.assertNotNull( _db.get(id) );
		Assert.assertNotNull( _db.get(id).get("domain").toString().endsWith("@--") );
		
		_db.restore(null);
		_db.restore(id);
		Assert.assertNotNull( _db.get(id).get("domain").toString().indexOf("@--") < 0 );
		
		_db.delete(null);
		_db.delete(id);
		Assert.assertNull( _db.get(id) );
		Assert.assertTrue(_db.select(1, 100, null).result.size() == 1);
		
		// test insert and return ID
		Long rid = null;
		for(int i = 10; i < 19; i++) {
			valuesMap = new HashMap<String, String>();
			valuesMap.put("f1", i + ".0");
			valuesMap.put("f2", "just test " + i);
			valuesMap.put("f4", "2017-02-19");
			Long newId = _db.insertRID(valuesMap);
			Assert.assertNotNull( newId );
			
			if(rid != null) {
				Assert.assertEquals( ++rid, newId );
			}
			rid = newId;
		}
		
		// test 非空检查
		try {
			valuesMap = new HashMap<String, String>();
			valuesMap.put("f4", "2017-02-19");
			_db.insertRID(valuesMap);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("NULL not allowed for column f1", true);
        }
		
		// // test alter field Type error with row > 0 （有数据）
		tblDefine = "[ " +
						"{'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," + //  null --> not null
						"{'label':'名称', 'code':'f2', 'type':'date'}" +  // String --> Date
					"]";
		
		record = new Record();
		record.setId(5L);
		record.setDatasource(datasource);
		record.setTable("tbl_3_" + type.split(" ")[0]);
		record.setDefine(tblDefine);
		try {
			_db.alterTable(record);
        } catch (Exception e) {
        	if( type.equals("MySQL") ) {
        		Assert.assertTrue( e.getMessage().indexOf("alter table tbl_3_MySQL modify f2   date") >= 0);
        	}
        	if( type.equals("Oracle") ) {
        		Assert.assertTrue( e.getMessage().indexOf("alter table tbl_3_Oracle modify f2   date") >= 0);
        	}
        	if( type.equals("PostgreSQL") ) {
        		Assert.assertTrue( e.getMessage().indexOf("alter table tbl_3_PostgreSQL alter f2 TYPE    date") >= 0);
        	}
        }
		
		PageInfo pi = logService.getLogsByCondition(new LogQueryCondition());
		Assert.assertTrue( pi.getTotalRows() > 0);
	}
}
