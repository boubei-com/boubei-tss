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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sms.AbstractSMS;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.modules.cloud.pay.AbstractProduct;
import com.boubei.tss.modules.cloud.pay.AfterPayService;
import com.boubei.tss.modules.cloud.pay.ModuleOrderHandler;
import com.boubei.tss.modules.cloud.pay.Result;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

@SuppressWarnings("unchecked")
@Service("CloudService")
public class CloudServiceImpl implements CloudService, AfterPayService {

    @Autowired
    ICommonDao commonDao;
    @Autowired
    CloudDao cloudDao;
    @Autowired
    IUserService userService;
    @Autowired
    APIService apiService;

    protected Logger log = Logger.getLogger(this.getClass());

    /************************************* cloud module **************************************/

    public void selectModule(Long user, Long module) {
        checkIsDomainAdmin();
        ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, module);

        // 创建 ModuleUser映射关系
        ModuleUser mu = new ModuleUser(user, module);
        mu.setIsBuy(ParamConstants.FALSE);
        mu.setDomain(Environment.getDomainOrign());

        commonDao.create(mu);

        // 生成一个转授策略
        SubAuthorize sa = new SubAuthorize();
        sa.setName(def.getId() + "_" + def.getModule() + "_" + user + "_try"); /* name:模块ID_模块名称_购买人_购买序号 */
        sa.setModuleId(def.getId());
        sa.setStartDate(DateUtil.today());
        sa.setOwnerId(Environment.getUserId());
        sa.setBuyerId(Environment.getUserId());

        Object try_days = EasyUtils.checkNull(def.getTry_days(), 31);
        sa.setEndDate(DateUtil.addDays(EasyUtils.obj2Int(try_days)));
        commonDao.create(sa);

        // 设置转授权限给当前域管理员
        List<Long> roles = def.resourcelist("roles");
        for (Long roleId : roles) {
            RoleUser ru = new RoleUser();
            ru.setRoleId(roleId);
            ru.setUserId(user);
            ru.setStrategyId(sa.getId());
            ru.setModuleId(module);
            commonDao.create(ru);
        }

