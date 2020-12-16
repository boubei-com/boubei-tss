/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock.dao.impl;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.mock.dao._IUserDAO;
import com.boubei.tss.framework.mock.model._User;
import com.boubei.tss.framework.persistence.BaseDao;

@Repository("_UserDAO")
public class _UserDAO extends BaseDao<_User> implements _IUserDAO {

    public _UserDAO() {
        super(_User.class);
    }

}
