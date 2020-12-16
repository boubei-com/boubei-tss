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

import java.util.List;
import java.util.Set;

import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.pay.Result;

public interface CloudService {

	CloudOrder createOrder(CloudOrder mo);

	List<?> listAvaliableModules();

	List<?> listSelectedModules(Long user);

	void unSelectModule(Long user, Long module);

	void selectModule(Long user, Long module);

	void refreshModuleUserRoles(Long module);

	void setSubAuthorizeRoles(Long userId, String roleIds, Long strategyId);
	
	Set<Long> limitReports();
	
	Set<Long> limitRecords();
	
	Set<Long> limitMenus();
	
	void fastCreateModuleUser(String user_name, String phone, Long moduleId,Long logistics_id);

	Result refund(String order_no, Double refund_fee, String out_refund_no, String feeType, boolean toBalance);
}
