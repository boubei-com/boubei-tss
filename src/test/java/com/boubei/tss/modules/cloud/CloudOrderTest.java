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

import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sms.AbstractSMS;
import com.boubei.tss.framework.sms.SMSLog;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.pay.AbstractProduct;
import com.boubei.tss.modules.cloud.pay.ModuleOrderHandler;
import com.boubei.tss.modules.cloud.pay.RechargeOrderHandler;
import com.boubei.tss.modules.cloud.pay.RenewalfeeOrderHandler;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.UserAction;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.permission.GroupPermission;
import com.boubei.tss.um.entity.permission.GroupResource;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.um.service.ISubAuthorizeService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;

import junit.framework.Assert;

public class CloudOrderTest extends AbstractTest4DM {

    @Autowired
    IGroupService groupService;
    @Autowired
    CloudService cloudService;
    @Autowired
    ICommonService commonService;

    @Autowired
    ModuleAction mAction;
    @Autowired
    CloudAction oAction;
    @Autowired
    AccountAction accountAction;
    @Autowired
    UserAction userAction;
    @Autowired
    ISubAuthorizeService saService;

    ModuleDef module1, module2;

    protected void init() {
        super.init();
        for (int i = 0; i < 20; i++) { // 消耗ID，使得不和其它地方重复
            createGroup("GG" + i, UMConstants.ASSISTANT_GROUP_ID);
        }
        super.initDomain();

        Group mg = super.createGroup("XX企业组3", UMConstants.DOMAIN_ROOT_ID);

        module1 = new ModuleDef();
        module1.setId(null);
        module1.setModule_group(mg.getId());
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
        module1.setProduct_class(ModuleOrderHandler.class.getName());
        commonDao.createObject(module1);
        Long moduleId = module1.getId();
        Assert.assertEquals(module1.getPK(), moduleId);
        BeanUtil.getProperties(module1);

        module2 = new ModuleDef();
        module2.setModule_group(mg.getId());
        module2.setKind("物流");
        module2.setModule("录单跟单");
        module2.setCode("MD-002");
        module2.setRoles(role0.getId() + "," + role1.getId());
        module2.setRoles_free(role4.getId().toString());
        module2.setInner_base_role(role4.getId() + "," + role4.getId());
        module2.setReports("11,12,13");
        module2.setRecords("14,15,16");
        module2.setStatus("opened");
        module2.setPrice(300D);
        module2.setPrice_def("${account_num}*${month_num}*300");
        module2.setTry_days(30);
        module2.setAccount_limit("1,99");
        module2.setMonth_limit("1,36");
        module2.setCashback_ratio(10D);
        commonDao.createObject(module2);
    }

    @Test
    public void testUserOnline() {

        login(domainUser);

        mAction.selectModule(module1.getId());
        List<?> list = mAction.listSelectedModules();
        Assert.assertEquals(1, list.size());

        // price1 = 0
        Assert.assertEquals(2, groupService.findEditableRoles().size());

        // 付费的需要域管理员通过使用转授策略把购买的模块角色赋予域用户
        module1.setPrice(120D);
        commonDao.update(module1);
        Assert.assertEquals(1, groupService.findEditableRoles().size()); // role0 freeUse

        // test 账号多地登录控制
        String sessionId = "1234567890abc";
        HttpSession session = new MockHttpSession();
        Context.sessionMap.put(sessionId, session);

        IOnlineUserManager ouManager = (IOnlineUserManager) Global.getBean("DBOnlineUserService");
        Long userId = domainUser.getId();

        String token = TokenUtil.createToken(sessionId, userId);
        ouManager.register(token, "TSS", sessionId, userId, domainUser.getUserName());
        ouManager.register(token, "TSS", sessionId, userId, domainUser.getUserName());
        Assert.assertEquals(1, commonDao.getEntities("from DBOnlineUser where sessionId = ?", sessionId).size()); // domainUser

        // 允许多地登录 multiLogin
        domainInfo.setMultilogin(ParamConstants.TRUE);
        commonDao.update(domainInfo);
        request.getSession().setAttribute("domain_multilogin", ParamConstants.TRUE);
        ouManager.register(token, "TSS", sessionId, staff1.getId(), staff1.getUserName());
        Assert.assertEquals(2, commonDao.getEntities("from DBOnlineUser where sessionId = ?", sessionId).size()); // domainUser + staff1

        // mock WX reg
        request.addHeader("USER-AGENT", "xxx micromessenger/6.7.2 nettype/4g language/zh_cn");
        ouManager.register(token, "TSS", sessionId, userId, domainUser.getUserName());
        Assert.assertEquals(3, commonDao.getEntities("from DBOnlineUser where sessionId = ?", sessionId).size()); // domainUser(pc + wx) + staff1

        // mock api call (不踢人)
        request = new MockHttpServletRequest();
        request.addParameter("uName", domainUser.getLoginName());
        request.addParameter("uToken", domainUser.getPassword());
        request.addHeader("USER-AGENT", "API CALL");
        Context.initRequestContext(request);

        session = new MockHttpSession();
        Context.sessionMap.put(sessionId, session);

        ouManager.register(token, "TSS", sessionId, userId, domainUser.getUserName());
        Assert.assertEquals(4, commonDao.getEntities("from DBOnlineUser where sessionId = ?", sessionId).size()); // domainUser(pc*2 + wx) + staff1
    }

