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
 
public interface PortalConstants {
 
    public static final Long ROOT_ID = 0L;
 
    public static final String PORTAL_CACHE    = "cache-portal-";    // 门户树缓存
    public static final String NAVIGATOR_CACHE = "cache-navigator-"; // 导航栏缓存

    /**
     * 模板路径
     */
    public static final String PORTALSTRUCTURE_XFORM = "template/portal/Structure.xml";     // 门户结构
    public static final String RELEASE_XFORM_TEMPLET = "template/portal/ReleaseConfig.xml"; // 门户发布信息模板

    /**
     * 资源文件目录
     */
    public static final String MODEL_DIR        = "modules/portal/model/";
    public static final String PORTAL_MODEL_DIR = "/" + MODEL_DIR + "portal";
    
    /** 项目标识以及资源类型 */
    public final static String PORTAL_RESOURCE_TYPE    = "4";
    public final static String NAVIGATOR_RESOURCE_TYPE = "5";
    
    /**
     * 门户结构操作选项 (0表示不判断权限)
     */
    public final static String PORTAL_NONE_OPERRATION   = "0"; //不过滤权限
    public final static String PORTAL_VIEW_OPERRATION   = "1"; //Portal查看操作ID
    public final static String PORTAL_EDIT_OPERRATION   = "2"; //Portal维护操作ID
    public final static String PORTAL_DEL_OPERRATION    = "3"; //Portal删除操作ID
    public final static String PORTAL_ADD_OPERRATION    = "4"; //Portal新增操作ID
    public final static String PORTAL_ORDER_OPERRATION  = "5"; //Portal排序操作ID
    public final static String PORTAL_STOP_OPERRATION   = "6"; //Portal停用操作ID
    public final static String PORTAL_START_OPERRATION  = "7"; //Portal启用操作ID
    
    public final static String[] PORTAL_OPERRATIONS = new String[]{"0", "1", "2", "3", "4", "5", "6", "7"};
    public final static String[] PORTAL_PARENT_OPERRATIONS = new String[]{"p_0", "p_1", "p_2", "p_3", "p_4", "p_5", "p_6", "p_7"};
    
    /**
     * 导航栏操作选项 (0表示不判断权限)
     */
    public final static String NAVIGATOR_NONE_OPERRATION   = "0"; //不过滤权限
    public final static String NAVIGATOR_VIEW_OPERRATION   = "1"; //MENU浏览操作ID
    public final static String NAVIGATOR_EDIT_OPERRATION   = "2"; //MENU维护操作ID
    
    public final static String[] NAVIGATOR_OPERRATIONS = new String[]{"0", "1", "2"};
    public final static String[] NAVIGATOR_PARENT_OPERRATIONS = new String[]{"p_0", "p_1", "p_2"};
}

