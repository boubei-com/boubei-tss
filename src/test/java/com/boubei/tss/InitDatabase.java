/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.entity.Article;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.entity.permission.ChannelPermission;
import com.boubei.tss.cms.service.IArticleService;
import com.boubei.tss.cms.service.IChannelService;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.ext.ImportRecord;
import com.boubei.tss.dm.ext.ImportReport;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record.file.ImportCSV;
import com.boubei.tss.dm.record.permission.RecordPermission;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.dm.report.permission.ReportPermission;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.portal.entity.Navigator;
import com.boubei.tss.portal.service.IComponentService;
import com.boubei.tss.portal.service.INavigatorService;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.um.permission.ResourcePermission;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IResourceService;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.XMLDocUtil;

/**
 * 初始化数据库。
 * 
 * 步骤：
 * 1、将 src/test/resources "Remove from build path"（需使用 src/main/resources目录下的配置文件）
 *    改完后重新 clean 一下 project
 * 2、创建一个空的名为tss的MySQL库，如库已存在，先drop再重建;
 * 3、run test: InitDatabase, 确保 step1 在 step2 前面执行;
 * 4、重新执行: mvn clean eclipse:eclipse, 刷新 project;
 * 
 * 注：也而已取其它名字, 需同时把application.properties的连接改成新名字，初始化完成后再改回tss
 */
@ContextConfiguration(
        locations={
          "classpath:META-INF/spring-framework.xml",  
          "classpath:META-INF/spring-um.xml",
          "classpath:META-INF/spring.xml"
        } 
      )
//@Rollback // 不自动回滚
@Transactional
public class InitDatabase extends AbstractTransactionalJUnit4SpringContextTests { 
 
    Logger log = Logger.getLogger(this.getClass());    
    
    @Autowired private IResourceService resourceService;
    @Autowired private ResourcePermission resourcePermission;
    @Autowired private ILoginService loginSerivce;
    
    @Autowired private IComponentService elementService;
    @Autowired private INavigatorService navigatorService;
    @Autowired private IChannelService channelService;
    @Autowired private IArticleService articleService;
    
    @Autowired private ReportService reportService;
    @Autowired private RecordService recordService;
    @Autowired private PermissionHelper ph;
    
    @Before
    public void setUp() throws Exception {
        Global.setContext(super.applicationContext);
        
        // 初始化虚拟登录用户信息
        OperatorDTO loginUser = new OperatorDTO(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
        String token = TokenUtil.createToken("1234567890", UMConstants.ADMIN_USER_ID); 
        IdentityCard card = new IdentityCard(token, loginUser);
        Context.initIdentityInfo(card);
    }
    
    private boolean isTestEnv() {
    	return "test".equals( Config.getAttribute("environment") );
    }
    
    @Test
    public void step1() {
    	if( isTestEnv() ) return;
    	
    	log.info("step1: init tss base starting......");
 
        String sqlpath = _TestUtil.getInitSQLDir();
        _TestUtil.excuteSQL(sqlpath);
 
        log.info("step1: over.");
    }
    
    @Test
    public void step2() { /*-------------------------------初始化系統的必要数据 -------------------- */
    	if( isTestEnv() ) return;
    	
        log.info("step2: init tss data starting......");
      
        // 获取登陆用户的权限（拥有的角色）并保存到用户权限（拥有的角色）对应表
        loginSerivce.saveUserRoleMapping(UMConstants.ADMIN_USER_ID);
 
        initUM();
        initDM();
        initCMS();
        initPortal();
        
        importSystemProperties();
    }
    
    @Test
    public void step3() { /*-------------------------------初始化系統的其它数据 -------------------- */
    	if( isTestEnv() ) return;
    	
    	log.info("step3: init system reports & records.");
        try {         	
 			initReportsAndRecords();
 		} catch (Exception e) {
 			log.error("初始化系统自带的Report和Record时出错了", e);
 		}
         
        log.info("step3: over.");
    }
 
    /**
     * 初始化UM、CMS、Portal相关应用、资源类型、权限选型信息
     */
    private void initUM() {
        /* 初始化应用系统、资源、权限项 */
        String sqlpath = _TestUtil.getInitSQLDir();
        Document doc = XMLDocUtil.createDocByAbsolutePath(sqlpath + "/../tss-resource-config.xml");
        resourceService.applicationResourceRegister(doc, UMConstants.PLATFORM_SYSTEM_APP);
        
        // 补全SQL初始化出来的系统级用户组
        Long[] groupIds = new Long[] {-2L, -3L, -7L, -8L, -9L};
        for(Long groupId : groupIds) {
        	resourcePermission.addResource(groupId, UMConstants.GROUP_RESOURCE_TYPE_ID);
        }
        
        // 给“域管理员”管理 企业域 的权限
        _TestUtil.mockPermission("um_permission_group", "$企业域", UMConstants.DOMAIN_ROOT_ID, UMConstants.DOMAIN_ROLE_ID, UMConstants.GROUP_EDIT_OPERRATION, 2, 0, 0);
        _TestUtil.mockPermission("um_permission_group", "$企业域", UMConstants.DOMAIN_ROOT_ID, UMConstants.DOMAIN_ROLE_ID, UMConstants.GROUP_VIEW_OPERRATION, 2, 0, 0);
        
        // 给与管理【辅助组】的权限
        _TestUtil.mockPermission("um_permission_group", "辅助用户组", -3L, UMConstants.DOMAIN_ROLE_ID, UMConstants.GROUP_EDIT_OPERRATION, 2, 0, 0);
        _TestUtil.mockPermission("um_permission_group", "辅助用户组", -3L, UMConstants.DOMAIN_ROLE_ID, UMConstants.GROUP_VIEW_OPERRATION, 2, 0, 0);
    }
    
    // 数据源配置
    private void initDM() {
    	Param group = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "数据源配置");
    	ParamManager.addSimpleParam(group.getId(), PX.DEFAULT_CONN_POOL, "默认数据源", "connectionpool");
        
        Param dlParam = ParamManager.addComboParam(group.getId(), PX.DATASOURCE_LIST, "数据源列表");
        ParamManager.addParamItem(dlParam.getId(), "connectionpool", "本地数据源", ParamConstants.COMBO_PARAM_MODE);
    }
    
