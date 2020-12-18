/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.mock.dao._IUserDAO;
import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.mock.model._GroupRole;
import com.boubei.tss.framework.mock.model._GroupRoleId;
import com.boubei.tss.framework.mock.model._Role;
import com.boubei.tss.framework.mock.model._User;
import com.boubei.tss.framework.mock.service._IUMSerivce;
import com.boubei.tss.framework.persistence.Temp;
import com.boubei.tss.framework.sso.Environment;

public class UMServiceTest extends AbstractTest4F { 
    
    @Autowired _IUMSerivce umSerivce;
    
    @Autowired _IUserDAO userDao;
    
    @Test
    public void testGetEntitiesByHQL() {
        _Group group = new _Group();
        group.setCode("RD");
        group.setName("研发");
        umSerivce.createGroup(group);
        
        _User user = new _User();
        user.setGroup(group);
        user.setUserName("JohnXa");
        user.setPassword("123456");
        user.setAge(new Integer(25));
        user.setAddr("New York");
        user.setEmail("john@hotmail.com");
        umSerivce.createUser(user);
        
        String hql = "from _User t where t.userName in ( :names ) ";
        List<String> nameList = new ArrayList<String>();
        nameList.add("JohnXa");
        nameList.add("Jon");
        List<?> result = userDao.getEntities(hql, new String[]{"names"}, new Object[]{nameList});
        assertEquals(1, result.size());
        log.debug(result.get(0));
    }
    
    @Test
    public void testGetEntitiesByNativeSql() {
        _Group group = new _Group();
        group.setCode("RD");
        group.setName("研发");
        umSerivce.createGroup(group);
        
        _User user = new _User();
        user.setGroup(group);
        user.setUserName("JohnXa");
        user.setPassword("123456");
        user.setAge(new Integer(25));
        user.setAddr("New York");
        user.setEmail("john@hotmail.com");
        umSerivce.createUser(user);
        
    	String nativeSql = "select t.* from test_user t where t.userName = ? ";
		List<?> result = userDao.getEntitiesByNativeSql(nativeSql, _User.class, "JohnXa");
		assertEquals(1, result.size());
		log.debug(result.get(0));
    }
 
    @Test
    public void testUMCRUD() {
        log.info("test start......");
        
        _Group group = new _Group();
        group.setCode("RD");
        group.setName("研发");
        umSerivce.createGroup(group);
        
        _User user = new _User();
        user.setGroup(group);
        user.setUserName("JohnXa");
        user.setPassword("123456");
        user.setAge(new Integer(25));
        user.setAddr("New York");
        user.setEmail("john@hotmail.com");
        umSerivce.createUser(user);
        
        List<_User> result = umSerivce.queryAllUsers();
        assertEquals(1, result.size());
        
        _User userPo = result.get(0);
        log.info(userPo + userPo.getCreateTime().toString());
        
        userPo.setUserName("Jon.King");
        umSerivce.updateUser(userPo);
        
        log.info(userPo + userPo.getUpdateTime().toString());
        
        result = umSerivce.queryAllUsers();
        userPo = result.get(0);
        log.info(userPo);
        
        umSerivce.deleteUser(userPo);
        
        result = umSerivce.queryAllUsers();
        assertEquals(0, result.size());
        
        _Role role = new _Role();
        role.setCode("Engineer");
        role.setName("工程师");
        umSerivce.createRole(role);
        
        _GroupRole gr = new _GroupRole();
        gr.setId(new _GroupRoleId(group.getId(), role.getId()));
        umSerivce.createGroupRole(gr);
        
        assertEquals(1, umSerivce.queryGroupRole().size());
    }
    
    @Test
    public void testBaseDao() {
    	_Group group = new _Group();
        group.setCode("RD");
        group.setName("研发");
        umSerivce.createGroup(group);
        
        List<Temp> list = new ArrayList<Temp>();
    	Temp temp = new Temp(Environment.threadID(), group.getId());
    	temp.setUdf1(group.getCode());
    	temp.setUdf2(group.getCode());
    	temp.setUdf3(group.getName());
    	list.add(temp);
    	
    	userDao.insert2TempTable(list);
    	
    	List<?> list2 = userDao.getEntities("from Temp");
    	Assert.assertTrue(list2.size() == 1);
    	temp = (Temp) list2.get(0);
    	Assert.assertEquals(group.getId(), temp.getId());
    	Assert.assertEquals(group.getCode(), temp.getUdf1());
    	Assert.assertEquals(group.getCode(), temp.getUdf2());
    	Assert.assertEquals(group.getName(), temp.getUdf3());
    	Assert.assertTrue( temp.equals(temp) );
    	
    	userDao.executeSQL("update test_group set name = 'XXX' where code = ?", "RD");
    	userDao.executeSQL("update test_group set name = 'XXX' where code = :code", 
    			new String[] {"code"}, new Object[] {"RD"});
    	
    	userDao.executeHQL("delete _Group where code = ?1", "RD");
    	userDao.executeHQL("delete _Group where code = :code", new String[] {"code"}, new Object[] {"RD"});
    	
    	list.add(temp);
    	temp.setUdf8("888");
    	userDao.insert2TempNative(list);
    	List<Map<String, Object>> objs = SQLExcutor.queryL("select * from TBL_TEMP_ where thread=?", Environment.threadID());
		Assert.assertEquals(2, objs.size());
    }
}