    private List<?> listOrders() {
        return (List<?>) oAction.listOrders(null, null);
    }

    @Test
    public void testCloudOrder() {

        login(domainUser);

        Assert.assertEquals(1, oAction.listSaleableModules().size());
        Assert.assertTrue(listOrders().isEmpty());

        oAction.queryPrice(new CloudOrder());

        // 下单1，已注册的域管理员购买
        CloudOrder co1 = new CloudOrder();
        co1.setId(null);
        co1.setModule_id(module1.getId());
        co1.setAccount_num(6);
        co1.setMonth_num(24);
        co1.setType(module1.getProduct_class());
        co1.setMchid(null);

        co1 = oAction.queryPrice(co1);
        Assert.assertNotNull(co1.getMoney_cal());

        co1.setRebate(0.9D);
        co1.setDerate(100D);
        co1 = oAction.queryPrice(co1);
        Assert.assertEquals(-100D, co1.getMoney_cal());

        setParameters(co1);
        oAction.createOrder(request);
        Assert.assertEquals(1, listOrders().size());
        co1 = (CloudOrder) listOrders().get(0);
        Assert.assertNull(AbstractProduct.createBean("123456-" + co1.getId()));

        request.removeParameter("account_num");
        request.addParameter("account_num", "101");
        try {
            oAction.createOrder(request);
            Assert.fail();
        } catch (Exception e) {
        }

        // 下单2，匿名购买
        logout();
        CloudOrder co2 = new CloudOrder();
        co2.setModule_id(module2.getId());
        co2.setAccount_num(3);
        co2.setMonth_num(12);
        co2.setInvite_user_id(domainUser.getId());

        // 发送短信验证码，并填写注册信息
        String phone = "13688889999";
        Object smsCode;
        try {
            smsCode = AbstractSMS.create().sendRandomNum(phone);
        } catch (Exception e) {
            SMSLog log = new SMSLog();
            log.setPhonenum(phone);
            log.setRandomnum(MathUtil.randomInt(899999) + 100000);
            log.setSendDay(DateUtil.format(new Date()));
            commonDao.create(log);
            smsCode = log.getRandomnum();
        }

        JSONObject json = new JSONObject();
        json.put("smsCode", smsCode.toString());
        json.put("phone", phone);
        json.put("user_name", "王海成");
        json.put("company_name", "链家物流");
        json.put("password", phone);
        co2.setParams(json.toString());

        setParameters(co2);
        co2 = oAction.createOrder(request);
        Assert.assertEquals(1, listOrders().size());

        Assert.assertEquals(10800D, co2.getMoney_cal());

        co2.setAccount_num(2);
        oAction.updateOrder(co2);

        Double money_cal = co2.getMoney_cal();
        oAction.updatePrice(co2.getId(), 0.9, 100d);
        co2 = (CloudOrder) commonDao.getEntity(CloudOrder.class, co2.getId());
        Assert.assertNull(co2.getMoney_real()); // 价格不变，只有Admin能改价格

        Account account = accountAction.queryAccount();
        Assert.assertTrue(account.getBalance() == 0D);

        // pay，只有Admin能操作直接支付，其它需要购买支付
        Long buyerId = Environment.getUserId();
        oAction.payedOrders(co2.getOrder_no(), co2.getMoney_cal()); // 非Admin支付

        Assert.assertNull(accountAction.checkSubAuthorizeExpire()[1]);
        accountAction.setSubAuthorizeExpire(-1L, "2018-01-01", DateUtil.format(new Date()));

        Assert.assertEquals(0, groupService.findEditableRoles().size()); // 未支付
        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co2.getOrder_no(), co2.getMoney_cal());
        commonDao.executeHQL("update RoleUser set userId = ? where moduleId = ?", buyerId, module2.getId());

