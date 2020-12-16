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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.H2DBServer;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.dm.DMConstants;

public class SQLExcutorH2Test {
	
	protected Logger log = Logger.getLogger(this.getClass());   
	
	private String sql1 = "select loginName, email from x_user t ";
	private String sql2 = "select loginName, email from (select * from x_user order by id) t where t.groupId = ?";
	
	@Before
	public void setUp() {
		initSourceTable();
	}
	
	@Test
	public void test0() {
		SQLExcutor ex = new SQLExcutor();
		Assert.assertTrue(ex.getGridTemplate().asXML().indexOf("没有查询到数据") > 0);
		ex.excuteQuery( " SELECT * FROM ( " + sql1 + " where 1=0 ) t", DMConstants.LOCAL_CONN_POOL);
		Assert.assertNull( ex.getFirstRow("email") );
		Assert.assertTrue( ex.toString().length() > 0 );
		
		ex.excuteQuery( sql1 + "\n LIMIT 0, 10", DMConstants.LOCAL_CONN_POOL);
		
		Map<Integer, Object> paramsMap = null;
		ex.excuteQuery(sql1, paramsMap, 0, 0, DMConstants.LOCAL_CONN_POOL);
		
		ex = new SQLExcutor();
		ex.excuteQuery(" SELECT * FROM ( " + sql1 + " order by loginName) t", paramsMap, 1, 1, DMConstants.LOCAL_CONN_POOL);
		
		ex = new SQLExcutor();
		ex.excuteQuery(sql1 + " order by loginName ", paramsMap, 1, 1, DMConstants.LOCAL_CONN_POOL);
		
    	try {
    		ex.excuteQuery( " SELECT * FROM X ", DMConstants.LOCAL_CONN_POOL);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("异常：Table X not found", true);
		}
    	
    	try {
			SQLExcutor.excuteBatch("truncate table x_user", null, new H2DBServer().getH2Connection());
		} catch (SQLException e) {
			Assert.fail();
		}
	}
	
	@Test
	public void test() {
		// test query
		SQLExcutor ex = new SQLExcutor();
		ex.excuteQuery(sql1, DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() > 0);
		Assert.assertNotNull(ex.getFirstRow("email"));
		
		Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
		paramsMap.put(1, 1L);
		ex.excuteQuery(sql2, paramsMap, DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() > 0);
		
		ex.excuteQuery(sql2, paramsMap, 1, 10, DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() > 0);
		
		Pool connpool = JCache.getInstance().getPool(DMConstants.LOCAL_CONN_POOL);
        Cacheable connItem = connpool.checkOut(0);
        Connection conn = (Connection) connItem.getValue();
        
		ex.excuteQuery(sql2, paramsMap, 1, 12, conn);
		Assert.assertTrue(ex.result.size() > 0);
		
		ex.excuteQuery(sql2, paramsMap, conn);
		Assert.assertTrue(ex.result.size() > 0);
		
		ex = new SQLExcutor();
		ex.excuteQuery(sql1, DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() > 0);
		Assert.assertNotNull(ex.getFirstRow("email"));
		
		// test excute single
		SQLExcutor.excute("insert into x_group values (10, 0, 10, 'T10', 'test')", DMConstants.LOCAL_CONN_POOL);
		ex.excuteQuery("select id, name, id as rn from x_group where id=10", DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() == 1);
		
		Map<Integer, Object> paramsMap2 = new HashMap<Integer, Object>();
		paramsMap2.put(1, 10);
		SQLExcutor.excute("delete from x_group where id=?", paramsMap2, DMConstants.LOCAL_CONN_POOL);
		ex.excuteQuery("select id, name from x_group where id=10", DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() == 0);
		
		// test excuteBatch
		String sql3 = "insert into x_group values (?, ?, ?, ?, ?)";
		List<Map<Integer, Object>> paramsList = new ArrayList<Map<Integer,Object>>();
		for(int i = 11; i < 20; i++) {
			Map<Integer, Object> map = new HashMap<Integer, Object>();
			map.put(1, i);
			map.put(2, 0);
			map.put(3, i);
			map.put(4, "T" + i);
			map.put(5, "test");
			
			paramsList.add(map);
		}
		SQLExcutor.excuteBatch(sql3, paramsList, DMConstants.LOCAL_CONN_POOL);
		
		ex = new SQLExcutor();
		ex.excuteQuery("select id, name from x_group order by id asc", DMConstants.LOCAL_CONN_POOL);
		Assert.assertTrue(ex.result.size() > 10);
		
		Assert.assertEquals("[3, 12]", ex.fieldTypes.toString());
		
		String sql = "select id, name from x_group where description = ?";
		List<Map<String, Object>> rerult = SQLExcutor.queryL(sql, "test");
		Assert.assertTrue(rerult.size() == 9);
		
		Assert.assertEquals( rerult.get(0).get("id"), SQLExcutor.queryVL(sql, "id", "test") );
		Assert.assertNull( SQLExcutor.queryVL(sql, "id", "test2sxxx") );
	}
	
	private void initSourceTable() {
		H2DBServer dbserver = new H2DBServer();
		Connection conn = dbserver.getH2Connection();
		
        try {
        	if( conn.isClosed() ) {
        		dbserver = new H2DBServer();
        		conn = dbserver.getH2Connection();
        	}
        	
        	if( conn.isClosed() ) {
        		Assert.fail("H2 connection is closed!");
        	}
        		
            Statement stat = conn.createStatement();
            stat.execute("create table if not exists x_group" + 
                    "(" + 
                        "id            NUMBER(19) not null, " + 
                        "parentId      NUMBER(19), " + 
                        "seqNo         NUMBER(10), " + 
                        "name          VARCHAR2(50 CHAR), " + 
                        "description   VARCHAR2(255 CHAR)" + 
                    ");" +
                    " alter table x_group add primary key (id); ");
            
            stat.execute("create table if not exists x_user" + 
                    "(" + 
                        "id           NUMBER(19) not null, " + 
                        "groupId      NUMBER(19) not null, " + 
                        "sex          NUMBER(1), " + 
                        "birthday     TIMESTAMP(6), " + 
                        "employeeNo   VARCHAR2(255 CHAR), " + 
                        "loginName    VARCHAR2(255 CHAR), " + 
                        "userName     VARCHAR2(255 CHAR), " + 
                        "password     VARCHAR2(255 CHAR), " + 
                        "email        VARCHAR2(255 CHAR) " + 
                    ");");
            
            PreparedStatement ps = conn.prepareStatement("insert into x_group values (?, ?, ?, ?, ?)");
            ps.setObject(1, 1L);
            ps.setObject(2, 0L);
            ps.setObject(3, 1);
            ps.setObject(4, "它山石");
            ps.setObject(5, "test test");
            ps.executeUpdate();
            
            ps.setObject(1, 2L);
            ps.setObject(2, 1L);
            ps.setObject(3, 1);
            ps.setObject(4, "子组1");
            ps.setObject(5, "test 1");
            ps.executeUpdate();
            
            ps = conn.prepareStatement("insert into x_user values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setObject(1, 1L);
            ps.setObject(2, 1L);
            ps.setObject(3, 1);
            ps.setObject(4, new Timestamp(new Date().getTime()));
            ps.setObject(5, "BL00618-001");
            ps.setObject(6, "JonKing-001");
            ps.setObject(7, "怒放的生命");
            ps.setObject(8, "123456");
            ps.setObject(9, "boubei@163.com");
            ps.executeUpdate();
            
        } catch (Exception e) {
            log.error(e);
        }  
    }
}
