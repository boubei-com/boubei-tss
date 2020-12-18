package com.boubei.tss.modules.cloud.pay;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.boubei.tss.PX;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.cloud.CloudService;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ProxyContract;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MailUtil;
import com.boubei.tss.util.MathUtil;

/**
 * @author hank
 * 产品抽象类，可被代理，直营的是admin代理
 */
public abstract class AbstractProduct {

    public static final String ADMIN_PAYER = "admin";

    protected static IUserDao dao = (IUserDao) Global.getBean("UserDao");
    protected IUserService userService = (IUserService) Global.getBean("UserService");
    protected CloudService cloudService = (CloudService) Global.getBean("CloudService");

    protected Logger log = Logger.getLogger(this.getClass());

    public CloudOrder co;
    public ModuleDef md;
    public Map<?, ?> trade_map;
    public String payType;
    public String payer;

    public User buyer;
    public User proxy = null;
    public ProxyContract contract;
    public boolean noProxy = true;
    public Map<String, Object> params = new HashMap<>();

    /**
     * out_trade_no: 时间戳_coID
     */
    public static AbstractProduct createBean(String out_trade_no) {
        Long coId = EasyUtils.obj2Long(out_trade_no.split("-")[1]);
        CloudOrder co = (CloudOrder) dao.getEntity(CloudOrder.class, coId);
        if (!out_trade_no.equals(co.getOrder_no())) {
            return null;
        }
        return createBean(co);
    }

    public static AbstractProduct createBean(CloudOrder co) {
        String productClazz = (String) EasyUtils.checkNull(co.getType(), ModuleOrderHandler.class.getName());
        return (AbstractProduct) BeanUtil.newInstanceByName(productClazz,
                new Class[]{CloudOrder.class}, new Object[]{co});
    }

    public AbstractProduct() {
    }

    public AbstractProduct(CloudOrder co) {
        this.co = co;

        try {
            this.params = EasyUtils.json2Map2(co.getParams());
        } catch (Exception e) {
        }

        this.buyer = dao.getUserByAccount((String) EasyUtils.checkNull(this.params.get("phone"), co.getCreator()), false);


        // 寻找代理，默认没有代理时假定代理人为Admin
        if (EasyUtils.isNullOrEmpty(co.getAppid())) {
            this.contract = new ProxyContract();
            this.contract.setPrice(0.0d);
            this.proxy = dao.getEntity(UMConstants.ADMIN_USER_ID);
        } else {
            String hql = "from ProxyContract where appid = ?1 and disabled = ?2 order by id desc";
            List<?> proxys = dao.getEntities(hql, co.getAppid(), ParamConstants.FALSE);
            this.noProxy = proxys.isEmpty();
            if (this.noProxy) {
                throw new BusinessException("应用" + co.getAppid() + "没有找到有效的代理合同！");
            }

            this.contract = (ProxyContract) proxys.get(0);
            this.proxy = dao.getUserByAccount(this.contract.getProxy_user(), false);
        }
    }

    // 产品模块检查
    protected abstract void beforeOrderModuleCheck();

    // 实现类自定义检查
    protected void beforeOrderCustomCheck() {
    }

    public void beforeOrder() {

        beforeOrderModuleCheck();

        beforeOrderCustomCheck();

        // 余额支付 校验账户余额是否足够，暂不计赠送金额
        if (CloudOrder.PAYTYPE_1.equals(co.getPay_type()) && co.getMoney_cal() > getBuyerAccount().getBalance()) {
            throw new BusinessException("您的账户余额不足，请用其它支付方式");
        }

        setPrice();

        if (getProxyAccount().getBalance() + getProxyPrice() < 0) {
            String title = "代理账户资金不足";
            sendMessage(proxy.getEmail(), title, "由于您的代理账户余额不足，" + co.getCreator() + "购买" + getName() + "失败！");
            throw new BusinessException(title + "，请先联系代理充值后再购买！");
        }
    }

