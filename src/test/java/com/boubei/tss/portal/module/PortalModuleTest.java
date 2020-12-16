/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.portal.AbstractTest4Portal;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.PortalDispatcher;
import com.boubei.tss.portal.action.ComponentAction;
import com.boubei.tss.portal.action.NavigatorAction;
import com.boubei.tss.portal.action.PortalAction;
import com.boubei.tss.portal.dao.IPortalDao;
import com.boubei.tss.portal.engine.macrocode.DecoratorMacrocodeContainer;
import com.boubei.tss.portal.engine.macrocode.LayoutMacrocodeContainer;
import com.boubei.tss.portal.engine.model.DecoratorNode;
import com.boubei.tss.portal.engine.model.LayoutNode;
import com.boubei.tss.portal.engine.model.PageNode;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.portal.entity.ReleaseConfig;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.entity.Theme;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.RoleAction;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.FileHelper;

/**
 * 门户结构相关模块的单元测试。
 */
public class PortalModuleTest extends AbstractTest4Portal {
    
    @Autowired PortalAction portalAction;
    @Autowired ComponentAction elementAction;
    @Autowired NavigatorAction menuAction;
    @Autowired IPortalDao portalDao;
    
    Long portalId;
    Structure root;
    Structure page1;
    Structure page2;
    Structure section1;
    Structure section11;
    Structure portlet;
    Theme defaultTheme;
    
    public void init() {
    	 super.init();
    	 
         Long parentId = PortalConstants.ROOT_ID;
         
         // 新建portal
         defaultTheme = new Theme();
         defaultTheme.setName("默认主题");
         
         root = new Structure();
         root.setParentId(parentId);
         root.setType(Structure.TYPE_PORTAL);
         root.setName("Jon的门户-1" + System.currentTimeMillis());
         root.setSupplement("<page><property><name>Jon的门户</name><description><![CDATA[]]></description></property><script><file><![CDATA[123.js]]></file><code><![CDATA[]]></code></script><style><file><![CDATA[123.css]]></file><code><![CDATA[]]></code></style></page>");
         root.setDescription("测试门户");
         root.setTheme(defaultTheme);
         root.setCode(System.currentTimeMillis() + "");
         portalAction.save(response, root); // create portal root
         
         portalId = root.getPortalId();
         
         // 新建页面、版面
         page1 = createPageOrSection(root, "页面一", "page1", Structure.TYPE_PAGE);
         page2 = createPageOrSection(root, "页面二", "page2", Structure.TYPE_PAGE);
         section1 = createPageOrSection(page1, "版面一", "section1", Structure.TYPE_SECTION);
         section11 = createPageOrSection(section1, "子版面一", "section11", Structure.TYPE_SECTION);
         
         Structure section2 = createPageOrSection(page2, "版面二", "section2", Structure.TYPE_SECTION);
         
         Component _portlet = createTestPortlet();
         portlet = createPortletInstance(section11, "portletInstance1", "portletInstance1", _portlet);
         Structure temp = createPortletInstance(section2, "portletInstance2", "portletInstance2", _portlet);
         
         portalAction.getStructureInfo(response, request, root.getId());
         portalAction.getStructureInfo(response, request, page1.getId());
         portalAction.getStructureInfo(response, request, temp.getId());
         
         request.addParameter("parentId", page1.getId().toString());
         portalAction.getStructureInfo(response, request, PortalAction.DEFAULT_NEW_ID);
         
         try {
         	componentService.deleteComponent(_portlet.getId());
         	Assert.fail("should throw exception but didn't.");
         } catch (Exception e) {
         	Assert.assertTrue("结点在使用中，删除失败！", true);
         }
    }
    
    @Test
    public void testPortalTheme() {
        portalAction.getThemes4Tree(response, portalId);
        
        List<?> themeList = portalService.getThemesByPortal(portalId);
        assertEquals(1, themeList.size());
        Theme defaultTheme = (Theme) themeList.get(0);
        Long defaultThemeId = defaultTheme.getId();
        portalAction.saveThemeAs(response, defaultThemeId, "我的主题");
        
        themeList = portalService.getThemesByPortal(portalId);
        assertEquals(2, themeList.size());
        Theme newTheme = (Theme) themeList.get(1);
 
        portalAction.renameTheme(response, newTheme.getId(), "Jon的主题");
        portalAction.specifyDefaultTheme(response, defaultThemeId);
        try {
        	portalAction.removeTheme(response, newTheme.getId());
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("该主题为门户的默认主题或者当前主题，正在使用中，删除失败！", true);
        }
    }
    
