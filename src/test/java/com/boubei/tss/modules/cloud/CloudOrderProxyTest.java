/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud;

import java.util.Date;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.PX;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.exception.ExceptionEncoder;
import com.boubei.tss.framework.sms.SMSLog;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ProxyContract;
import com.boubei.tss.modules.cloud.entity.ProxyPrice;
import com.boubei.tss.modules.cloud.pay.ModuleOrderHandler;
import com.boubei.tss.modules.cloud.pay.RechargeOrderHandler;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.UserAction;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.um.service.ISubAuthorizeService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;

import org.junit.Assert;

public class CloudOrderProxyTest extends AbstractTest4DM {
	
	@Autowired IGroupService groupService;
	@Autowired CloudService cloudService;
	
	@Autowired ModuleAction mAction;
	@Autowired CloudAction oAction;
	@Autowired AccountAction accountAction;
	@Autowired UserAction userAction;
	@Autowired ISubAuthorizeService saService;
	
	ModuleDef module1, module2;
	
	protected void init() {
		super.init();
		super.initDomain();
		
		Config.setProperty(PX.ENVIRONMENT, "dev");

		module1 = new ModuleDef();
		module1.setId(null);
		module1.setKind("OA");
		module1.setModule("日常管理");
		module1.setCode("MD-001");
		module1.setRoles(role0.getId() + "," + role3.getId());
		module1.setReports("1,2,3");
		module1.setRecords("4,5,6");
		module1.setStatus("opened");
		module1.setPrice(0D);
		module1.setPrice_def("0");
		module1.setTry_days(31);
		module1.setAccount_limit("1,50");
		module1.setProduct_class( ModuleOrderHandler.class.getName() );
		commonDao.createObject(module1);
		Long moduleId = module1.getId();
		Assert.assertEquals(module1.getPK(), moduleId);
		BeanUtil.getProperties(module1);
		
		module2 = new ModuleDef();
		module2.setKind("物流");
		module2.setModule("录单跟单");
		module2.setCode("MD-002");
		module2.setRoles(role0.getId() + "," + role1.getId());
		module2.setReports("11,12,13");
		module2.setRecords("14,15,16");
		module2.setStatus("opened");
		module2.setPrice(300D);
		module2.setPrice_def("${account_num}*${month_num}*${price}");
		module2.setTry_days(30);
		module2.setAccount_limit("1,99");
		module2.setMonth_limit("1,36");
		module2.setCashback_ratio(10D);
		commonDao.createObject(module2);
	}
	
	private List<?> listOrders() {
		return (List<?>) oAction.listOrders(null, null);
	}