    private void initCMS() {
    	 Channel site = new Channel();
         site.setParentId(CMSConstants.HEAD_NODE_ID);
         site.setName("我的站点");
         site.setPath("temp");
         site.setDocPath("doc");
         site.setImagePath("img");
         site.setOverdueDate("0");
         
         site = channelService.createSite(site);
         
         Channel channel = new Channel();
         channel.setName("公告栏");
         channel.setOverdueDate(site.getOverdueDate());
         channel.setParentId(site.getId());
         channel = channelService.createChannel(channel);
         
         Article article = new Article();
         article.setTitle("欢迎来到它山石的世界");
         article.setAuthor("Jon.King");
         article.setKeyword("它山石 BI 数据管理");
         article.setChannel(channel);
         String content = " 欢迎来到它山石的世界";
         article.setContent(content);
         article.setStatus(CMSConstants.TOPUBLISH_STATUS);
         articleService.createArticle(article, channel.getId(), "", -1L);
         
         ph.createPermission(UMConstants.DOMAIN_ROLE_ID, site, CMSConstants.OPERATION_VIEW, 1, 0, 0, ChannelPermission.class.getName());
         ph.createPermission(UMConstants.DOMAIN_ROLE_ID, channel, CMSConstants.OPERATION_VIEW, 1, 0, 0, ChannelPermission.class.getName());
         ph.createPermission(UMConstants.DOMAIN_ROLE_ID, channel, CMSConstants.OPERATION_ADD_ARTICLE, 1, 0, 0, ChannelPermission.class.getName());
         ph.createPermission(UMConstants.DOMAIN_ROLE_ID, channel, CMSConstants.OPERATION_EDIT, 1, 0, 0, ChannelPermission.class.getName());
         ph.createPermission(UMConstants.DOMAIN_ROLE_ID, channel, CMSConstants.OPERATION_PUBLISH, 1, 0, 0, ChannelPermission.class.getName());
         
         ph.createPermission(UMConstants.ANONYMOUS_ROLE_ID, channel, CMSConstants.OPERATION_VIEW, 1, 0, 0, ChannelPermission.class.getName());
    }
    