        // TODO 执行模块自定义类的init方法，对模块进行初始化
    }

    /**
     * 检查当前用户是否为域管理员，只有域管理员可以选择或取消功能模块
     */
    private void checkIsDomainAdmin() {
        if (!Environment.isDomainAdmin()) {
            throw new BusinessException(EX.MODULE_1);
        }
    }

    /**
     * 域用户选择模块后，获得了模块所含的角色；当模块新添加了角色后，自动刷给域用户。
     * 避免域用户需要重新选择模块才能获取新角色（先【结束试用】，再【我要试用】）
     */
    public void refreshModuleUserRoles(Long moduleID) {

        ModuleDef def = (ModuleDef) commonDao.getEntity(ModuleDef.class, moduleID);
        List<Long> roles = def.resourcelist("roles");

        // 模块角色减少时，本方法只能去掉域管理员的角色；域管理员也已经把角色授给了其它域成员的话，则无法收回
        List<RoleUser> ruList = (List<RoleUser>) commonDao.getEntities("from RoleUser where moduleId = ?", moduleID);
        for (Iterator<RoleUser> it = ruList.iterator(); it.hasNext(); ) {
            RoleUser ru = it.next();
            if (!roles.contains(ru.getRoleId())) {
                commonDao.delete(ru);
                it.remove();
            }
        }

        // 模块已被多少域用户购买使用
        List<Long> userIds = (List<Long>) commonDao.getEntities("select userId from ModuleUser where moduleId = ?", moduleID);
        String hql3 = "select id from SubAuthorize where moduleId = ? and buyerId = ?";
        for (Long domainUserId : userIds) {
            // 当前域用户已经获得的模块策略
            List<Long> strategyIds = (List<Long>) commonDao.getEntities(hql3, moduleID, domainUserId);
            for (Long strategyId : strategyIds) {
                for (Long roleId : roles) {
                    RoleUser ru = new RoleUser();
                    ru.setRoleId(roleId);
                    ru.setUserId(domainUserId);
                    ru.setModuleId(moduleID);
                    ru.setStrategyId(strategyId);

                    if (!ruList.contains(ru)) {
                        commonDao.create(ru);
                        ruList.add(ru);
                    }
                }
            }
        }
    }

    public void unSelectModule(Long user, Long module) {
        checkIsDomainAdmin();

        // 不能删除购买获得的策略（按照策略的命名判断，"_try"结尾）
        String hql = "from SubAuthorize where name like ?";
        List<?> list = commonDao.getEntities(hql, module + "\\_%\\_" + user + "_try");
        if (list.size() > 0) {
            SubAuthorize sa = (SubAuthorize) list.get(0);
            commonDao.deleteAll(commonDao.getEntities("from RoleUser where strategyId=?", sa.getId()));
            commonDao.delete(sa);

            // 删除选择试用产生的ModuleUser
            commonDao.deleteAll(commonDao.getEntities("from ModuleUser where userId=? and moduleId=? and isBuy=0", user, module));
        }
    }

    public List<?> listSelectedModules(Long user) {
        String hql = "select o from ModuleDef o, ModuleUser mu " + " where mu.moduleId = o.id and mu.userId = ? and o.status in ('opened', 'closed')"
                + " order by o.id desc ";
        return commonDao.getEntities(hql, user);
    }

    public List<?> listAvaliableModules() {
        String hql = "from ModuleDef o where o.status in ('opened') order by o.seqno asc, o.id desc ";
        return commonDao.getEntities(hql);
    }

    public Set<Long> limitReports() {
        return limitResources("ReportPermission", "reports", "rp_", Report.OPERATION_VIEW);
    }

    public Set<Long> limitRecords() {
        return limitResources("RecordPermission", "records", "rc_", Record.OPERATION_VDATA, Record.OPERATION_CDATA);
    }

    public Set<Long> limitMenus() {
        return limitResources("NavigatorPermission", "menus", "menu_", PortalConstants.NAVIGATOR_VIEW_OPERRATION);
    }

    private Set<Long> limitResources(String permissionTable, String rType, String prefix, String... opts) {
        Set<Long> result = new LinkedHashSet<Long>();
        String hql1 = "select distinct p.resourceId from " + permissionTable + " p  where p.operationId = ? and p.roleId in (";

        String domain = Environment.getDomain();
        List<ModuleDef> list = queryDomainModules(domain);
        List<Long> ownRoles = new ArrayList<>(Environment.getOwnRoles()); // clone 一份角色列表
        for (ModuleDef md : list) {
            String roles = (String) EasyUtils.checkNull(md.getRoles(), "-999");
            roles += "," + EasyUtils.checkNull(md.getRoles_free(), "-999");

            // 查出模块的roles拥有查看权限的所有功能（菜单），如果模块没有单独指定功能（菜单），则取所有授予模块所含角色的功能（菜单）
            List<Long> rList = md.resourcelist(rType);
            if (rList.isEmpty()) {
                for (String opt : opts) {
                    result.addAll((List<Long>) commonDao.getEntities(hql1 + roles + ")", opt));
                }
            } else {
                result.addAll(rList);
            }

            String[] _roles = roles.split(",");
            for (String _role : _roles) {
                ownRoles.remove(EasyUtils.obj2Long(_role));
            }
        }

        String roles = (String) EasyUtils.checkNull(EasyUtils.list2Str(ownRoles), "-999");
        for (String opt : opts) {
            result.addAll((List<Long>) commonDao.getEntities(hql1 + roles + ")", opt)); // 模块以外的角色，直接授予的
        }

        filterByDomain(domain, result, prefix);
        return result;
    }

    /*
     * 需要有效期内的策略对应的 模块：
     * eg：A同时购买了E8财务版 和 E8高级版，都有财务角色，但财务版限制了部分功能；现高级版策略已经到期，则只保留财务版的功能.
     * */
    private List<ModuleDef> queryDomainModules(String domain) {
        String hql = "from ModuleDef "
        		+ " where id in (select moduleId from ModuleUser where domain = ?) "
                + "   and id in (select sa.moduleId from SubAuthorize sa where ? in (sa.buyerId,sa.ownerId) and sa.endDate > now()) ";

        Object userId = Environment.getNotnullUserId();
        List<ModuleDef> list = (List<ModuleDef>) commonDao.getEntities(hql, domain, userId);

        Object umc = Environment.getInSession(SSOConstants.USER_MODULE_C);
        Set<String> domainModules = new HashSet<>((List<String>) EasyUtils.checkNull(umc, new ArrayList<>()));
        for (ModuleDef md : list) {
            domainModules.remove(md.getCode());
        }
        domainModules.add("-999");
        
        list.addAll((List<ModuleDef>) commonDao.getEntities("from ModuleDef where code in (" + DMUtil.insertSingleQuotes(EasyUtils.list2Str(domainModules)) + ")"));

        return list;
    }

    /**
     * 按域信息里配置的资源菜单黑（白）名单，二次过滤角色授予及Module里定义的资源列表；
     * 优先级：黑名单 > 白名单
     * <p>
     * 白名单：适合放不在模块里的发布的定制菜单，直接授予角色和域即可
     */
    private void filterByDomain(String domain, Set<Long> resources, String tag) {
        String white_list = (String) Environment.getDomainInfo("white_list");
        String[] white_lists = EasyUtils.checkNull(white_list, "-999").toString().split(",");
        for (String rid : white_lists) {
            resources.add(EasyUtils.str2Long(rid.replaceFirst(tag, "")));
        }

        String black_list = (String) Environment.getDomainInfo("black_list");
        String[] black_lists = EasyUtils.checkNull(black_list, "-999").toString().split(",");
        for (String rid : black_lists) {
            resources.remove(EasyUtils.str2Long(rid.replaceFirst(tag, "")));
        }
    }

    /************************************* cloud order **************************************/

    public CloudOrder createOrder(CloudOrder co) {
        return createOrder(co, false);
    }

    public CloudOrder createOrder(CloudOrder co, Boolean forceRegister) {
        Map<String, String> map = new HashMap<>();
        try {
            map = EasyUtils.json2Map(co.getParams());
        } catch (Exception e) {
        }

        boolean isNewUser = false;
        if (Environment.isAnonymous() || forceRegister) {
            isNewUser = selfRegister(co, map); // 新注册购买
        }

        // for 购买时补充填写用户信息
        String phone = map.get("phone");
        String userName = map.get("user_name");
        String orgName = map.get("company_name");

        String userCode = (String) EasyUtils.checkNull(phone, Environment.getUserCode());
        User user = userService.getUserByLoginName(userCode);
        user.setUserName((String) EasyUtils.checkNull(userName, user.getUserName()));
        user.setUdf((String) EasyUtils.checkNull(orgName, user.getUdf()));
        user.setDisabled(ParamConstants.FALSE); // 第一次下单没有支付，账号在自注册域，状态停用；第二次再重新下单先启用
        commonDao.update(user);

        // 模拟登录
        if (Environment.isAnonymous()) {
            apiService.mockLogin(userCode);
        }
        co.setCreator(userCode);
        co.setType((String) EasyUtils.checkNull(co.getType(), ModuleOrderHandler.class.getName()));

        AbstractProduct product = AbstractProduct.createBean(co);
        product.beforeOrder();
        co.setProduct(product.getName());

        co.setStatus(CloudOrder.NEW);
        co.setOrder_date(new Date());
        co = (CloudOrder) commonDao.create(co);

        co.setOrder_no(co.getOrder_date().getTime() + "-" + co.getId());
        co.setCreator(userCode);

        commonDao.update(co);

        // 如果是下单生成的自注册新用户，则先停用，支付后启用
        if (isNewUser || UMConstants.SELF_REGISTER_GROUP_ID.equals(Environment.getUserGroupId())) {
            user.setDisabled(ParamConstants.TRUE);
            commonDao.update(user);
        }

        return co;
    }

    private Boolean selfRegister(CloudOrder mo, Map<String, String> map) {
        // 校验短信验证码smsCode
        String smsCode = map.get("smsCode");
        String mobile = map.get("phone");
        if (mo.checkSMS) {
            if (EasyUtils.isNullOrEmpty(smsCode) || !AbstractSMS.create().checkCode(mobile, smsCode)) {
                throw new BusinessException("短信验证码校验失败，请重新输入。");
            }
        }

        List<?> users = commonDao.getEntities(" from User where ? in (loginName, telephone, email)", mobile);
        if (users.size() > 0) {
            return false;
        }

        // 注册账号
        User user = new User();
        user.setLoginName(mobile);
        user.setTelephone(mobile);
        user.setBelongUserId(mo.getInvite_user_id());
        user.setUserName(mobile);
        user.setPassword(map.get("password"));
        userService.regUser(user, true);
        user.setOrigin("云注册");
        return true;
    }


    public void handle(String order_no, Double real_money, String payer, String payType, String payClazz, Map<?, ?> trade_map) {
        AbstractProduct iAfterPay = AbstractProduct.createBean(order_no);
        iAfterPay.afterPay(trade_map, real_money, payer, payType, payClazz);
    }

    public Result refund(String order_no, Double refund_fee, String out_refund_no, String feeType, boolean toBalance) {
        AbstractProduct abstractProduct = AbstractProduct.createBean(order_no);

        CloudOrder co = abstractProduct.co;
        if (!CloudOrder.PAYED.equals(co.getStatus())) {
            return new Result(false, EX.parse(EX.CO_1, CloudOrder.PAYED));
        }
        
        String payType = co.getPay_type();
        boolean moneyBackToAccount = false;
        Result result;
        // 余额支付的特殊处理掉，其余 支付宝 微信 传化等接口实现
        if (toBalance || CloudOrder.PAYTYPE_1.equals(payType)) {
            result = new Result(true, "");
            moneyBackToAccount = true;
        } else {
            result = abstractProduct.refund(refund_fee, out_refund_no);
        }

        if (result.getResult()) {
            // 检查是否退过款了
        	feeType = (String) EasyUtils.checkNull(feeType, "退" + co.getProduct());
            List<AccountFlow> accountFlows = cloudDao.getAccountFlowsByFeeType(feeType, out_refund_no);
            if ( accountFlows.isEmpty() ) {
                Account account = cloudDao.getAccountByBelongUserName(co.getCreator());
                if (moneyBackToAccount) {
                    account.deduct(-refund_fee); // 返回到余额 账户资金变动
                }
                
                // 记流水
                AccountFlow flow = new AccountFlow(account, feeType, "退回" + payType + ",退款单号：" + out_refund_no);
                flow.setMoney(refund_fee);
                commonDao.createObject(flow);
            }
        }

        return result;
    }

    public void setSubAuthorizeRoles(Long userId, String ruIds, Long strategyId) {
        SubAuthorize sa = (SubAuthorize) commonDao.getEntity(SubAuthorize.class, strategyId);
        sa.setOwnerId(userId);
        commonDao.update(sa);

        List<RoleUser> rus = new ArrayList<>();
        List<Long> ruIDList = new ArrayList<>();

        String[] _ruIds = ruIds.split(",");
        for (String _ruId : _ruIds) {
            Long ruId = EasyUtils.obj2Long(_ruId);
            Object ru = commonDao.getEntity(RoleUser.class, ruId);
            if (ru != null) {
                ruIDList.add(ruId);
                rus.add((RoleUser) ru); // 让打钩的排前面，排后面会导致冗余的删不掉
            }
        }
        Object exclude = EasyUtils.checkTrue(ruIDList.isEmpty(), "", " and id not in(" + ruIds + ")");
        rus.addAll((List<RoleUser>) commonDao.getEntities("from RoleUser where strategyId = ? " + exclude, strategyId));

        // 剔除掉冗余的RoleUser记录
        Map<String, RoleUser> map = new HashMap<>();

        for (RoleUser ru : rus) {
            String key = ru.getModuleId() + "-" + ru.getStrategyId() + "-" + ru.getRoleId();
            if (ruIDList.contains(ru.getId())) {
                ru.setUserId(userId);
                commonDao.update(ru);
            } else {
                if (map.containsKey(key)) {
                    commonDao.delete(ru);
                } else {
                    ru.setUserId(null);
                    commonDao.update(ru);
                }
            }
            map.put(key, ru);
        }
    }

    public void fastCreateModuleUser(String user_name, String phone, Long moduleId, Long logistics_id) {
        CloudOrder co = new CloudOrder();
        co.setAccount_num(1);
        co.setMonth_num(500);
        co.setModule_id(moduleId);
        co.setMoney_cal(0D);

        ModuleDef md = (ModuleDef) commonDao.getEntity(ModuleDef.class, moduleId);
        co.setType(md.getProduct_class());

        Map<String, String> params = new HashMap<String, String>();
        params.put("phone", phone);
        params.put("password", phone);
        params.put("user_name", user_name);
        params.put("company_name", user_name);
        params.put("logistics_id", logistics_id.toString());
        co.setParams(EasyUtils.obj2Json(params));

        co.checkSMS = false;
        createOrder(co, true);

        // 自动支付 admin支付
        Double money_real = 0D;
        co.setMoney_real(money_real);
        co.singleSubAuthorize = true;

        AbstractProduct iAfterPay = AbstractProduct.createBean(co);
        iAfterPay.afterPay(null, money_real, AbstractProduct.ADMIN_PAYER, "线下", null);
    }
}