        // test 转授的角色前台禁止选择
        Long groupId = (Long) commonDao.getEntities("select groupId from GroupUser where userId = ?", buyerId).get(0);
        List<?> roles = commonDao.getEntities("select roleId from RoleUser where userId = ? and roleId < 0", buyerId);

        // for maven test 下多线程方式跑Test --- start
        Assert.assertEquals(UMConstants.ADMIN_USER_ID, Environment.getUserId());
        List<String> permissions = permissionHelper.getOperationsByResource(groupId, GroupPermission.class.getName(), GroupResource.class);
//		Assert.assertEquals("[1, 2, p_1, p_2]", permissions.toString());
        if (!permissions.contains("1")) {
            Group _group = groupService.getGroupById(groupId);
            permissionHelper.createPermission(UMConstants.ADMIN_ROLE_ID, _group, UMConstants.GROUP_VIEW_OPERRATION, 1, 1, 1, GroupPermission.class.getName());
            permissionHelper.createPermission(UMConstants.ADMIN_ROLE_ID, _group, UMConstants.GROUP_EDIT_OPERRATION, 1, 1, 1, GroupPermission.class.getName());
        }
        // for maven test 下多线程方式跑Test --- END

        userAction.getUserInfoAndRelation(response, buyerId, groupId);

        // test 既有转授角色，又有直接授予的角色
//		userService.createOrUpdateUser(userService.getUserById(buyerId), groupId.toString(), roles);
        for (Object roleId : roles) {
            RoleUser ru = new RoleUser();
            ru.setUserId(buyerId);
            ru.setRoleId((Long) roleId);
            commonDao.createObject(ru);
        }
        userAction.getUserInfoAndRelation(response, buyerId, groupId);

        try {
            oAction.payedOrders(co2.getOrder_no(), co2.getMoney_cal());
            Assert.fail();
        } catch (Exception e) {
            // 订单已支付
        }
        Assert.assertTrue(listOrders().size() > 0);
        login(buyerId, co2.getCreator());

        // check
        Assert.assertEquals(1, groupService.findEditableRoles().size()); // module2.free_roles = -10000
        Assert.assertEquals(1, mAction.listSelectedModules().size());

        Assert.assertEquals(1, listOrders().size());

        // test account action
        account = accountAction.queryAccount();
        Assert.assertNotNull(account);
        accountAction.queryAccountFlow(request, 1, 100);
        List<?> saList = accountAction.querySubAuthorize();
        Assert.assertEquals(co2.getAccount_num().intValue(), saList.size());

        SubAuthorize sa = (SubAuthorize) saList.get(0);
        List<?> list2 = accountAction.querySubAuthorizeRoles(sa.getId());
        Assert.assertEquals(2, list2.size());
        RoleUser ru1 = (RoleUser) ((Object[]) list2.get(0))[0];
        RoleUser ru2 = (RoleUser) ((Object[]) list2.get(1))[0];
        accountAction.setSubAuthorizeRoles(Environment.getUserId(), ru1.getId() + "," + ru2.getId(), sa.getId());

