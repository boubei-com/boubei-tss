/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.mock.dao._IGroupDAO;
import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.mock.model._User;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.sn.SelfNO;
import com.boubei.tss.modules.sn.SerialNO;

import junit.framework.Assert;

public class BaseDaoTest extends AbstractTest4F { 
    
    @Autowired _IGroupDAO dao;
    @Autowired ICommonDao commonDao;
    
    @Before
    public void setUp() throws Exception {
    	super.setUp();
    	
    	_Group group = new _Group();
        group.setCode("RD2");
        group.setName("研发2");
        dao.createObject(group);
    	
    	_User user = new _User();
        user.setGroup(group);
        user.setUserName("JohnXa");
        user.setPassword("123456");
        user.setAge(new Integer(25));
        user.setAddr("New York");
        user.setEmail("john@hotmail.com");
        dao.createObject(user);
    }

    @Test
    public void testGetEntitiesByNativeSql() {
    	String nativeSql = "select t.* from test_user t where t.userName = ? ";
		List<?> result = dao.getEntitiesByNativeSql(nativeSql, _User.class, "JohnXa");
    	
		try {
			result = dao.getEntitiesByNativeSql(nativeSql, new Object[] {null});
		} catch(Exception e) {
			log.error(e.getMessage());
		}
		
		try {
			result = dao.getEntitiesByNativeSql(nativeSql, _User.class, new Object[] {null});
		} catch(Exception e) {
			log.error(e.getMessage());
		}
		
		nativeSql = "select t.* from test_user t where t.id > :id and t.userName in (:userName) ";
		result = dao.getEntitiesByNativeSql(nativeSql, new String[] {"id", "userName"}, new Object[] { 0L, "JohnXa,Jane".split(",") });
		Assert.assertEquals(1, result.size());
    }
    
    @Test
    public void testGetEntities() {
    	String hql = " from _User t where t.userName = ? ";
		List<?> result = dao.getEntities(hql, "JohnXa");
		try {
			result = dao.getEntities(hql, new Object[] {null});
		} catch(Exception e) {
			log.error(e.getMessage());
		}
		
		hql = " from _User t where t.userName = :userName ";
		result = dao.getEntities(hql, new String[] {"userName"}, new Object[] { "JohnXa" });
		try {
			result = dao.getEntities(hql, new String[] {"userName"}, new Object[] {null});
		} catch(Exception e) {
			log.error(e.getMessage());
		}
		
		hql = " from _User t where t.userName in (:userName) ";
		result = dao.getEntities(hql, new String[] {"userName"}, new Object[] { "JohnXa,Jane".split(",") });
		
		log.debug(result);
    }
    
    @Test
    public void testTempTable() {
    	dao.insert2TempTable(null);
    	dao.insertEntityIds2TempTable(null);
    	dao.insertIds2TempTable(null);
    	dao.insertIds2TempTable(null, 0);
    }
    
    @Test 
    public void testSaveRecordObj() {
    	SelfNO snObj = new SelfNO();
		snObj.setId(null);
		snObj.setCode("1111111111");
		snObj.setTag(Environment.getUserCode());
		snObj.setUdf("test");
		
		dao.createRecordObject(snObj);
		dao.updateRecordObject(snObj);
		
		SerialNO sn = new SerialNO();
		sn.setPrecode("xxx");
		sn.setDay(new Date());
		sn.setLastNum(1);
		commonDao.createWithLog(sn);
    }
}
