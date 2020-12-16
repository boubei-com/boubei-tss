/* ==================================================================
 * Created [2016-06-22] by Jon.King
 * ==================================================================
 * TSS
 * ==================================================================
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018
 * ==================================================================
 */

package com.boubei.tss.modules.cloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.pay.AbstractProduct;
import com.boubei.tss.modules.cloud.pay.AfterPayService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

@Controller
@RequestMapping("/cloud")
public class CloudAction {

    @Autowired
    private CloudService cloudService;
    @Autowired
    private ICommonService commonService;
    @Autowired
    private CloudDao cloudDao;

    @RequestMapping(value = "/order", method = RequestMethod.POST)
    @ResponseBody
    public CloudOrder createOrder(HttpServletRequest request) {
        Map<String, String> map = DMUtil.getRequestMap(request, false);
        CloudOrder co = new CloudOrder();
        BeanUtil.setDataToBean(co, map);

        return cloudService.createOrder(co);
    }

    @RequestMapping(value = "/order", method = RequestMethod.PUT)
    @ResponseBody
    public CloudOrder updateOrder(CloudOrder co) {
        AbstractProduct.createBean(co).setPrice(); // 重新计算价格
        commonService.update(co);

        return co;
    }

    @RequestMapping(value = "/order/price/query")
    @ResponseBody
    public CloudOrder queryPrice(CloudOrder co) {
        co.setCreator((String) EasyUtils.checkNull(co.getCreator(), Environment.getUserCode(), "ANONYMOUS"));
        if (co.getType() != null) {
            AbstractProduct.createBean(co).setPrice();
        }

        return co;
    }

    @RequestMapping(value = "/order/price", method = RequestMethod.POST)
    @ResponseBody
    public void updatePrice(Long id, Double rebate, Double derate) {
        // 只有超级管理员有权限，才有权限对订单价格进行减免或给与折扣
        if (!Environment.isAdmin())
            return;

        CloudOrder mo = (CloudOrder) commonService.getEntity(CloudOrder.class, id);
        mo.setRebate(rebate);
        mo.setDerate(derate);
        mo.setMoney_cal(mo.getMoney_cal() * rebate - derate);

        commonService.update(mo);
    }

    @RequestMapping(value = "/order/list", method = RequestMethod.GET)
    @ResponseBody
    public Object listOrders(Integer page, Integer rows) {
        String hql = " from CloudOrder where creator = '" + Environment.getUserCode() + "' order by id desc";
        if (Environment.isAdmin()) {
            hql = " from CloudOrder order by id desc";
        }
        if (page == null && rows == null) {
            return cloudDao.getEntities(hql);
        } else {
            return cloudDao.getPaginationEntities(hql, page, rows);
        }
    }

    @RequestMapping(value = "/proxy/authorize", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> listMyCustomer(int page, int rows, String appid) {
        String sql = "select distinct uu.loginname, uu.username, usa.name, usa.startdate, usa.enddate, usa.disabled, cmo.appid "
                + "from cloud_proxy_contract cpc,cloud_module_order cmo,um_user uu,um_sub_authorize usa "
                + " where cpc.appid = ? "
                + "	  and cpc.disabled = ? and cpc.proxy_user = ? and cpc.module_id = cmo.module_id and cpc.appid = cmo.appid "
                + "   and cmo.status = ? and cmo.creator = uu.loginname and uu.id = usa.ownerid";

        Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
        paramsMap.put(1, appid);
        paramsMap.put(2, ParamConstants.FALSE);
        paramsMap.put(3, Environment.getUserCode());
        paramsMap.put(4, CloudOrder.PAYED);

        SQLExcutor ex = new SQLExcutor();
        ex.excuteQuery(sql, paramsMap, page, rows, DMConstants.LOCAL_CONN_POOL);

        return ex.toEasyUIGrid();
    }

    @RequestMapping(value = "/modules", method = RequestMethod.GET)
    @ResponseBody
    public List<?> listSaleableModules() {
        String hql = "from ModuleDef o where o.status in ('opened') and price > 0 order by o.seqno asc, o.id desc ";
        return commonService.getList(hql);
    }

    @RequestMapping(value = "/order/pay/{order_no}", method = RequestMethod.POST)
    @ResponseBody
    public void payedByAccountBalance(@PathVariable String order_no) {
        List<?> accounts = commonService.getList("from Account where belong_user.id = ?", Environment.getUserId());
        if (accounts.isEmpty()) {
            throw new BusinessException(EX.CO_2);
        }
        Account account = (Account) accounts.get(0);

        List<?> cloudOrders = commonService.getList("from CloudOrder where order_no = ?", order_no);
        if (cloudOrders.isEmpty()) {
            throw new BusinessException(EX.CO_3);
        }
        CloudOrder co = (CloudOrder) cloudOrders.get(0);

        if (CloudOrder.PAYED.equals(co.getStatus())) {
            throw new BusinessException(EX.parse(EX.CO_4, co.getStatus()));
        }

        if (account.getBalance() - co.getMoney_cal() < account.getBalance_safe()) {
            throw new BusinessException(EX.CO_5);
        }

        AfterPayService afterPayService = (AfterPayService) cloudService;
        afterPayService.handle(order_no, co.getMoney_cal(), account.getBelong_user().getLoginName(), CloudOrder.PAYTYPE_1, null, null);
    }

    /**
     * Admin 设置订单状态为已支付
     */
    @RequestMapping(value = "/order/payed/{order_no}", method = RequestMethod.POST)
    @ResponseBody
    public void payedOrders(@PathVariable String order_no, Double money_real) {
        if (!Environment.isAdmin())
            return;

        AfterPayService afterPayService = (AfterPayService) cloudService;
        List<?> list = commonService.getList(" from CloudOrder where order_no = ?", order_no);
        CloudOrder co = (CloudOrder) list.get(0);

        money_real = (Double) EasyUtils.checkNull(money_real, co.getMoney_cal());
        co.setMoney_real(money_real);

        afterPayService.handle(order_no, money_real, AbstractProduct.ADMIN_PAYER, "线下", null, null);
    }

    /**
     * 非购买，管理员自动创建策略分配给新开域（TMS里当前域为下游承运商开域）
     */
    @RequestMapping(value = "/order/autoPayed", method = RequestMethod.POST)
    @ResponseBody
    public void fastCreateModuleUser(String user_name, String phone, Long moduleId, Long logistics_id) {
        cloudService.fastCreateModuleUser(user_name, phone, moduleId, logistics_id);
    }
}
