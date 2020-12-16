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
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;

import junit.framework.Assert;

/**
 * 往临时表插入数据后，直接用原生SQL无法读取到数据，要先刷新下hibernate的缓存
 *
 */
public class TempTableTest extends AbstractTest4F {
	
	@Autowired ICommonDao commonDao;
	
	@Test
	public void test() {
		List<Long> list = new ArrayList<Long>();
		list.add(12L);
		list.add(13L);
		list.add(15L);
		commonDao.insertIds2TempTable(list);
		
		List<?> rt = commonDao.getEntitiesByNativeSql("select count(*) from TBL_TEMP_");
		System.out.println(rt.get(0));
		
		rt = commonDao.getEntities("select count(*) from Temp");  // 刷新hibernate缓存？
		System.out.println(rt.get(0));
		
		rt = commonDao.getEntitiesByNativeSql("select count(*) from TBL_TEMP_");
		System.out.println(rt.get(0));
		
		List<?> temps = commonDao.getEntities("from Temp where thread=?", Environment.threadID());
		Assert.assertEquals(3, temps.size());
		Temp t1 = (Temp) temps.get(0);
		Assert.assertEquals(t1.getThread().longValue(), Environment.threadID());
		
		Temp t2 = new Temp();
		t2.setPK(null);
		t2.getPK();
		
		t2.setUdf1("1");
		t2.setUdf2("2");
		t2.setUdf3("3");
		t2.setUdf4("4");
		t2.setUdf5("5");
		t2.setUdf6("6");
		t2.setUdf7("7");
		t2.setUdf8("8");
		log.debug( EasyUtils.obj2Json(t2) );
	}

}