    /** 初始化默认的修饰器，布局器 */
    private void initPortal() {
        Component layoutGroup = new Component();
        layoutGroup.setName("布局器组");
        layoutGroup.setIsGroup(true);
        layoutGroup.setType(Component.LAYOUT_TYPE);
        layoutGroup.setParentId(PortalConstants.ROOT_ID);   
        layoutGroup = elementService.saveComponent(layoutGroup);
        
        Component defaultLayout = new Component();
        defaultLayout.setIsDefault(ParamConstants.TRUE);
        defaultLayout.setParentId(layoutGroup.getId());   
        Document document = XMLDocUtil.createDoc("template/portal/defaultLayout.xml");
        org.dom4j.Element propertyElement = document.getRootElement().element("property");
        String layoutName = propertyElement.elementText("name");
        defaultLayout.setName(layoutName);
        defaultLayout.setPortNumber(new Integer(propertyElement.elementText("portNumber")));
        defaultLayout.setDefinition(document.asXML());
        elementService.saveComponent(defaultLayout);
        
        Component decoratorGroup = new Component();
        decoratorGroup.setName("修饰器组");
        decoratorGroup.setIsGroup(true);
        decoratorGroup.setType(Component.DECORATOR_TYPE);
        decoratorGroup.setParentId(PortalConstants.ROOT_ID);  
        decoratorGroup = elementService.saveComponent(decoratorGroup);
        
        Component defaultDecorator = new Component();
        defaultDecorator.setIsDefault(ParamConstants.TRUE);
        defaultDecorator.setParentId(decoratorGroup.getId());
        
        document = XMLDocUtil.createDoc("template/portal/defaultDecorator.xml");
        propertyElement = document.getRootElement().element("property");
        String decoratorName = propertyElement.elementText("name");
        defaultDecorator.setName(decoratorName);
        defaultDecorator.setDefinition(document.asXML());
        elementService.saveComponent(defaultDecorator);
        
        Component portletGroup = new Component();
        portletGroup.setName("portlet组");
        portletGroup.setIsGroup(true);
        portletGroup.setType(Component.PORTLET_TYPE);
        portletGroup.setParentId(PortalConstants.ROOT_ID);   
        portletGroup = elementService.saveComponent(portletGroup);
        
        // 新建一个应用菜单组（不依附于门户）
        Navigator appMenuGroup = new Navigator();
        appMenuGroup.setName("应用菜单组");
        appMenuGroup.setType(Navigator.TYPE_MENU);
        appMenuGroup.setParentId(PortalConstants.ROOT_ID);
        navigatorService.saveNavigator(appMenuGroup);
    }
    
    /**
     * 导入 application.properties文件 和 appServers.xml
     */
    public void importSystemProperties(){
        String name = "系统参数";
        Param param = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, name);
        ResourceBundle resources = ResourceBundle.getBundle("application", Locale.getDefault());
        if (resources == null) return;
        
        ParamManager.addSimpleParam(param.getId(), "sysTitle", "系统名称", "它山石");
        ParamManager.addSimpleParam(param.getId(), "TEMP_EXPORT_PATH", "目录（附件）", "target/temp");
        ParamManager.addSimpleParam(param.getId(), "upload_path", "目录（上传）", "target/upload");
        ParamManager.addSimpleParam(param.getId(), "REPORT_CACHE", "缓存报表数据", "false");
       
        ParamManager.addSimpleParam(param.getId(), "regable", "开放注册", "REG_BDEV");
        ParamManager.addSimpleParam(param.getId(), "welcomeMsg", "注册成功欢迎词", "在这个世界里，您可以自由的管理您的数据，您是自己数据的真正主宰者。" +
        		"如果您是一个企业或个人用户，我们为您提供了丰富多彩的各行各业模块功能，您可以随意挑选试用；" +
        		"如果您是一个开发者，您可以在自己开发目录下自由的开发各种功能，然后发布给其它用户使用。所有这一切，都从注册一个它山石平台账号开始！");
        
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("session.cyclelife", "登录超时（秒）");
        m.put("url.white.list", "资源地址白名单");
        m.put("ip.white.list", "IP白名单");
        m.put("error.keyword", "Error关键字");
        m.put("report.template.dir", "目录（报表资源）");
        m.put("email.sys", "邮件服务器（系统）");
        m.put("email.default", "邮件服务器（业务）");
        
        for (Enumeration<String> enumer = resources.getKeys(); enumer.hasMoreElements();) {
            String key = enumer.nextElement();
            if(!m.containsKey(key)) continue;
            
            String pname = m.get(key);
            String value = resources.getString(key);
            ParamManager.addSimpleParam(param.getId(), key, pname, value);
        }
 
