/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um;

import com.boubei.tss.framework.web.mvc.BaseActionSupport;

/**
 * UM相关常量定义文件
 */
public final class UMConstants {
	
	/** 有效期设置 */
    public static final int ROLE_LIFE_TIME = 50;      // 角色有效期(50年)
	public static final int STRATEGY_LIFE_TIME = 7;   // 策略有效期（7天）
    
	/**
	 * 远程接口路径设置
	 */
	public static final String GENERAL_SEARCH_SERVICE_URL = "/remote/RemoteSearchService"; //综合查询的远程接口路径
	public static final String PERMISSION_SERVICE_URL     = "/remote/PermissionService";  //授权的远程接口路径
 
	/**
	 * 同步功能相关常量定义
	 */	
	public static final String DATA_SOURCE_TYPE_LDAP = "ldap";
	public static final String DATA_SOURCE_TYPE_DB   = "db";
	
	
	// ===========================================================================
	// 定义一些约定好的常量
	// ===========================================================================
	
	/**
	 * 判断是否为新建对象(和前台约定好的)
	 */
	public static final Long DEFAULT_NEW_ID = BaseActionSupport.DEFAULT_NEW_ID; 

    public static final String ADMIN_ROLE      = "系统管理员";      // 管理员角色
    public static final Long ADMIN_ROLE_ID     = new Long(-1);    // 管理员角色的ID
    public static final String ANONYMOUS_ROLE  = "$匿名角色";
    public static final Long ANONYMOUS_ROLE_ID = new Long(-10000);// 匿名角色的ID
    public static final Long DOMAIN_ROLE_ID = new Long(-8);  // 域管理员角色ID
    public static final Long DEV_ROLE_ID    = new Long(-9);  // 开发者角色ID

    public static final String ADMIN_USER      = "Admin";          //系统管理员的ID(初始化数据库时写死的)
	public static final Long ADMIN_USER_ID     = new Long(-1);     //系统管理员的ID(初始化数据库时写死的)
	public static final Long ANONYMOUS_USER_ID = new Long(-10000); //匿名用户的ID(初始化数据库时写死的)
	public static final Long ROBOT_USER_ID     = new Long(-997);   //机器人用户：用于JOB执行等，虚拟账号，实际不存在
	public static final String ROBOT_USER_NAME = "Job.Robot";
    
	public static final Long DEFAULT_ROOT_ID          = 0L;  /** 默认根结点ID */
	public static final Long MAIN_GROUP_ID            = -2L; //主用户组的ID (初始化数据库时写死的)
	public static final Long ASSISTANT_GROUP_ID       = -3L; //辅助用户组的ID(初始化数据库时写死的)
	public static final Long SELF_REGISTER_GROUP_ID   = -7L; //自注册用户组的ID(初始化数据库时写死的)
	public static final Long DOMAIN_ROOT_ID 		  = -8L; //域用户组的ID
	public static final Long DEV_GROUP_ID 		  	  = -9L; //开发者用户组的ID
	
	public static final Long ROLE_ROOT_ID             = -6L; //角色的ID(视图生成)
	
	public static final String DEFAULT_DOMAIN = "无域";
	public static final String COMPANY_DOMAIN = "企业域";
	
	// ===========================================================================
    // 资源权限相关
    // ===========================================================================
    
    /**
     * 系统类型id
     */
    public static final String PLATFORM_SYSTEM_NAME = "平台系统"; 
    public static final String OTHER_SYSTEM_NAME    = "其他系统"; 
    
    public static final String PLATFORM_SYSTEM_APP = "-1"; 
    public static final String OTHER_SYSTEM_APP    = "-2"; 
    
    /**
     * 权限选项id的常量定义完毕
     */
    public static final String APPLICATION_TREE_NODE  = "1";  //应用系统节点id
    public static final String RESOURCETYPE_TREE_NODE = "2";  //资源类型节点id
    public static final String OPERATION_TREE_NODE    = "3";  //权限选项节点id

    /**
     * 授权模式
     */
    public static final String IGNORE_PERMISSION           = "0"; // 不关心授权模式
    public static final String LOWER_PERMISSION            = "1"; // 普通授权
    public static final String AUTHORISE_PERMISSION        = "2"; // 可授权授权
    public static final String PASSON_AUTHORISE_PERMISSION = "3"; // 可传递授权
    public static final String SUB_AUTHORISE_PERMISSION    = "4"; // 权限转授
    
    /** 
     * 授权时层次节点的权限维护状态(1-仅此节点,2-该节点及所有下层节点) 
     */
    public static final Integer PERMIT_NODE_SELF = 1; // 仅此节点
    public static final Integer PERMIT_SUB_TREE  = 2; // 该节点及所有下层节点
    
