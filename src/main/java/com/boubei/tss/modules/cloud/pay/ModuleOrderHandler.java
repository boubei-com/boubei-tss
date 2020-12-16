package com.boubei.tss.modules.cloud.pay;

import java.util.List;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * @author hank 购买模块成功后续操作
 */
public class ModuleOrderHandler extends AbstractProduct {

	static final String priceDefDefault = "${account_num}*${month_num}*${price?c}";

	protected ILoginService loginService = (ILoginService) Global.getBean("LoginService");

	public String domain;
	public Boolean singleSubAuthorize = false;

	public ModuleOrderHandler(){
		super();
	}

	public ModuleOrderHandler(CloudOrder co) {
		super(co);
		this.md = (ModuleDef) dao.getEntity(ModuleDef.class, co.getModule_id());
	}

	public String getName() {
		return md.getModule();
	}

	/*
	 * 检验 购买账号 月份 限制
	 */
	protected void beforeOrderModuleCheck() {
		checkLimit(md.getAccount_limit(), co.getAccount_num(), "账号", true, false);
		
		// 对于未创建过独立域的订单受起购月份限制（这里的逻辑会导致EFF会员每个订单都会受这个限制）
		checkLimit(md.getMonth_limit(), co.getMonth_num(), "月", isNewDomain(buyer.getId()), false);
	}

	protected void checkLimit(String limits, int value, String unit, boolean checkMin, boolean isRenewalfee) {
		String[] limit = EasyUtils.checkNull(limits, "1,999").toString().split(",");
		int min = EasyUtils.obj2Int(limit[0]);
		int max = EasyUtils.obj2Int(limit[limit.length - 1]);
		if( isRenewalfee ) {
			max = (int) EasyUtils.checkTrue(max == 1, 0, max);
			min = (int) EasyUtils.checkTrue(max == 0, 0, min);
		}

		if ((checkMin && value < min) || value > max || value <= 0) {
			Object op = EasyUtils.checkTrue(isRenewalfee, "续费", "购买");
			throw new BusinessException("【" + md.getModule() + "】不支持" + op + ", 请重新购买");
		}
	}

	private boolean isNewDomain(Long userId) {
		List<Long> hasRoles = loginService.getRoleIdsByUserId(userId);
		List<?> hasGroups = loginService.getGroupsByUserId(userId);

		return hasGroups.size() == 1 || (hasGroups.size() > 1 && !hasRoles.contains(UMConstants.DOMAIN_ROLE_ID));
	}

	protected void handle() {
		// 购买成功，正式启用用户（if新用户）;
		if (ParamConstants.TRUE.equals(buyer.getDisabled())) {
			buyer.setDisabled(ParamConstants.FALSE);
			dao.update(buyer);
		}

		/*
		 * 注册企业域，并将用户移动到新建的域下作为域管理员。 判断user是否已经是域管理员（非自注册域），是的话无需再注册域;
		 * 回调时user非登录状态
		 */
		Long buyerId = buyer.getId();
		boolean isNewDomain = isNewDomain(buyerId);
		if (isNewDomain) {
			String domainName = (String) EasyUtils.checkNull(buyer.getUdf(), buyer.getLoginName());
			userService.regBusiness(buyer, domainName, this.md.getModule_group());
			domain = buyer.getDomain();
		} 
		else {
			Object[] group = loginService.getMainGroup(buyerId);
			domain = (String) group[2];
			buyer.setGroupId( (Long) group[0] ); // 设置buyer的groupId，方便需要在 域组下建组或账号
			buyer.setDomain(domain);
		}

		// 创建模块授权策略
		Long module_id = co.getModule_id();
		createSubAuthorize(isNewDomain); 

		// 如果此时还没有选择 购买过此模块， 在此创建 ModuleUser 映射关系
		String hql = "from ModuleUser where userId = ? and moduleId = ? and domain = ? and isBuy = 1";
		List<?> list = dao.getEntities(hql, buyerId, module_id, domain);
		if (list.isEmpty()) {
			ModuleUser mu = new ModuleUser(buyerId, module_id);
			mu.setIsBuy(ParamConstants.TRUE);
			mu.setDomain(domain);
			dao.createObject(mu);
		}

		createFlows(getBuyerAccount());
	}

	public void setPrice() {
		String price_def;
		if (noProxy) {
			co.setPrice(md.getPrice());
			price_def = (String) EasyUtils.checkNull(md.getPrice_def(), priceDefDefault);
		} else {
			// 查找这个用户的设置价格 优先取针对用户的报价，其次是-1的（代理设置的默认价）
			String hql = " select price from ProxyPrice where buyer = ? and creator = ? and module.id = ? ";
			List<?> proxyPrices = dao.getEntities(hql, co.getCreator(), proxy.getLoginName(), co.getModule_id());
			if (proxyPrices.isEmpty()) {
				proxyPrices = dao.getEntities(hql, "default", proxy.getLoginName(), co.getModule_id());
			}
			if (proxyPrices.isEmpty()) {
				throw new BusinessException("代理商未设置价格，无法下单！");
			}
			co.setPrice((Double) proxyPrices.get(0));
			price_def = priceDefDefault;
		}

		Double money = EasyUtils.eval(price_def, BeanUtil.getProperties(co));

		if (co.getRebate() != null) {
			money *= co.getRebate();
		}
		if (co.getDerate() != null) {
			money -= co.getDerate();
		}

		co.setMoney_cal((double) Math.round(money * 100) / 100);
	}

	/**
	 * 获取订单代理价（admin代理的，代理价为0元）
	 */
	protected Double getProxyPrice() {
		Double proxyCost = co.getAccount_num() * co.getMonth_num() * contract.getPrice();

		/*
		 * 用户现金100余额0，扣代理成本40 用户现金40余额60，返点代理20 公式 = - money_real + money_cal -
		 * proxyCost = 余额支付 - 代理价
		 */
		//return co.getMoney_balance() - proxyCost;
		return -proxyCost;
	}

}