        Param paramGroup = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "应用服务配置");
        
        Document doc = XMLDocUtil.createDoc("tss/appServers.xml");
        List<?> elements = doc.getRootElement().elements();
        for (Iterator<?> it = elements.iterator(); it.hasNext();) {
            org.dom4j.Element element = (org.dom4j.Element) it.next();
            String appName = element.attributeValue("name");
            String appCode = element.attributeValue("code");
            ParamManager.addSimpleParam(paramGroup.getId(), appCode, appName, element.asXML());
        }
    }
    
    public void initReportsAndRecords() throws Exception {
    	// 1.系统自带报表
    	String initDir = _TestUtil.getSQLDir() + "/_init/";
		String json = FileHelper.readFile( initDir + "reports.json" );
    	new ImportReport().createReports(json, "connectionpool", Report.DEFAULT_PARENT_ID);
    	
    	// 2.系统自带数据表
		Record.SYS_TABLES.add("um_user_token");
		Record.SYS_TABLES.add("component_job_def");
		
    	json = FileHelper.readFile( initDir + "records.json" );
    	new ImportRecord().createRecords(json, "connectionpool", Record.DEFAULT_PARENT_ID);
    	
    	Long jobRecordId = recordService.getRecordID("系统定时器", Record.TYPE1, true);
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	request.addParameter("recordId", jobRecordId.toString());
    	new ImportCSV().processUploadFile(request, initDir + "/jobs.xlsx", null);
    	_TestUtil.mockPermission("dm_permission_record", "系统定时器", jobRecordId, UMConstants.DEV_ROLE_ID, Record.OPERATION_CDATA, 1, 0, 0);
    	_TestUtil.mockPermission("dm_permission_record", "系统定时器", jobRecordId, UMConstants.DOMAIN_ROLE_ID, Record.OPERATION_CDATA, 1, 0, 0);
    	
    	Long etlRecordId = recordService.getRecordID("ETL任务", Record.TYPE1, true);
    	request = new MockHttpServletRequest();
    	request.addParameter("recordId", etlRecordId.toString());
    	new ImportCSV().processUploadFile(request, initDir + "/etls.xlsx", null);
    	_TestUtil.mockPermission("dm_permission_record", "ETL任务", etlRecordId, UMConstants.DOMAIN_ROLE_ID, Record.OPERATION_CDATA, 1, 0, 0);
    	
    	// 修改ETL的JobID
    	String updateSQL = "update dm_etl_task t set t.jobId = (select id from component_job_def where name = t.jobName )";
    	SQLExcutor.excute(updateSQL, DMConstants.LOCAL_CONN_POOL);
    	
    	Long fbRecordId = recordService.getRecordID("问题需求反馈", Record.TYPE1, true);
    	_TestUtil.mockPermission("dm_permission_record", "问题需求反馈", fbRecordId, Anonymous._ID, Record.OPERATION_CDATA, 1, 0, 0);
    	
    	Long rlRecordId = recordService.getRecordID("功能模块发布", Record.TYPE1, true);
    	_TestUtil.mockPermission("dm_permission_record", "功能模块发布", rlRecordId, UMConstants.DEV_ROLE_ID, Record.OPERATION_CDATA, 1, 0, 0);
    
    	// 3.再初始化两个目录：我的报表、我的功能
    	Report group1 = new Report();
    	group1.setName("我的报表");
    	group1.setRemark("在此目录下创建你自己的报表吧。open it");
    	group1.setType(Report.TYPE0);
    	group1.setParentId(Report.DEFAULT_PARENT_ID);
    	reportService.createReport(group1);
    	reportService.startOrStop(group1.getId(), 0);
    	
    	Record group2 = new Record();
    	group2.setName("我的功能");
    	group2.setRemark("在此目录下创建你自己的功能数据表吧。open it");
    	group2.setType(Report.TYPE0);
    	group2.setParentId(Report.DEFAULT_PARENT_ID);
    	recordService.createRecord(group2);
    	recordService.startOrStop(group2.getId(), 0);
    	
    	// 报表权限默认授予“开发者”角色
    	Report root1 = new Report();
    	root1.setId(Report.DEFAULT_PARENT_ID);
    	root1.setName("root");
    	ph.createPermission(UMConstants.DOMAIN_ROLE_ID, root1, Report.OPERATION_VIEW, 1, 0, 0, ReportPermission.class.getName());
    	ph.createPermission(UMConstants.DEV_ROLE_ID, root1, Report.OPERATION_VIEW, 1, 0, 0, ReportPermission.class.getName());
    	ph.createPermission(UMConstants.DOMAIN_ROLE_ID, group1, Report.OPERATION_VIEW, 1, 0, 0, ReportPermission.class.getName());
    	ph.createPermission(UMConstants.DEV_ROLE_ID, group1, Report.OPERATION_VIEW, 1, 1, 0, ReportPermission.class.getName());
    	
    	Record root2 = new Record();
    	root2.setId(Record.DEFAULT_PARENT_ID);
    	root2.setName("root");
    	ph.createPermission(UMConstants.DOMAIN_ROLE_ID, root1, Record.OPERATION_CDATA, 1, 0, 0, RecordPermission.class.getName());
    	ph.createPermission(UMConstants.DEV_ROLE_ID, root1, Record.OPERATION_CDATA, 1, 0, 0, RecordPermission.class.getName());
    	ph.createPermission(UMConstants.DOMAIN_ROLE_ID, group2, Record.OPERATION_CDATA, 1, 0, 0, RecordPermission.class.getName());
    	ph.createPermission(UMConstants.DEV_ROLE_ID, group2, Record.OPERATION_CDATA, 1, 1, 0, RecordPermission.class.getName());
    }
}
