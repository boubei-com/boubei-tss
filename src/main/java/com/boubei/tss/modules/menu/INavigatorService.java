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

import com.boubei.tss.modules.log.Logable;
import com.boubei.tss.um.permission.filter.PermissionTag;
 
public interface INavigatorService {

	/**
     * 获取所有菜单
	 * @return
	 */
	@PermissionTag(
			operation = PortalConstants.NAVIGATOR_VIEW_OPERRATION, 
			resourceType = PortalConstants.NAVIGATOR_RESOURCE_TYPE)
	List<?> getAllNavigator();
	
	/**
     * 新建一个菜单或菜单项
	 * @param entity
	 * @return
	 */
	@Logable(operateObject="菜单按钮", operateInfo=" 新建/修改了 ${returnVal} 节点 ")
	Navigator saveNavigator(Navigator entity);
	
	/**
     * 删除一个菜单或者菜单项
	 * @param id
	 */
	@Logable(operateObject="菜单按钮", operateInfo=" 删除了 ID为 ${args[0]} 的 节点 ")
	void deleteNavigator(Long id);
	
	/**
     * 停/启用一个菜单或者菜单项
	 * @param id
	 * @param disabled
	 */
	@Logable(operateObject="菜单按钮", operateInfo="<#if args[1]=1>停用<#else>启用</#if>了(ID: ${args[0]})节点 ")
	void disable(Long id,Integer disabled);
	
	/**
     * 获取一个菜单或者菜单项
	 * @param id
	 * @return
	 */
	Navigator getNavigator(Long id);
	
	/**
     * 菜单项排序
	 * @param id
	 * @param targetId
	 * @param direction
	 */
	void sort(Long id, Long targetId, int direction);

    /**
     * 移动菜单项
     * @param id
     * @param targetId
     */
    void moveNavigator(Long id, Long targetId);

    /**
     * 生成一个菜单下所有菜单项集合的XML格式数据。
     * 注：本方法主要供门户取菜单调用，比较频繁，在Dao调用时对其进行了缓存。
     * 
     * @param id
     * @return
     */
    String getNavigatorXML(Long id);
    
    List<Navigator> getMenuItems(Long id);
    
    List<MenuDTO> getMenuTree(Long id);
}
