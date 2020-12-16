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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.modules.cloud.entity.Account;
import com.boubei.tss.modules.cloud.entity.AccountFlow;
import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.persistence.IEntity;


@Repository("CloudDao")
public class CloudDaoImpl extends BaseDao<IEntity> implements CloudDao {

    public CloudDaoImpl() {
        super(IEntity.class);
    }

    public Map<String, Object> getPaginationEntities(String hql, int page, int rows) {
        int first = (page - 1) * rows;
        int max = page * rows;
        List<?> l = this.em.createQuery(hql).setFirstResult(first).setMaxResults(max).getResultList();

        String countHQL = "select count(*) " + hql.substring(hql.indexOf(" from"), hql.indexOf(" order by"));
        Object total = getEntities(countHQL).get(0);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("rows", l);
        result.put("total", total);

        return result;
    }

    @SuppressWarnings("unchecked")
	public List<AccountFlow> getAccountFlowsByFeeType(String feeType, String remark) {
        String hql = " from AccountFlow where type = ? and remark like '%" + remark + "'";
        return (List<AccountFlow>) getEntities(hql, feeType);
    }

    public Account getAccountByBelongUserName(String loginName) {
        List<?> accounts = getEntities(" from Account where belong_user.loginName = ?", loginName);
        return (Account) accounts.get(0);
    }

}
