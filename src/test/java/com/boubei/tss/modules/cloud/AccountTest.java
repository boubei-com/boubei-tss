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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.pay.IRefund;
import com.boubei.tss.modules.cloud.pay.Result;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.EasyUtils;

import org.junit.Assert;

public class AccountTest extends AbstractTest4DM {
    @Autowired
    CloudAction cloudAction;
    @Autowired
    CloudService cloudService;

    protected void init() {
        super.init();
        for (int i = 0; i < 20; i++) { // 消耗ID，使得不和其它地方重复
            createGroup("GG" + i, UMConstants.ASSISTANT_GROUP_ID);
        }
        super.initDomain();
    }

    @Test
    public void test() {
        Account account = new Account();
        account.setBalance(100D);
        account.deduct(90D);
        Assert.assertEquals(new Double(10), account.getBalance());

        account = new Account();
        account.setBalance(100D);
        account.setBalance_freeze(70D);
        account.deduct(90D);
        Assert.assertEquals(new Double(70), account.getBalance_freeze());
        Assert.assertEquals(new Double(10), account.getBalance());

        account = new Account();
        account.setBalance(80D);
        account.setBalance_freeze(100D);
        account.deduct(90D);
        Assert.assertEquals(new Double(90), account.getBalance_freeze());
        Assert.assertEquals(new Double(0D), account.getBalance());

        account.addBalanceFreeze(1.1D);
        account.add(2.2D);
    }

    @Test
    public void testPayedByAccountBalance() {
        login(domainUser);
        Account account = new Account();
        account.setBelong_user(domainUser);
        account.setBalance(100D);
        account.setBalance_freeze(50D);
        commonDao.create(account);

        CloudOrder cloudOrder = new CloudOrder();
        cloudOrder.setOrder_no(System.currentTimeMillis() + "");
        cloudOrder.setType(ProductTest.class.getName());
        cloudOrder.setMoney_cal(80D);
        cloudService.createOrder(cloudOrder);

        cloudAction.payedByAccountBalance(cloudOrder.getOrder_no());
        Assert.assertEquals(new Double(20.0), account.getBalance());


        // 设置安全额度10
        account.setBalance_safe(10D);
        CloudOrder cloudOrder2 = new CloudOrder();
        cloudOrder2.setOrder_no(System.currentTimeMillis() + "");
        cloudOrder2.setType(ProductTest.class.getName());
        cloudOrder2.setMoney_cal(11D);
        cloudService.createOrder(cloudOrder2);

        try {
            cloudAction.payedByAccountBalance(cloudOrder2.getOrder_no());
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(EX.CO_5, e.getMessage());
        }
        
        try {
            cloudAction.payedByAccountBalance("asas12123");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(EX.CO_3, e.getMessage());
        }
        
        cloudOrder2.setStatus(CloudOrder.PAYED);
        try {
            cloudAction.payedByAccountBalance(cloudOrder2.getOrder_no());
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(EX.parse(EX.CO_4, CloudOrder.PAYED), e.getMessage());
        }
        
        cloudOrder2.setStatus(CloudOrder.NEW);
        Result r = cloudService.refund(cloudOrder2.getOrder_no(), cloudOrder.getMoney_real(), "退款单_001", null, true);
        Assert.assertEquals(EX.parse(EX.CO_1, CloudOrder.PAYED), r.getErrorMsg());

        // 发起退款
        cloudService.refund(cloudOrder.getOrder_no(), cloudOrder.getMoney_real(), "退款单_002", null, true);
        Assert.assertEquals(new Double(100.0), account.getBalance());
        // 再次退款
        cloudOrder2.setStatus(CloudOrder.PART_PAYED);
        cloudService.refund(cloudOrder.getOrder_no(), cloudOrder.getMoney_real(), "退款单_002", null, true);
        
        login(staff1);
        try {
            cloudAction.payedByAccountBalance(cloudOrder2.getOrder_no());
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals(EX.CO_2, e.getMessage());
        }
    }
    
    @Test
    public void testPayedAndRefund() {
        login(domainUser);
        Account account = new Account();
        account.setBelong_user(domainUser);
        account.setBalance(100D);
        account.setBalance_freeze(50D);
        commonDao.create(account);

        CloudOrder cloudOrder = new CloudOrder();
        cloudOrder.setOrder_no("CO20201009001");
        cloudOrder.setType(ProductTest.class.getName());
        cloudOrder.setMoney_cal(80D);
        cloudService.createOrder(cloudOrder);

        login("Admin");
        cloudAction.payedOrders(cloudOrder.getOrder_no(), 80D); // 线下支付
 
        // 发起退款
        login(domainUser);
        cloudService.refund(cloudOrder.getOrder_no(), cloudOrder.getMoney_real(), "退款单_001", null, false);
        
        cloudOrder.setPay_clazz(Refund1.class.getName());
        Result r = cloudService.refund(cloudOrder.getOrder_no(), cloudOrder.getMoney_real(), "退款单_002", null, false);
        EasyUtils.obj2Json(r);
    }
    
    public static class Refund1 implements IRefund {
		public Result refund(CloudOrder co, Double refund_fee, String out_refund_no) {
			return new Result(true, "test refund");
		}
    }
}
