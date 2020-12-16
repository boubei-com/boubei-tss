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

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.mock.dao._IGroupDAO;
import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.persistence.TreeSupportDao;

@Repository("_GroupDAO")
public class _GroupDAO extends TreeSupportDao<_Group> implements _IGroupDAO {

    public _GroupDAO() {
        super(_Group.class);
    }
    
    public void deleteGroup(_Group group) {
        List<_Group> children = getChildrenById(group.getId());
        
        deleteAll(getEntities("from _User"));
        deleteAll(children);
    }

}