        RoleUser ru = new RoleUser();
        ru.setModuleId(ru1.getModuleId());
        ru.setStrategyId(sa.getId());
        ru.setRoleId(ru1.getRoleId());
        commonDao.createObject(ru);
        accountAction.setSubAuthorizeRoles(Environment.getUserId(), "-1," + ru2.getId(), sa.getId());

        // test RenewalfeeOrder (续费)
        CloudOrder co3 = new CloudOrder();
        co3.setMonth_num(12);
        co3.setParams(sa.getId() + "");
        co3.setType(RenewalfeeOrderHandler.class.getName());
        co3.setMoney_cal(100d);
        co3.setModule_id(module2.getId());

        setParameters(co3);
        co3 = oAction.createOrder(request);

        // pay
        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co3.getOrder_no(), co3.getMoney_cal());

        login(phone);
        Assert.assertNull(accountAction.checkSubAuthorizeExpire()[1]);

        login(UMConstants.ADMIN_USER);
        sa = (SubAuthorize) saList.get(1);
        accountAction.setSubAuthorizeExpire(sa.getId(), "2018-01-01", DateUtil.format(new Date()));

        login(phone);
        // for maven test 下多线程方式跑Test --- start
        Group _group = groupService.getGroupById(Environment.getUserGroupId());
        permissionHelper.createPermission(UMConstants.DOMAIN_ROLE_ID, _group, UMConstants.GROUP_VIEW_OPERRATION, 1, 0, 0, GroupPermission.class.getName());
        permissionHelper.createPermission(UMConstants.DOMAIN_ROLE_ID, _group, UMConstants.GROUP_EDIT_OPERRATION, 1, 0, 0, GroupPermission.class.getName());
        // for maven test 下多线程方式跑Test --- END
        userAction.getUserInfoAndRelation(response, buyerId, Environment.getUserGroupId());
        Assert.assertEquals(1L, accountAction.checkSubAuthorizeExpire()[0]);

        logout();
        login(UMConstants.ADMIN_USER);

        oAction.updatePrice(co2.getId(), 0.9, 100d);
        co2 = (CloudOrder) commonDao.getEntity(CloudOrder.class, co2.getId());
        Assert.assertEquals(money_cal * 0.9 - 100, co2.getMoney_cal()); // 价格变了，Admin能改价格

        module2.setRoles(module2.getRoles() + ",999");
        commonDao.update(module2);
        mAction.refreshModuleUserRoles(module2.getId());
    }

    @Test
    public void testFix() {
        Account account = new Account();
        account.setBalance_freeze(0D);
        account.setBalance_safe(0D);
        account.setPoints(10000);
        account.setPoints_freeze(0);
        account.setRemark(null);
        account.setDomain(null);
        BeanUtil.setDataToBean(account, BeanUtil.getProperties(account));
        EasyUtils.obj2Json(account);

        AccountFlow flow = new AccountFlow();
        flow.setId(null);
        flow.setFk(null);
        BeanUtil.setDataToBean(flow, BeanUtil.getProperties(flow));
        EasyUtils.obj2Json(flow);
    }

    @Test
    public void testCloudOrder2() {
        logout();
        CloudOrder co = new CloudOrder();
        co.setModule_id(module2.getId());
        co.setAccount_num(3);
        co.setMonth_num(12);

        // 发送短信验证码，并填写注册信息
        String phone = "13688889999";
        JSONObject json = new JSONObject();
        json.put("smsCode", "123456");
        json.put("phone", phone);
        json.put("user_name", "王海成");
        json.put("company_name", "链家物流");
        json.put("password", phone);
        co.setParams(json.toString());
        setParameters(co);

        try {
            co = oAction.createOrder(request);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().indexOf("短信验证码校验失败") >= 0);
        }

        SMSLog log = new SMSLog();
        log.setPhonenum(phone);
        log.setRandomnum(123456);
        log.setSendDay(DateUtil.format(new Date()));
        commonDao.create(log);
        co = oAction.createOrder(request);

        AbstractProduct.createBean(co.getOrder_no()).afterPay(null, 100D, "JK", "线下", null);
        co = (CloudOrder) commonDao.getEntity(CloudOrder.class, co.getId());
        Assert.assertEquals(CloudOrder.PART_PAYED, co.getStatus());

        // 再下3单
        co = oAction.createOrder(request);

        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co.getOrder_no(), null);
        co = (CloudOrder) commonDao.getEntity(CloudOrder.class, co.getId());
        Assert.assertEquals(CloudOrder.PAYED, co.getStatus());

        logout();
        login(phone);

        co = oAction.createOrder(request);

        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co.getOrder_no(), null);
        co = (CloudOrder) commonDao.getEntity(CloudOrder.class, co.getId());
        Assert.assertEquals(CloudOrder.PAYED, co.getStatus());

        logout();
        co = oAction.createOrder(request);

        List<?> saList = accountAction.querySubAuthorize();
        SubAuthorize sa1 = (SubAuthorize) saList.get(0);
        SubAuthorize sa2 = (SubAuthorize) saList.get(1);
        sa2.setName(module1.getId() + "_xxxx");
        sa2.setModuleId(module1.getId());
        commonDao.update(sa2);

        CloudOrder ro1 = new CloudOrder();
        ro1.setModule_id(module2.getId());
        ro1.setMonth_num(12);
        ro1.setParams(sa1.getId() + "," + sa2.getId());
        ro1.setType(RenewalfeeOrderHandler.class.getName());
        setParameters(ro1);
        try {
            oAction.createOrder(request);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().indexOf("不能同时续费多个产品") >= 0);
        }

        // 换个手机号下单
        logout();

        String phone2 = "13688884444";
        co = new CloudOrder();
        co.setModule_id(module2.getId());
        co.setAccount_num(1);
        co.setMonth_num(12);

        // 发送短信验证码，并填写注册信息
        json.put("phone", phone2);
        json.put("user_name", "王世通");
        json.put("company_name", "世通一达");
        co.setParams(json.toString());
        setParameters(co);

        log = new SMSLog();
        log.setPhonenum(phone2);
        log.setRandomnum(123456);
        log.setSendDay(DateUtil.format(new Date()));
        commonDao.create(log);

        co = oAction.createOrder(request);

        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co.getOrder_no(), null);
        login(phone2);

        // test RenewalfeeOrder (续费)
        CloudOrder ro2 = new CloudOrder();
        ro2.setModule_id(module2.getId());
        ro2.setMonth_num(12);
        ro2.setParams(sa1.getId() + "");
        ro2.setType(RenewalfeeOrderHandler.class.getName());
        setParameters(ro2);
        try {
            oAction.createOrder(request);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().indexOf("不能续费其它用户购买的账号") >= 0);
        }
    }

    @Test
    public void testRechargeOrder() {

        login(domainUser);

        CloudOrder co = new CloudOrder();
        co.setMoney_cal(100D);
        co.setType(RechargeOrderHandler.class.getName());

        setParameters(co);
        co = oAction.createOrder(request);
        Assert.assertEquals(1, listOrders().size());

        oAction.listOrders(1, 10);

        // pay
        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co.getOrder_no(), null);
        login(domainUser);

        Account account = accountAction.queryAccount();
        Assert.assertNotNull(account);
        Assert.assertEquals(co.getMoney_cal(), account.getBalance());

        List<?> flows = (List<?>) accountAction.queryAccountFlow(request, null, null);
        Assert.assertEquals(1, flows.size());
        AccountFlow af = (AccountFlow) flows.get(0);
        Assert.assertEquals(co.getMoney_cal(), af.getMoney());
        Assert.assertEquals(co.getMoney_cal(), af.getBalance());
    }

    @Test
    public void testRechargeOrderByUnDomainUser() {
        login(domainUser);
        Account domainUserAccount = accountAction.queryAccount();
        Double balacne = domainUserAccount.getBalance();

        login(staff0);

        double reChargeMoney = 100D;
        CloudOrder co = new CloudOrder();
        co.setMoney_cal(reChargeMoney);
        co.setType(RechargeOrderHandler.class.getName());
        co.setParams("{\"targetUserCode\":\"" + domainUser.getLoginName() + 1 + "\"}");
        try {
			setParameters(co);
            co = oAction.createOrder(request);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(RechargeOrderHandler.ERROR1, e.getMessage());
        }

        co.setParams("{\"targetUserCode\":\"" + domainUser.getLoginName() + "\"}");
		setParameters(co);
        co = oAction.createOrder(request);

        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co.getOrder_no(), null);

        login(domainUser);
        domainUserAccount = accountAction.queryAccount();
        Assert.assertEquals(balacne + reChargeMoney, domainUserAccount.getBalance());
    }

    @Test
    public void flowZero() {
        Account account = accountAction.queryAccount();
        AccountFlow zeroFlow = new AccountFlow();
        zeroFlow.setAccount_id(account.getId());
        zeroFlow.setMoney(0.0);
        commonService.create(zeroFlow);

        AccountFlow flow = new AccountFlow();
        flow.setAccount_id(account.getId());
        flow.setMoney(1.0);
        commonService.create(flow);

        List<?> flows = (List<?>) accountAction.queryAccountFlow(request, null, null);
        Assert.assertEquals(2, flows.size());

        request.setParameter("ignoreZero", "true");
        flows = (List<?>) accountAction.queryAccountFlow(request, null, null);
        Assert.assertEquals(1, flows.size());
    }

    @Test
    public void testSelectAndBuy() {

        login(domainUser);

        Long userId = domainUser.getId();
        Long moduleId = module2.getId();
        mAction.selectModule(moduleId);

        cloudService.unSelectModule(userId, moduleId);
        Assert.assertEquals(0, commonDao.getEntities("from ModuleUser where userId=? and moduleId=?", userId, moduleId).size());
        cloudService.unSelectModule(userId, moduleId);

        mAction.selectModule(moduleId);

        CloudOrder co1 = new CloudOrder();
        co1.setId(null);
        co1.setModule_id(moduleId);
        co1.setAccount_num(6);
        co1.setMonth_num(12);
        setParameters(co1);
        co1 = oAction.createOrder(request);

        login(UMConstants.ADMIN_USER);
        oAction.payedOrders(co1.getOrder_no(), null);
        login(domainUser);

        String hql = "from SubAuthorize where name like ?";

        List<?> list = commonDao.getEntities(hql, moduleId + "\\_%\\_" + userId + "_try");
        Assert.assertEquals(1, list.size());

        list = commonDao.getEntities(hql, moduleId + "\\_%\\_" + userId + "_1");
        Assert.assertEquals(1, list.size());
        SubAuthorize sa = (SubAuthorize) list.get(0);

        saService.saveSubauth(sa, domainUser.getId() + ",-1000", "", role0.getId() + "," + role1.getId());

        cloudService.unSelectModule(userId, moduleId);
        Assert.assertEquals(1, commonDao.getEntities("from ModuleUser where userId=? and moduleId=?", userId, moduleId).size()); // 剩下购买的策略
        cloudService.unSelectModule(userId, moduleId);
    }

    @Test
    public void testastCreateModuleUserF() {
        module2.setMonth_limit("1,600");
        oAction.fastCreateModuleUser("铭聚", "13588833888", module2.getId(), 0L);
        oAction.fastCreateModuleUser("铭聚", "13588833888", module2.getId(), 0L);
    }

    @Test
    public void testOther() {
    	 CloudOrder co = new CloudOrder();
         co.setMoney_cal(100D);
         co.setType(RechargeOrderHandler.class.getName());
         co.setPay_type( CloudOrder.PAYTYPE_1 );
         
         ModuleOrderHandler p = new ModuleOrderHandler();
         p.co = co;
         try {
        	 p.createFlows(new Account());
         } catch(Exception e) {}
    }
}
