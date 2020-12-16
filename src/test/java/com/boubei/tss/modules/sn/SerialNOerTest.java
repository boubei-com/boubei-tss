package com.boubei.tss.modules.sn;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

public class SerialNOerTest extends AbstractTest4DM {
	
	@Autowired SerialNOer serialNOer;
	
	@Test
	public void test() {
		
		Assert.assertEquals("BD00001", serialNOer.create("BDxxxx", 1, 7).get(0));
		
		String precode = "SO";
		String sn = precode + DateUtil.format(DateUtil.today(), "yyyyMMdd").substring(2);
		Assert.assertEquals(sn + "001", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "002", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "003", SerialNOer.create(precode, 1).get(0) );
		
		System.out.println(SerialNOer.create(precode, 10));
		System.out.println(SerialNOer.create(precode, 10));
		
		List<String> list = SerialNOer.create(precode, 10000);
		Assert.assertEquals(sn + "10023", list.get(list.size()-1));
		
		precode = "TO";
		sn = precode + DateUtil.format(DateUtil.today(), "yyyyMMdd").substring(2);
		Assert.assertEquals(sn + "001", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "002", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "003", SerialNOer.createOne(precode) );
		
		Assert.assertEquals(sn + "004", SerialNOer.get(precode) );
		
		precode = "";
		sn = precode + DateUtil.format(DateUtil.today(), "yyyyMMdd").substring(2);
		Assert.assertEquals(sn + "001", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "002", SerialNOer.create(precode, 1).get(0) );
		
		Assert.assertEquals(sn + "003", SerialNOer.createOne(precode) );
		Assert.assertEquals(sn + "004", SerialNOer.get() );
		
		SerialNO sno = new SerialNO();
		sno.setId(-1L);
		sno.getDay();
		sno.getDomain();
		sno.getId();
		sno.getLastNum();
		sno.getPK();
		sno.getPrecode();
		
		new SNCreator(precode, "AA", 0).toString();
	}
	
	@Test
	public void test2() {
		super.initDomain();
		login(domainUser);
		SQLExcutor.excute("update x_domain set prefix = 'BJ' where domain = 'BD'", DMConstants.LOCAL_CONN_POOL);
		
		String domainPrefix = "BJ";
		
		String precode = "SOxxxx";
		String sn = domainPrefix + "SO";
		Assert.assertEquals(sn + "001", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "002", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "003", SerialNOer.create(precode, 1).get(0) );
		
		System.out.println(SerialNOer.create(precode, 10));
		System.out.println(SerialNOer.create(precode, 10));
		
		List<String> list = SerialNOer.create(precode, 10000);
		Assert.assertEquals(sn + "10023", list.get(list.size()-1));
		
		precode = "TOxxxx";
		sn = domainPrefix + "TO";
		Assert.assertEquals(sn + "001", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "002", SerialNOer.create(precode, 1).get(0) );
		
		Assert.assertEquals(sn + "003", SerialNOer.createOne(precode) );
		
		precode = "xxxx";
		sn = domainPrefix;
		Assert.assertEquals(sn + "001", SerialNOer.create(precode, 1).get(0) );
		Assert.assertEquals(sn + "002", SerialNOer.create(precode, 1).get(0) );
		
		Assert.assertEquals(sn + "003", SerialNOer.createOne(precode) );
		
		// 全局流水号
		precode = "JKxxxx";
		Assert.assertEquals("JK001", SerialNOer.get(precode, true) );
		Assert.assertEquals("JK002", SerialNOer.get(precode, true) );
		
		Assert.assertEquals("BJJK001", SerialNOer.get(precode, false) );
		Assert.assertEquals("BJJK002", SerialNOer.get(precode, false) );
	}
	
	@Test
	public void testSelfNO() {
		super.initDomain();
		login(super.ceo);
		
		String domainPrefix = "JK";
		SQLExcutor.excute("update x_domain set prefix = 'JK' where domain = 'BD'", DMConstants.LOCAL_CONN_POOL);
		
		SelfNO snObj = new SelfNO();
		snObj.setId(null);
		snObj.setCode("1111111111");
		snObj.setTag(Environment.getUserCode());
		snObj.setUdf("test");
		commonDao.create(snObj);
		log.debug(EasyUtils.obj2Json(snObj));
		
		Assert.assertEquals(snObj.getCode(), SerialNOer.get() );
		
		String precode = "SOxxxx";
		String sn = domainPrefix + "SO";
		Assert.assertEquals(sn + "001", SerialNOer.create(precode, 1).get(0) );
	}
	
	@Test
	public void testFixLengthSN() {
		super.initDomain();
		login(super.ceo);
		
		Assert.assertEquals("10000001", SerialNOer.getFixSN(8, "1", true));
		
		Assert.assertEquals("20000001", SerialNOer.getFixSN(8, "2", false));
		
		Assert.assertEquals("100000001", SerialNOer.getFixSN(9, "", false));
		Assert.assertEquals("100000002", SerialNOer.getFixSN(9, "", false));
		Assert.assertEquals("100000003", SerialNOer.getFixSN(9, "", false));
		
		String day = DateUtil.format(new Date(), "yyyyMMdd").substring(2);
		Assert.assertEquals("M" +day+ "001", SerialNOer.getFixSN(10, "MyyMMddxxxx", false));
		Assert.assertEquals("M" +day+ "002", SerialNOer.getFixSN(11, "MyyMMddxxxx", false));
		Assert.assertEquals("M" +day+ "003", SerialNOer.getFixSN(9, "MyyMMddxxxx", false));
		
		Assert.assertEquals("110000001", serialNOer.getFixSN("11", 1, 9, false).get(0));
	}
}
