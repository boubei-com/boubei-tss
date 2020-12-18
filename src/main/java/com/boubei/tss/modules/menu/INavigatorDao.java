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

import com.boubei.tss.cache.aop.Cached;
import com.boubei.tss.cache.extension.CacheLife;
import com.boubei.tss.framework.persistence.ITreeSupportDao;
import com.boubei.tss.um.permission.filter.PermissionFilter4Branch;
import com.boubei.tss.um.permission.filter.PermissionTag;

public interface INavigatorDao extends ITreeSupportDao<Navigator> {

    /**
     * 保存一个菜单或菜单项。
     * @param navigator
     * @return
     */
    Navigator save(Navigator navigator);
    
    /**
     * 删除一个导航栏。
     * @param navigator
     */
    void deleteNavigator(Navigator navigator);
    
    /**
     * 根据菜单获取其下的所有菜单项集合。
     * 本方法将被拦截以进行权限过滤。
     * @param menuId
     * @param userId 用于对不同用户进行针对性缓存
     * @return
     */
    @PermissionTag(filter = PermissionFilter4Navigator.class)
    @Cached(cyclelife = CacheLife.SHORT)
    List<Navigator> getMenuItems(Long menuId, Long userId);
 
    @PermissionTag(
            resourceType = PortalConstants.NAVIGATOR_RESOURCE_TYPE,
            filter = PermissionFilter4Branch.class)
    List<Navigator> getChildrenById(Long id, String operationId);
}
