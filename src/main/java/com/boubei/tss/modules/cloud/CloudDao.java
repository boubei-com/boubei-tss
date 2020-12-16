/* ==================================================================   
 * Created [2006-12-28] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud;

import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.persistence.IDao;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;

public interface CloudDao extends IDao<IEntity> {
	
	 Map<String, Object> getPaginationEntities(String hql, int page, int rows);

	 List<AccountFlow> getAccountFlowsByFeeType(String feeType, String remark);

	 Account getAccountByBelongUserName(String loginName);
}
