/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.mock.dao;

import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.persistence.ITreeSupportDao;

public interface _IGroupDAO extends ITreeSupportDao<_Group> {
 
    void deleteGroup(_Group group);
    
}