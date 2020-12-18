/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.menu;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.TreeSupportDao;
 
@Repository("NavigatorDao")
public class NavigatorDao extends TreeSupportDao<Navigator> implements INavigatorDao{

	public NavigatorDao() {
		super(Navigator.class);
	}
 
    public Navigator save(Navigator navigator) {
    	if(navigator.getId() == null) {    
            return create(navigator);
        }
        
        return navigator;
    }
    
    public void deleteNavigator(Navigator navigator){
        delete(em.merge(navigator));
    }
    
    public List<Navigator> getMenuItems(Long id, Long userId) {
        return getChildrenById(id);
    }

	public List<Navigator> getChildrenById(Long id, String operationId) {
		return getChildrenById(id);
	}
}

	