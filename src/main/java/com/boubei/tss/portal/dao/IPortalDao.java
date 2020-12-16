/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.dao;

import java.util.List;

import com.boubei.tss.framework.persistence.ITreeSupportDao;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.um.permission.filter.PermissionFilter4Branch;
import com.boubei.tss.um.permission.filter.PermissionTag;

public interface IPortalDao extends ITreeSupportDao<Structure>{
    
    /**
     * 本方法将会被拦截进行资源注册操作，所以一般的保存应该直接调save方法。
     * @param p
     * @return
     */
    Structure saveStructure(Structure ps);
    
    /**
     * 删除门户节点。 
     * 删除注册资源由拦截器ResourcePermissionInterceptor完成。
     * @param ps
     */
    void deleteStructure(Structure ps);

    /**
     * 根据父亲节点获取所有子节点。
     * 必须根据decode来排序，以保证层次和顺序结构。
     * 
     * @param id
     *         父节点Id
     * @param operationId
     *         操作ID，拦截器用
     * @return
     *         所有子节点
     */
    @PermissionTag(
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Branch.class)
    List<Structure> getChildrenById(Long id, String operationId);

    /**
     * 获取所有的父亲级节点，一直到根目录。
     * 必须根据decode来排序，以保证层次和顺序结构。
     * 
     * @param id
     *         当前节点
     * @param operationId 
     *         操作ID ，拦截器用
     * @return
     */
    @PermissionTag(
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Branch.class)
    List<Structure> getParentsById(Long id, String operationId);
    
    /**
     * 获取指定门户Portal的所有组成元素
     * @param portalId
     *              门户ID
     * @param currentThemeId 
     * @return
     */
    Object[] getPortalComponents(Long portalId, Long currentThemeId);

    /**
     * 获取portal下的所有主题
     * @param portalId
     * @return
     */
    List<?> getThemesByPortal(Long portalId);
}