    /**
     * 权限项纵向依赖的类型
     */
    public static final String DEPEND_UP_DOWN = "1"; // 向上兼向下
    public static final String DEPEND_UP      = "2"; // 向上
    public static final String DEPEND_DOWN    = "3"; // 向下
    
    /**
     * 项目标识以及资源类型
     */
    public final static String TSS_APPLICATION_ID = "tss";
    
    public final static String GROUP_RESOURCE_TYPE_ID  = "1"; // 用户组资源类型ID
    public final static String ROLE_RESOURCE_TYPE_ID   = "2"; // 角色资源类型ID

    /**
     * 用户组资源操作选项 (0表示不判断权限)，默认给新建（子组、用户）的权限
     */
    public final static String GROUP_VIEW_OPERRATION  = "1";  // 参看用户组
    public final static String GROUP_EDIT_OPERRATION  = "2";  // 维护用户组
    
    /**
     * 角色(组)资源操作选项 (0表示不判断权限)
     */
    public final static String ROLE_VIEW_OPERRATION = "1";  // 参看角色
    public final static String ROLE_EDIT_OPERRATION = "2";  // 维护角色
    
	/**
	 * Grid模板文件的路径定义
	 */
	public static final String GENERAL_SEARCH_ROLE_GRID     = "template/um/grid/gs_role_info.xml";// 综合查询根据用户搜索角色情况的Grid模板
	public static final String GENERAL_SEARCH_USER_GRID     = "template/um/grid/gs_user_info.xml";//综合查询根据角色搜索用户情况的Grid模板	
	public static final String GENERAL_SEARCH_STRATEGY_GRID = "template/um/grid/gs_subauth_info.xml";// 综合查询搜索用户因转授而获得的角色的情况的Grid模板
	
	public static final String MAIN_USER_GRID  = "template/um/grid/user_list.xml"; //用户组下的用户浏览Grid模板
	public static final String MESSAGE_GRID  = "template/um/grid/message_grid.xml";
	
	/**
	 *  XForm模板文件的路径定义
	 */
	public static final String GROUP_MAIN_XFORM  = "template/um/xform/group_main.xml";    
	public static final String GROUP_ASSISTANT_XFORM  = "template/um/xform/group_assistant.xml";
	
	public static final String USER_REGISTER_XFORM  = "template/um/xform/user_register.xml";  
	public static final String USER_BASEINFO_XFORM  = "template/um/xform/user_baseinfo.xml"; 
	
	public static final String ROLE_XFORM      = "template/um/xform/role.xml";    
	public static final String ROLEGROUP_XFORM = "template/um/xform/roleGroup.xml"; 
	public static final String STRATEGY_XFORM  = "template/um/xform/subauth.xml"; 
    
	public static final String RESOURCETYPE_XFORM      = "template/um/xform/resourceType.xml";        //ResourceType(新建)
	public static final String APPLICATION_XFORM       = "template/um/xform/application.xml";         //Application(新建)
	public static final String OTHER_APPLICATION_XFORM = "template/um/xform/application_other.xml";   //其他应用系统(新建)
	public static final String OPERATION_XFORM         = "template/um/xform/operation.xml";           //Operation对象
	public static final String SERACH_PERMISSION_XFORM = "template/um/xform/searchpermission.xml";    //查询权限的
	public static final String AUTH_METHOD_XFORM       = "template/um/xform/authenticatemethod.xml";  //设置用户认证方式
    public static final String PASSWORD_FORGET_XFORM   = "template/um/xform/password_forget.xml";     //密码忘记提示 
    public static final String PASSWORD_CHANGE_XFORM   = "template/um/xform/password_change.xml";     //密码修改

	/**
	 * 对象节点 Tree 图标文件的路径定义
	 */
	public static final String GROUP_TREENODE_ICON      = "images/user_group_";     //Group图标(停用)
	public static final String STRATEGY_TREENODE_ICON = "images/rule_"; // 权限转授图标
	public static final String USER_GRID_NODE_ICON  = "images/user_";  //User图标
	public static final String GROUP_ROLE_TREENODE_ICON = "images/role_group_";  //角色组图标
	public static final String ROLE_TREENODE_ICON = "images/role_";   //角色图标

	public static final String APPLICATION_TREENODE_ICON   = "images/app.gif";   //应用图标
	public static final String RESOURCETYPE_TREENODE_ICON = "images/resource_type.gif";//资源图标
	public static final String OPERATION_TREENODE_ICON   = "images/permission.gif";    //权限选项Tree图标
	public static final String RESOURCE_TREENODE_ICON   = "images/resource1.gif";      //资源Tree图标
}