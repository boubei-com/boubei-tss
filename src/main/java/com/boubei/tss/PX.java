/* ==================================================================   
 * Created [2017-10-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */
package com.boubei.tss;

/**
 * 系统可注册的参数配置（application.properties文件 和 component_param表）
 * report_export_url = http://www.boubei.com:8082  导出数据分流机器, 前台页面报表导出时用到
 * sysTitle = TSS BI   登陆页大标题
 * subTitle = Be your own data hero  登录页小标题
 * index_logo = logo-scm.png   LOGO路径
 * mobile_menu = 72    移动端菜单组ID
 * domain_page 默认域管理员登录后首页， 初次注册后登录可强制进入domain.html：http://ip/tss/bi.html?domain_page=domain.html
 */
public interface PX {
	
	// Config: 只能在application.properties里配置
	
	/** 配置文件中应用编号属性名：系统获取Code值，如TSS、CMS */
	static String APPLICATION_CODE = "application.code";
	static String ENVIRONMENT = "environment";

	/** Spring配置文件， 默认为spring.xml */
	static String SPRING_CONTEXT_PATH = "aplication.context";

	/** 是否启用定时Job */
	static String ENABLE_JOB = "job.enable";
	
	// ParamConfig：component_param 或 application.properties
	
	/** 数据表是否只做逻辑删除 */
	static String LOGIC_DEL = "LOGIC_DEL";
	
	/** 关闭注册 regable = "false"， 域账号兼有开发者功能：EBI环境，regable = "REG_BDEV"  */
	static String REGABLE = "regable";
	
	/** 关闭开发者注册 regableDev = "false" */
	static String REGABLE_DEV = "regableDev";
	
	/** 报表服务是否启用三分钟缓存，正式环境必需启用，测试环境可禁用 */
	static String REPORT_CACHE = "REPORT_CACHE";
	
    /** 
     * 资源地址白名单，白名单内的资源允许【匿名访问】
     * url.white.list = /version,.in,.do,.portal,login.html,_forget.html,_register.html.....
     */
	static String URL_WHITE_LIST = "url.white.list";
	
    /** IP黑白名单，百名单内的ip允许跨域访问系统的服务和资源，黑名单禁止访问任何资源；*/
    static String IP_WHITE_LIST = "ip.white.list";
    static String IP_BLACK_LIST = "ip.black.list";
    
    /** 开启攻击防御  */
    static String DENY_MASS_ATTACK = "DENY_MASS_ATTACK";
    static String MAX_HTTP_REQUEST = "MAX_HTTP_REQUEST";
    static String MIN_REQUEST_INTERVAL = "MIN_REQUEST_INTERVAL";
    
    /** session生命周期，单位（秒）*/
	static String SESSION_CYCLELIFE_CONFIG = "session.cyclelife";
	
	/** 日志缓冲池最多可存日志条数的参数  */
    static String LOG_FLUSH_MAX_SIZE = "log_flush_max_size";
    
    /** 报表自定义展示页上传目录 */
    static String REPORT_TL_DIR = "report.template.dir";
    
    /**  用户自定义的参与Freemarker解析参数  */
    static String USER_DEFINED_PARAMS = "userdefinedParams";
    
    /** 邮件服务器配置 email.sys、email.default等 */
    static String MAIL_SERVER_ = "email.";
    static String MAIL_SERVER_PORT = "email.port.";
    
    /** 对含有此处配置的关键字的错误异常进行邮件提醒 */
    static String ERROR_KEYWORD = "error.keyword";
    
    /** LDAP认证地址 */
    static String OA_LDAP_URL = "oa.ldap.url";
    
    /** 如果是从其它系统单点登录到平台（TSS），则自动转到配置的门户首页地址 */
    static String SSO_INDEX_PAGE = "sso.index.page";
    
    // ParamManager: component_param
    
    /** 强制用户修改密码间隔天数。默认180天 */
    static String PASSWD_CYCLELIFE = "passwd.cyclelife";
    
    /** 读取最新、最热门、最近访问报表时，选取的日志天数，日志量大的，不宜取太多天。默认3天 */
    static String TOP_REPORT_LOG_DAYS = "TOP_REPORT_LOG_DAYS";
    
    /** 可以指定一个系统管理员之外的角色为超级管理员 */
    static String ADMIN_ROLE = "ADMIN_ROLE";
    
    /** QueryCache支持的最大等待线程数量，没有配置默认100 */
    static String MAX_QUERY_REQUEST = "MAX_QUERY_REQUEST";
    
	/** 后台Action单独发布的数据服务，用于report/record为下拉框选择数据服务 */
	static String DATA_SERVICE_CONFIG = "DATA_SERVICE_CONFIG";
	
	/** /home/tssbi/temp 用于报表导出及数据表的附件上传 */
	static String ATTACH_PATH  = "TEMP_EXPORT_PATH"; 
	
	/** connectionpool: 默认数据源 */
	static String DEFAULT_CONN_POOL = "default_conn_pool";
	
	/** comboParams  数据源下拉列表 */
	static String DATASOURCE_LIST   = "datasource_list";
	
	/** param Group  缓存池 */
	static String CACHE_PARAM = "CACHE_PARAM";
	
	/** 维护用于邮件推送的配置（收件人列表）*/
	static String EMAIL_MACRO  = "EmailMacros";
	static String NOTIFY_AFTER_PAY_LIST  = "NOTIFY_AFTER_PAY_LIST";
	
	/** 导入XLSX文件大小上限, 单位K */
	static String MAX_XLSX_SIZE  = "MAX_XLSX_SIZE";
	
	/** 导入图片文件大小上限，超出则进行压缩, 单位K */
	static String MAX_PIC_SIZE  = "MAX_PIC_SIZE";
	
	/** 策略到期预先几天提醒 */
	static String SA_EXPIRE_NOTIFY_DAYS = "SA_EXPIRE_NOTIFY_DAYS";
	
	/** 系统体验账号 */
	static String SYS_TEST_USER = "SYS_TEST_USER";
	
	/** 系统管理员邮箱 */
	static String MONITORING_RECEIVERS = "Monitoring-Receivers";
	
	static String PUBLIC_DOMAINS = "PUBLIC_DOMAINS";
	
}