    @Test
    public void testPortalRelease() {
        portalAction.getActivePortals4Tree(response);
        portalAction.getActivePagesByPortal4Tree(response, portalId);
        portalAction.getThemesByPortal(response, portalId);
        
        List<?> themeList = portalService.getThemesByPortal(portalId);
        assertTrue(themeList.size() > 0);
       
        // test get init template
        portalAction.getReleaseConfig(response, BaseActionSupport.DEFAULT_NEW_ID);
        
        ReleaseConfig rconfig = new ReleaseConfig();
        rconfig.setName("门户发布配置");
        rconfig.setPortal(root);
        rconfig.setPage(page1);
        rconfig.setTheme((Theme) themeList.get(0));
        rconfig.setVisitUrl("default.portal");
        rconfig.setRemark("~~~~~~~~~~~~~~~~");
        portalAction.saveReleaseConfig(response, rconfig); // create
        
        rconfig.setVisitUrl("default");
        portalAction.saveReleaseConfig(response, rconfig); // update
        
        ReleaseConfig rconfig2 = new ReleaseConfig();
        rconfig2.setId(null);
        rconfig2.setName("门户发布配置-2");
        rconfig2.setPortal(root);
        rconfig2.setPage(page1);
        rconfig2.setTheme((Theme) themeList.get(0));
        rconfig2.setVisitUrl("default.portal");
        try {
        	 portalAction.saveReleaseConfig(response, rconfig2); // create
        	 Assert.fail("should throw exception but didn't.");
		} catch(Exception e) {
			Assert.assertTrue("发布地址已经存在", true);
			
			rconfig2.setVisitUrl("default2.portal");
			portalAction.saveReleaseConfig(response, rconfig2);
		}
        Assert.assertNotNull(rconfig2.getTheme());
        Assert.assertNotNull(rconfig2.getPage());
        Assert.assertNotNull(rconfig2.getPK());
        
        try {
        	rconfig2.setVisitUrl("default");
       	 	portalAction.saveReleaseConfig(response, rconfig2); // update
       	 	Assert.fail("should throw exception but didn't.");
		} catch(Exception e) {
			Assert.assertTrue("发布地址已经存在", true);
		}
        
        ReleaseConfig rconfig3 = new ReleaseConfig();
        rconfig3.setPortal(root);
        rconfig3.getAttributes4XForm();
        
        ReleaseConfig rconfig4 = new ReleaseConfig();
        rconfig4.setName("BI门户");
        rconfig4.setPortal(root);
        rconfig4.setVisitUrl("bi.portal");
        portalAction.saveReleaseConfig(response, rconfig4); // create
        
        // 按发布地址读取发布配置信息
        rconfig = portalService.getReleaseConfig("default.portal");
        Assert.assertNotNull(rconfig);
        
		try {
			portalService.getReleaseConfig("index.portal");
			Assert.fail("should throw exception but didn't.");
		} catch(Exception e) {
			Assert.assertTrue("发布地址不存在", true);
		}
        
        portalAction.getReleaseConfig(response, rconfig.getId()); // load exsited config
        portalAction.getAllReleaseConfigs4Tree(response);
        
        // test PortalDispatcher
        PortalDispatcher dispatcher = new PortalDispatcher();
        try {
        	request.setRequestURI("http://localhost:8088/tss/default.portal");
        	dispatcher.doPost(request, new MockHttpServletResponse());
        	
        	request.setRequestURI("http://localhost:8088/tss/bi.portal");
        	dispatcher.doPost(request, new MockHttpServletResponse());
        	
        	request.setRequestURI("http://localhost:8088/tss/index.portal");
        	dispatcher.doPost(request, new MockHttpServletResponse());
        	
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
        
        // 先注销登录
        logout();
        try {
        	request.setRequestURI("http://localhost:8088/tss/default.portal");
        	dispatcher.doPost(request, new MockHttpServletResponse());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
        
        // 重新登录
        login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
        rconfig.setPage(null);
        rconfig.setTheme(null);
        try {
        	request.setRequestURI("http://localhost:8088/tss/default.portal");
        	dispatcher.doPost(request, new MockHttpServletResponse());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
        
        portalAction.removeReleaseConfig(response, rconfig.getId());
    }
 
    @Test
    public void testPortalCRUD() {
    	Long structureId = BaseActionSupport.DEFAULT_NEW_ID;
        Long parentId = PortalConstants.ROOT_ID;
    	
    	request.addParameter("type", Structure.TYPE_PORTAL + "");
        request.addParameter("parentId", parentId + "");
        portalAction.getStructureInfo(response, request, structureId); // test get init template
        
        List<?> list = portalService.getAllStructures();
        assertTrue(list.size() >= 7);

        structureId = root.getId();
        request.addParameter("type", Structure.TYPE_PORTAL + "");
        request.addParameter("parentId", PortalConstants.ROOT_ID + "");
        portalAction.getStructureInfo(response, request, structureId);
        
        portalAction.save(response, root); // update portal root
  
        Structure root2 = new Structure();
        BeanUtil.copy(root2, root, "id,name".split(","));
        root2.setName("root2");
        FileHelper.createDir( Structure.getPortalResourceFileDir(root2.getCode()).getPath() );
        portalAction.save(response, root2); // create portal root
        
        Structure newps = new Structure();
        newps.setName("root2_page1");
        newps.setCode("root2_page1");
        newps.setType(Structure.TYPE_PAGE);
        newps.setPortalId(root2.getPortalId());
        newps.setParentId(root2.getId());
        newps.setDecorator(defaultDecorator);
        newps.setDefiner(defaultLayout);
        portalAction.save(response, newps);
        
        portalAction.getAllStructures4Tree(response);
        
        // 删除新建的门户
        portalAction.delete(response, root.getId());
        
        list = portalService.getActivePortals();
        assertFalse(list.contains(root));
        
		// other
        PortalNode portalNode = new PortalNode(root2);
        PageNode pageNode = new PageNode(newps, portalNode);
		DecoratorNode dnode = new DecoratorNode(defaultDecorator, pageNode, null);
		new DecoratorMacrocodeContainer("test", dnode , null);
		LayoutNode lnode = new LayoutNode(defaultLayout, pageNode, null);
		new LayoutMacrocodeContainer("test", lnode  , null);
        
        assertTrue(_TestUtil.printLogs(logService) > 0);
    }
    
    @Test
    public void testPortalMenu() {
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_SECTION);
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_PORTLET_INSTANCE);
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_PAGE);
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_PORTAL);
    }
    
    @Autowired RoleAction action;
    @Test
    public void testPortalPermission() {
    	// 获取节点操作权限
        portalAction.getOperationsByResource(response, root.getId());
        portalAction.getOperationsByResource(response, 0L);
        
        String permissions = page1.getId() + "|1111101, ,"; // 半勾
        request.addParameter("applicationId", "tss");
        request.addParameter("resourceType", PortalConstants.PORTAL_RESOURCE_TYPE);
        request.addParameter("permissions", permissions);
        action.savePermission(response, request, "2", 1, UMConstants.ANONYMOUS_ROLE_ID);
        
        permissions = UMConstants.ANONYMOUS_ROLE_ID + "|1111110"; // 半勾
        request.removeParameter("permissions");
        request.addParameter("permissions", permissions);
        action.savePermission(response, request, "2", 0, page1.getId());
    }
    
    @Test
    public void testPortalSort() {
    	// 测试排序
        portalAction.sort(response, page1.getId(), page2.getId(), 1);
        portalAction.getAllStructures4Tree(response);
    }
    
    @Test
    public void testPortalMove() {
    	// 测试移动
        portalAction.move(response, portlet.getId(), portlet.getParentId());
        portalAction.getAllStructures4Tree(response);
    }
    
    @Test
    public void testPortalStartAndStop() {
    	// 测试停用启用
        portalAction.disable(response, root.getId(), ParamConstants.TRUE);
        
        portalAction.disable(response, page1.getId(), ParamConstants.FALSE);
        portalAction.disable(response, root.getId(), ParamConstants.FALSE);
    }
}
