/* ==================================================================   
 * Created [2009-09-27] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.service;

import java.util.List;

import com.boubei.tss.modules.log.Logable;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.entity.ReleaseConfig;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.entity.Theme;
import com.boubei.tss.portal.permission.PermissionFilter4Portal;
import com.boubei.tss.um.permission.filter.PermissionFilter4Check;
import com.boubei.tss.um.permission.filter.PermissionFilter4Create;
import com.boubei.tss.um.permission.filter.PermissionFilter4Move;
import com.boubei.tss.um.permission.filter.PermissionFilter4Sort;
import com.boubei.tss.um.permission.filter.PermissionFilter4Update;
import com.boubei.tss.um.permission.filter.PermissionTag;
 
public interface IPortalService {

    /**
     * <p>
     * 获取所有门户结构PortalStructure
     * </p>
     */
    @PermissionTag(
            operation = PortalConstants.PORTAL_VIEW_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE)
    List<?> getAllStructures();

    /**
     * 获取所有启用门户
     */
    @PermissionTag(
            operation = PortalConstants.PORTAL_VIEW_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE)
    List<?> getActivePortals();

    /**
     * 获取指定门户下启用的页面和版面
     * @param portalId
     */
    @PermissionTag(
            operation = PortalConstants.PORTAL_VIEW_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE)
    List<?> getActivePagesByPortal(Long portalId);
    
    /**
     * 获取门户下所有可用的门户结构节点列表（不包括门户根节点）
     * @param portalId
     */
    @PermissionTag(
            operation = PortalConstants.PORTAL_VIEW_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE)
    List<?> getStructuresByPortal(Long portalId);

    /**
     * <p>
     * 获取单个门户结构PortalStructure的节点信息，<p></p>
     * 如果该门户结构PortalStructure是根节点，则要一块取出其对应门户Portal的信息
     * </p>
     * @param id
     *         门户结构主键ID值
     * @return
     *         门户结构对象
     */
    @PermissionTag(
            operation = PortalConstants.PORTAL_VIEW_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Check.class)
    Structure getStructureWithTheme(Long id);
    
    /**
     * 只取门户结构节点
     * @param id
     * @return
     */
    @PermissionTag(
            operation = PortalConstants.PORTAL_VIEW_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Check.class)
    Structure getStructure(Long id);

    /**
     * <p>
     * 保存门户结构信息，如果该门户结构PortalStructure是根节点，则要一块保存其门户Portal的信息。<p></p>
     * 更新操作也一样。
     * 
     * save()结束返回 该门户结构PortalStructure 对象
     * </p>
     * 
     * @param ps
     *            要保存的门户结构实体
     * @return
     *            保存成功以后的门户结构实体
     */
    @Logable(operateObject="门户结构", operateInfo="新建了 ${returnVal} 节点")
    @PermissionTag(
            operation = PortalConstants.PORTAL_ADD_OPERRATION , 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Create.class)
    Structure createStructure(Structure ps);
    
    @Logable(operateObject="门户结构", operateInfo="修改了 ${returnVal} 节点")
    @PermissionTag(
            operation = PortalConstants.PORTAL_EDIT_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Update.class)
    Structure updateStructure(Structure ps);
    
    /**
     * <p>
     * 删除门户结构PortalStructure
     * 如果有子节点，同时删除子节点（递归过程，子节点的子节点......)
     * </p>
     * 
     * @param id
     *          操作节点的ID
     */
    @Logable(operateObject="门户结构", operateInfo="删除了(ID: ${args[0]})节点")
    void deleteStructure(Long id);

    /**
     * <p>
     * 停用/启用 门户结构PortalStructure（将其下的disabled属性设为"1"/"0"）<p></p>
     * 停用时，如果有子节点，同时停用所有子节点（递归过程，子节点的子节点......)<p></p>
     * 启用时，如果有父节点且父节点为停用状态，则启用父节点（也是递归过程，父节点的父节点......）<p></p>
     * 前提：用户对被操作的节点及其所有子节点（或父节点）有停用启用的权限。
     * </p>
     * 
     * @param id
     *          操作节点的ID
     * @param disabled
     *          停用或者启用（1/0）
     */
    @Logable(operateObject="门户结构", operateInfo="<#if args[1]=1>停用<#else>启用</#if>了(ID: ${args[0]})节点")
    void disable(Long id, Integer disabled);

    /**
     * * <p>
     * 排序，同节点下的节点排序（一次只能排一个）
     * </p>
     * 
     * @param id
     *          排序节点ID
     * @param targetId
     *          目标节点ID
     * @param direction
     *          方向
     */
    @Logable(operateObject="门户结构", operateInfo="(ID: ${args[0]})节点移动到了(ID: ${args[1]})节点<#if args[2]=1>之下<#else>之上</#if>")
    @PermissionTag(
            operation = PortalConstants.PORTAL_ORDER_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Sort.class)
    void sort(Long id, Long targetId, int direction);
    
    /**
     * 移动门户结构（portlet）到页面或其它版面下
     * @param id
     * @param container
     */
    @Logable(operateObject="门户结构", operateInfo="(ID: ${args[0]})节点移动到了(ID: ${args[1]})节点里.")
    @PermissionTag(
            operation = PortalConstants.PORTAL_ADD_OPERRATION + "," + PortalConstants.PORTAL_DEL_OPERRATION, 
            resourceType = PortalConstants.PORTAL_RESOURCE_TYPE,
            filter = PermissionFilter4Move.class
    )
    void move(Long id, Long container);
    
    /**
     * 获取一个门户树的一份拷贝。
     * 方法将会被权限过滤拦截器拦截，用户没有权限的门户结构节点将会被过滤掉。
     * 如果用户是匿名用户，则应该调用本方法，因为匿名用户没有自定义门户。
     * 
     * @param portalId
     * @param themeId 
     * @return
     */
    @PermissionTag(filter = PermissionFilter4Portal.class)
    PortalNode getPortal(Long portalId, Long themeId);
    
    
    /****************************   门户相关的（包括门户主题、门户发布等）的相关维护  *************************************************/
    
    //******************************* 以下为主题管理 ***************************************************************
    /**
     * 门户下的当前主题另存为。。。
     * @param themeId 
     * @param themeName
     */
    @Logable(operateObject="门户主题", operateInfo="复制出名为${args[1]}的新主题(SourceID: ${args[0]})")
    Theme saveThemeAs(Long themeId, String themeName);

    /**
     * 获取一个Portal的所有主题
     * @param portalId
     * @return
     */
    List<?> getThemesByPortal(Long portalId);

    /**
     * 设置默认主题
     * @param themeId
     */
    @Logable(operateObject="门户主题", operateInfo="将(ID: ${args[0]})主题设置为默认主题")
    void specifyDefaultTheme(Long themeId);

    /**
     * 删除主题，如果删除的主题是当前门户的默认主题或者当前主题，则删除失败
     * @param portalId
     * @param themeId
     */
    @Logable(operateObject="门户主题", operateInfo="删除了(ID: ${args[0]})主题")
    void removeTheme(Long themeId);

    /**
     * 重命名主题名字
     * @param themeId
     * @param name
     */
    @Logable(operateObject="门户主题", operateInfo="将(ID: ${args[0]})主题重新命名为 ${args[1]}")
    void renameTheme(Long themeId, String name);
    
    
    //******************************* 以下为门户发布管理 ***************************************************************
    /**
     * 根据访问地址或者门户的真实地址
     * @param visitUrl
     * @return
     */
    ReleaseConfig getReleaseConfig(String visitUrl);

    /**
     * 获取所有的门户发布信息
     * @return
     */
    List<?> getAllReleaseConfigs();

    /**
     * 保存发布信息
     * @param issueInfo
     * @return
     */
    ReleaseConfig saveReleaseConfig(ReleaseConfig issueInfo);

    /**
     * 移除发布信息
     * @param id
     */
    void removeReleaseConfig(Long id);

    /**
     * 获取发布信息
     * @param id
     * @return
     */
    ReleaseConfig getReleaseConfig(Long id);
    
    
    //***********************************  门户流量统计获取 ***************************************************************
    /**
     * 获取门户下页面的访问流量
     * @param portalId
     * @return
     */
    List<?> getFlowRate(Long portalId);
}