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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.framework.web.display.tree.StrictLevelTreeParser;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.util.EasyUtils;

@Controller
@RequestMapping("/auth/navigator")
public class NavigatorAction extends BaseActionSupport {

	@Autowired private INavigatorService service;
    
    /**
     * 生成单个菜单
     */
	@RequestMapping("/xml/{id}")
    public void getNavigatorXML(HttpServletResponse response, @PathVariable("id") Long id) {
        print("MainMenu", service.getNavigatorXML(id));
    }
	
	@RequestMapping("/json/{id}")
	@ResponseBody
    public List<MenuDTO> getNavigatorJson(@PathVariable("id") Long id) {
        return service.getMenuTree(id);
    }
	
    /** 刷新一下参数的缓存 */
	@RequestMapping("/cache/{key}")
    public void flushCache(HttpServletResponse response, @PathVariable("key") Object key) {
		CacheHelper.flushCache(CacheLife.SHORT.toString(), key.toString());
		
        printSuccessMessage();
    }
	
	private void flushMenuCache() {
		CacheHelper.flushCache(CacheLife.SHORT.toString(), "menu");
		CacheHelper.flushCache(CacheLife.SHORT.toString(), "Menu");
	}
    
	/**
	 * <p>
	 * 菜单的树型展示。
     * 菜单依附于门户而存在，要想给某角色授于菜单管理权限，首先要授予门户节点的查看权限。
	 * </p>
	 */
	@RequestMapping("/list")
	public void getAllNavigator4Tree(HttpServletResponse response) {        
        List<?> data = service.getAllNavigator();
        TreeEncoder encoder = new TreeEncoder(data, new StrictLevelTreeParser(PortalConstants.ROOT_ID));
		encoder.setNeedRootNode(false);
        print("MenuTree", encoder);
	}

	/**
	 * 单个菜单的详细信息
	 */
	@RequestMapping("/{id}")
	public void getNavigatorInfo(HttpServletResponse response, HttpServletRequest request, 
			@PathVariable("id") Long id) {
		
		Object type;
		Map<String, Object> map;
        if( DEFAULT_NEW_ID.equals(id) ) {
        	map = new HashMap<String, Object>();
            map.put("target", "_self");
            map.put("parentId", request.getParameter("parentId"));
            map.put("portalId", request.getParameter("portalId"));
        	map.put("type", type = request.getParameter("type"));
        }
        else {
        	Navigator info = service.getNavigator(id);            
        	map = info.getAttributes4XForm();
        	type = info.getType();
        }        
        XFormEncoder encoder = new XFormEncoder("template/portal/MenuXForm" + type + ".xml", map);;
        
        print("MenuInfo", encoder); 
	}
	
	/**
	 * 保存菜单
	 */
	@RequestMapping(method = RequestMethod.POST)
	public void save(HttpServletResponse response, Navigator navigator) {
        boolean isNew = navigator.getId() == null;
        
        navigator = service.saveNavigator(navigator);
        flushMenuCache();
        
        doAfterSave(isNew, navigator, "MenuTree");
	}
	
	/**
	 * 删除菜单
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(HttpServletResponse response, @PathVariable("id") Long id) {
		service.deleteNavigator(id);
		flushMenuCache();
		
		printSuccessMessage();
	}
	
	/**
	 * 停用/启用 菜单Navigator（将其下的disabled属性设为"1"/"0"）
	 */
    @RequestMapping("/disable/{id}/{state}")
    public void disable(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("state") int state) {
    	
		service.disable(id, state);
		flushMenuCache();
		
        printSuccessMessage();
	}
	
	/**
	 * 同组下的Navigator排序
	 */
	@RequestMapping(value = "/sort/{id}/{targetId}/{direction}", method = RequestMethod.POST)
	public void sort(HttpServletResponse response, 
            @PathVariable("id") Long id, 
            @PathVariable("targetId") Long targetId,  
            @PathVariable("direction") int direction) {
		
		service.sort(id, targetId, direction);   
		flushMenuCache();
		
        printSuccessMessage();
	}
    
    /**
     * 移动
     */
	@RequestMapping(value = "/move/{id}/{targetId}", method = RequestMethod.POST)
    public void moveTo(HttpServletResponse response, 
    		@PathVariable("id") Long id, 
    		@PathVariable("targetId") Long targetId) {
		
        if(id.equals(targetId)) {
            return;
        }
        service.moveNavigator(id, targetId);
        flushMenuCache();
        
        printSuccessMessage();
    }
	
	@RequestMapping("/operations/{resourceId}")
    public void getOperations(HttpServletResponse response, @PathVariable("resourceId") Long resourceId) {
        List<String> list = PermissionHelper.getInstance().getOperationsByResource(resourceId,
                        NavigatorPermission.class.getName(), NavigatorResource.class);

        print("Operation", EasyUtils.list2Str(list));
    }
}