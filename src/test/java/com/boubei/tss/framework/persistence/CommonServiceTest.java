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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.mock.model._User;

import junit.framework.Assert;

public class CommonServiceTest extends AbstractTest4F { 
    
    @Autowired ICommonService commonService;
    @Autowired ICommonDao commonDao;

    @Test
    public void test() {
    	_Group group = new _Group();
        group.setCode("RD");
        group.setName("研发");
        commonService.create(group);
        
        _User user = new _User();
        user.setGroup(group);
        user.setUserName("JonKing");
        user.setPassword("123456");
        user.setAge(new Integer(25));
        user.setAddr("New York");
        user.setEmail("john@hotmail.com");
        commonService.createWithLog(user);
        
        user.setEmail("xxx@163.com");
        commonService.updateWithLog(user);
        commonService.update(user);
        
        Assert.assertNotNull(commonService.getEntity(_User.class, user.getId()));
 
    	String hql = " from _User t where t.userName = ? ";
		List<?> result = commonService.getList(hql, "JonKing");
		Assert.assertEquals(1, result.size());
		try {
			result = commonService.getList(hql, new Object[] {null});
		} catch(Exception e) {
			log.error(e.getMessage());
		}
		
		hql = " from _User t where t.userName = :userName ";
		result = commonService.getList(hql, new String[] {"userName"}, new Object[] { "JonKing" });
		Assert.assertEquals(1, result.size());
		try {
			result = commonService.getList(hql, new String[] {"userName"}, new Object[] {null});
		} catch(Exception e) {
			log.error(e.getMessage());
		}
		
		hql = " from _User t where t.userName in (:userName) ";
		result = commonService.getList(hql, new String[] {"userName"}, new Object[] { "JonKing,Jane".split(",") });
		Assert.assertEquals(1, result.size());
		
		log.debug(result);
		
		commonDao.updateWithLog(user);
		
		commonService.deleteWithLog(_User.class, user.getId());
		
		_Group group2 = new _Group();
        group.setCode("G1");
        group.setName("G1");
        List<_Group> list = new ArrayList<>();
        list.add(group2);
        commonService.createBatch(list);
        commonService.updateBatch(list);
        commonService.deleteBatch(_Group.class, Arrays.asList(group2.getId()));
    }
}