    /**
     * 获取订单代理价（admin代理的，代理价为0元）
     */
    protected abstract Double getProxyPrice();

    public abstract void setPrice();

    /**
     * 购买付款成功后，进行初始化
     */
    public void afterPay(Map<?, ?> trade_map, Double real_money, String payer, String payType, String payClazz) {
        // 只有待付款状态的订单可以执行afterPay
        if (!CloudOrder.NEW.equals(co.getStatus())) {
            throw new BusinessException("订单" + co.getStatus());
        }

        this.trade_map = trade_map;
        this.payType = payType;
        this.payer = payer;

        co.setMoney_real(real_money);
        co.setPay_date(new Date());
        co.setPay_type(payType);
        co.setPay_clazz(payClazz);

        if (real_money < co.getMoney_cal() && !ADMIN_PAYER.equals(payer)) {
            co.setRemark("订单金额不符");
            co.setStatus(CloudOrder.PART_PAYED); // 部分付款
        } else {
            co.setStatus(CloudOrder.PAYED);

            handle();
            proxyHandle();
            init();

            sendMessage(null,
                    "用户付款通知",
                    "用户：" + ((User) EasyUtils.checkNull(buyer, new User())).getUserName()
                    + "\n支付：" + co.getMoney_real() + "元"
                    + "\n支付方式：" + co.getPay_type()
                    + "\n产品：" + co.getProduct()
                    + "\n账号：" + co.getCreator()
                    + "\n参数：" + EasyUtils.obj2String(co.getParams()));

            sendSMS2Buyer();

        }

        dao.update(co);
    }

    /**
     * 退款
     *
     * @param refund_fee    退款金额
     * @param out_refund_no 退款单号
     * @return
     */
    public Result refund(Double refund_fee, String out_refund_no) {
        if (EasyUtils.isNullOrEmpty(co.getPay_clazz())) {
            return new Result(false, "未知的支付类型");
        }
        IRefund iRefund = (IRefund) BeanUtil.newInstanceByName(co.getPay_clazz());
        return iRefund.refund(co, refund_fee, out_refund_no);
    }

    protected abstract void handle();

    protected void init() {
    }

    public abstract String getName();

    protected String toflowType() {
        return AccountFlow.TYPE0;
    }

    protected String toflowRemark() {
        return null;
    }

    protected Account getBuyerAccount() {
        return getAccount(buyer.getId());
    }

    protected Account getProxyAccount() {
        return getAccount(proxy.getId());
    }

    protected Account getAccount(Long userId) {
        List<?> accounts = getAccountWithNoAutoCreate(userId);
        if (accounts.size() > 0) {
            return (Account) accounts.get(0);
        }

        Account account = new Account();
        account.setBalance(0D);
        account.setBalance_freeze(0D);
        account.setBelong_user(userService.getUserById(userId));
        account = (Account) dao.createObject(account);

        return account;
    }

    protected List<?> getAccountWithNoAutoCreate(Long userId) {
        return dao.getEntities(" from Account where belong_user.id = ?1", userId);
    }

    // 普通购买，同时创建一条充值流水和扣款流水
    public void createFlows(Account account) {
        // 不是余额支付的，认为是先进行了充值行为，再购买
        String pay_type = co.getPay_type();
        if (!CloudOrder.PAYTYPE_1.equals(pay_type)) {
            createIncomeFlow(account);
        }
        createBuyFlow(account);
    }

    // 创建充值流水
    protected void createIncomeFlow(Account account) {
        AccountFlow flow = new AccountFlow(account, this, AccountFlow.TYPE1, null);
        createFlow(account, flow, co.getMoney_real());
    }

    // 创建扣款流水
    protected void createBuyFlow(Account account) {
        AccountFlow flow = new AccountFlow(account, this, this.toflowType(), this.toflowRemark());
        createFlow(account, flow, -co.getMoney_real());
    }

