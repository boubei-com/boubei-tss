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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.appserver.AppServer;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.cloud.CloudService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.MacrocodeCompiler;

@Service("NavigatorService")
public class NavigatorService implements INavigatorService {

	@Autowired CloudService cloudService;
	@Autowired private INavigatorDao dao;

    public List<?> getAllNavigator(){
        return dao.getEntities("from Navigator o order by o.decode");
	}
	
	public Navigator saveNavigator(Navigator entity){
		if( entity.getId() == null ) {
		    Long parentId = entity.getParentId();
            Integer nextSeqNo = dao.getNextSeqNo(parentId);
            entity.setSeqNo(nextSeqNo);
		}
		
		return dao.save(entity);
	}
	
	public void deleteNavigator(Long id){
        List<Navigator> children = dao.getChildrenById(id, PortalConstants.NAVIGATOR_EDIT_OPERRATION);
        for( Navigator child : children ){
            dao.deleteNavigator(child);
        }
	}
	
	public Navigator getNavigator(Long id){
        return dao.getEntity(id);
	}
	
	public void disable(Long id, Integer disabled) {
        List<Navigator> list;
        if(ParamConstants.TRUE.equals(disabled)) {
            list = dao.getChildrenById(id, PortalConstants.NAVIGATOR_EDIT_OPERRATION);
        } 
        else {
            list = dao.getParentsById(id);    
        }
        
        for(  Navigator temp : list ){
            temp.setDisabled(disabled);
            dao.updateWithoutFlush(temp);
        }
	}

    public void sort(Long id, Long targetId, int direction) {
        dao.sort(id, targetId, direction);
    }

    public void moveNavigator(Long id, Long targetId) {
        Navigator navigator = dao.getEntity(id);
        
        navigator.setParentId(targetId);
        navigator.setSeqNo(dao.getNextSeqNo(targetId));
                   
        dao.moveEntity(navigator);
    }
    
    public String getNavigatorXML(Long id) {
		Navigator navigator = getNavigator(id);  
		if(navigator == null) {
			return "<MainMenu/>";
		}
		
		List<Navigator> menuItems = dao.getMenuItems(id, Environment.getUserId());
		
		// 解析地址参数中 ${APP_URL}等信息，取 AppServer的配置信息。
		Collection<AppServer> appservers = Context.getApplicationContext().getAppServers();
		Map<String, Object> macros = new HashMap<String, Object>();
		for(AppServer appserver : appservers) {
			macros.put("${" + appserver.getCode() + "_URL}", appserver.getBaseURL());
		}
	 
		Element node = navigator.compose2Tree(menuItems);
		if(node == null) {
			return "<MainMenu/>";
		}
		return MacrocodeCompiler.run(node.asXML(), macros, true);
	}
    
    public List<Navigator> getMenuItems(Long id) {
		return dao.getMenuItems(id, Environment.getUserId());
	}
	
	public List<MenuDTO> getMenuTree(Long id) {
		List<Navigator> list = getMenuItems(id);
		
		// Module 白名单过滤
		Set<Long> limits = cloudService.limitMenus();
		for(Iterator<Navigator> it = list.iterator(); it.hasNext(); ) {
			Navigator m = it.next();
			if( !limits.contains(m.getId()) ) {
				it.remove();
			}
		}
		
		return MenuDTO.buildTree(id, list);
	}
}

	