	@Test
	public void testCloudOrder() {
		
		String appid = "appid_123456";
		String buyer = "13688886666";
		login(domainUser);
		
		Assert.assertEquals(1, oAction.listSaleableModules().size());
		Assert.assertTrue( listOrders().isEmpty() );
		Account proxyAccount = accountAction.queryAccount();
		Assert.assertTrue(proxyAccount.getId() < 0);
		
		// 签订代理合同，代理价=50
		ProxyContract pc = new ProxyContract();
		pc.setId(null);
		pc.setAppid(appid);
		pc.setCode("NO.001");
		pc.setDeposit(10000D);
		pc.setModule(module2);
		pc.setPrice(50D);
		pc.setProxy_user(domainUser.getLoginName());
		
		pc.setStartDate(DateUtil.today());
		pc.setEndDate(DateUtil.addDays(365));
		pc.setDisabled(ParamConstants.FALSE);
		commonDao.createObject(pc);
		log.debug( EasyUtils.obj2Json(pc) );
		
		// 代理充值=200
		CloudOrder co = new CloudOrder();
		co.setMoney_cal(200D);
		co.setType( RechargeOrderHandler.class.getName() );
		setParameters( co );
		co = oAction.createOrder(request);
		Assert.assertEquals( 1, listOrders().size() );

		login( UMConstants.ADMIN_USER );
		oAction.payedOrders(co.getOrder_no(), null); // Admin pay
		login( domainUser );
		proxyAccount = accountAction.queryAccount();
		Assert.assertEquals(new Double(200), proxyAccount.getBalance());
		
		// 代理为买家设置销售价=90
		ProxyPrice pp = new ProxyPrice();
		pp.setId(null);
		pp.setBuyer(buyer);
		pp.setModule(module2);
		pp.setPrice(90D);
		pp.setUnit_price(0.9D);
		commonDao.createObject(pp);
		log.debug( EasyUtils.obj2Json(pp) );
		
		// 下单，匿名购买
		logout();
		CloudOrder co1 = new CloudOrder();
		co1.setModule_id(module2.getId());
		co1.setAccount_num(1);
		co1.setMonth_num(12);
		co1.setAppid( pc.getAppid() );
		
		// 发送短信验证码，并填写注册信息
		SMSLog log = new SMSLog();
    	log.setPhonenum(buyer);
    	log.setRandomnum( MathUtil.randomInt(899999) + 100000 );
    	log.setSendDay( DateUtil.format(new Date()) );
    	commonDao.create(log);
    	String smsCode = log.getRandomnum().toString();
		
		JSONObject json = new JSONObject();
		json.put("phone", buyer);
		json.put("user_name", "曹俊");
		json.put("company_name", "山西运城");
		json.put("password", buyer);
		json.put("smsCode", smsCode);
		co1.setParams( json.toString() );
		setParameters( co1 );
		
		// 代理余额不足 1*12*50 = 600
		try {
			co1 = oAction.createOrder(request);
			Assert.fail();
		} catch( Exception e) {
			Assert.assertEquals("代理账户资金不足，请先联系代理充值后再购买！", e.getMessage());
		}
		
		// 买家余额支付，余额不足
		try {
//			co1.setMoney_balance(100D);
			co1.setPay_type(CloudOrder.PAYTYPE_1);
			co1.setMoney_cal( 100D );
			setParameters( co1 );
			co1 = oAction.createOrder(request);
			Assert.fail();
		} catch( Exception e) {
			Assert.assertEquals("您的账户余额不足，请用其它支付方式", e.getMessage());
		}
		
		// 正常购买
		co1.setMonth_num(1);
//		co1.setMoney_balance(0D);
		co1.setPay_type( null );
		setParameters( co1 );
		co1 = oAction.createOrder(request);
		
		Assert.assertEquals( 1, listOrders().size() );
		Assert.assertEquals(new Double(90), co1.getMoney_cal());
		
		login( UMConstants.ADMIN_USER );
		oAction.payedOrders(co1.getOrder_no(), null); // Admin pay，应扣除代理商余额50元
		login( buyer );
		
		// 买家充值 99
		CloudOrder co2 = new CloudOrder();
		co2.setMoney_cal(99D);
		co2.setType( RechargeOrderHandler.class.getName() );
		co2.setAppid( pc.getAppid() );
		setParameters( co2 );
		co2 = oAction.createOrder(request);

		login( UMConstants.ADMIN_USER );
		oAction.payedOrders(co2.getOrder_no(), null); // Admin pay，应扣除代理商余额99元
		login( buyer );
		Account buyerAccount = accountAction.queryAccount();
		Assert.assertEquals(new Double(99), buyerAccount.getBalance());
		
		// TODO 买家续费：余额 + 微信
		
		// 检查代理的账户资金是否准确：200 - 99（充值）- 50（代理扣费）
		login(domainUser);
		proxyAccount = accountAction.queryAccount();
		Assert.assertEquals(new Double(51), proxyAccount.getBalance());
		
		Assert.assertEquals(1, oAction.listMyCustomer(1, 10, appid).get("total"));
		
		// 换一个人购买，代理未设置价格，无法下单
		logout();
		String buyer2 = "13344445555";
		CloudOrder co3 = new CloudOrder();
		co3.setModule_id(module2.getId());
		co3.setAccount_num(1);
		co3.setMonth_num(12);
		co3.setAppid( pc.getAppid() );
		
    	log.setPhonenum(buyer2);
    	commonDao.update(log);
		json = new JSONObject();
		json.put("phone", buyer2);
		json.put("user_name", "曹俊2");
		json.put("company_name", "山西运城2");
		json.put("password", buyer2);
		json.put("smsCode", smsCode);
		
		try {
			co3.setParams( json.toString() );
			setParameters( co3 );
			oAction.createOrder(request);
			Assert.fail();
		} catch( Exception e) {
			Assert.assertEquals("代理商未设置价格，无法下单！", e.getMessage());
		}
		
		// appid没有签订代理合同
		try {
			co3.setAppid("xxxxx");
			setParameters( co3 );
			oAction.createOrder(request);
			Assert.fail();
		} catch( Exception e) {
			Assert.assertEquals("应用xxxxx没有找到有效的代理合同！", ExceptionEncoder.getFirstCause(e).getMessage());
		}
		
		// 异常流，co.module_id = null
		try {
			co3.setAppid(pc.getAppid());
			co3.setModule_id(null);
			co3.setParams( json.toString() );
			setParameters( co3 );
			oAction.createOrder(request);
			Assert.fail();
		} catch( Exception e) {
			Assert.assertTrue( "实例化失败", true );
		}
		
		Config.setProperty(PX.ENVIRONMENT, "test");
	}
}