    protected void createFlow(Account account, AccountFlow flow, Double deltaMoney) {
        flow.setMoney(deltaMoney);

        Double balance = MathUtil.addDoubles(account.getBalance(), deltaMoney);
        flow.setBalance(balance);
        account.setBalance(balance);

        dao.createObject(flow);
    }

    protected void createSubAuthorize(boolean isNewDomain) {

        // 检查是否已经有这个策略
        String hql = " from SubAuthorize where buyerId = ?1 and moduleId = ?2";
        boolean isFirstTimeBuyThisModule = dao.getEntities(hql, this.buyer.getId(), this.md.getId()).isEmpty();

        if (co.singleSubAuthorize && !isFirstTimeBuyThisModule) {
            return;
        }

        boolean autoAssign = isNewDomain || isFirstTimeBuyThisModule;

        int account_num = co.getAccount_num();
        int mouth_num = co.getMonth_num();
        Long buyerId = buyer.getId();
        for (int i = 0; i < account_num; i++) {
            // 如果是新开域（或类EFF购买），则把第一个策略的角色都给购买人
            autoAssign = autoAssign && i == 0;

            // 模块ID_模块名称_购买人_购买序号
            String saName = md.getId() + "_" + md.getModule() + "_" + buyerId + "_" + (i + 1);

            SubAuthorize sa = new SubAuthorize();
            sa.setName(saName);
            sa.setModuleId(md.getId());
            sa.setOwnerId(buyerId);
            sa.setBuyerId(buyerId);
            sa.setStartDate(DateUtil.today());

            Double try_days = EasyUtils.obj2Double(EasyUtils.checkNull(md.getTry_days(), 0));
            Date endDate = DateUtil.addMonths(mouth_num);
            endDate = DateUtil.addDays(endDate, try_days); // 加上试用天数
            sa.setEndDate(DateUtil.noHMS(endDate));

            dao.createObject(sa);

            // 创建策略角色对应关系
            List<Long> roleIds = md.resourcelist("roles");
            for (Long roleId : roleIds) {
                RoleUser ru = new RoleUser();
                ru.setModuleId(md.getId());
                ru.setRoleId(roleId);
                ru.setStrategyId(sa.getId());
                ru.setUserId((Long) EasyUtils.checkTrue(autoAssign, buyerId, null));

                dao.createObject(ru);
            }
        }
    }

    // 代理账户扣款
    protected void proxyHandle() {
        Account account = getProxyAccount();
        Double money = getProxyPrice();
        Double balance = MathUtil.addDoubles(account.getBalance(), money);

        AccountFlow flow = new AccountFlow(account, this, AccountFlow.TYPE2, null);
        flow.setMoney(money);
        flow.setPay_man(proxy.getLoginName());
        flow.setPayment(AccountFlow.PAYMENT1);
        flow.setBalance(balance);
        dao.createObject(flow);

        account.setBalance(balance);
        dao.update(account);

        // 通知代理用户成功付款，资金余额

        sendMessage(proxy.getEmail(), "用户购买成功",
                co.getCreator() + "购买" + co.getProduct() + "成功，"
                        + "\n" + EasyUtils.checkTrue(money < 0, "扣除", "返还") + flow.getType() + Math.abs(money) + "元。"
                        + "\n您的账户当前余额：" + balance + "元。"
                        + "\n请保持足够的账户余额，以免用户购买失败！");

    }

    /* 发送通知 */
    protected void sendMessage(String mailList, final String title, final String content) {
        if (!EasyUtils.isProd()) return;

        String mails = ParamConfig.getAttribute(PX.NOTIFY_AFTER_PAY_LIST, "boubei@163.com");
        if (!EasyUtils.isNullOrEmpty(mailList)) {
            mails += "," + mailList;
        }
        final String[] receivers = mails.split(",");

        // 改用异步发送
        new Thread() {
            public void run() {
                MailUtil.send(title, content, receivers, MailUtil.DEFAULT_MS);
            }
        }.start();
    }

    protected void sendSMS2Buyer() {

    }
}
