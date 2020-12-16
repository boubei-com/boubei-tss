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

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.portal.AbstractTest4Portal;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.action.ComponentAction;
import com.boubei.tss.portal.action.NavigatorAction;
import com.boubei.tss.portal.action.PortalAction;
import com.boubei.tss.portal.engine.HTMLGenerator;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.entity.Theme;

import junit.framework.Assert;

/**
 * 测试门户动态浏览
 */
public class PortalBrowseTest extends AbstractTest4Portal {
    
    @Autowired PortalAction portalAction;
    @Autowired ComponentAction elementAction;
    @Autowired NavigatorAction menuAction;
    
    Long portalId;
    Structure root;
    Structure page1;
    Structure page2;
    Structure section1;
    Structure section11;
    Structure portletInstance4;
    Theme defaultTheme;
 
    public void init() {
         super.init();
         
         // 门户浏览时，freemarker解析时需要用到request里的参数
         MockHttpServletRequest request2 = new MockHttpServletRequest();
         request2.setSession(request.getSession());
         Context.initRequestContext(request2); 
         
         Long parentId = PortalConstants.ROOT_ID;
         
         // 新建portal
         defaultTheme = new Theme();
         defaultTheme.setName("默认主题");
         
         root = new Structure();
         root.setParentId(parentId);
         root.setType(Structure.TYPE_PORTAL);
         root.setName("Jon的门户-1" + System.currentTimeMillis());
         root.setSupplement("<page><property><name>Jon的门户</name><description><![CDATA[]]></description></property><script><file><![CDATA[]]></file><code><![CDATA[]]></code></script><style><file><![CDATA[]]></file><code><![CDATA[]]></code></style></page>");
         root.setDescription("测试门户");
         root.setTheme(defaultTheme);
         root.setCode(System.currentTimeMillis() + "");
         portalAction.save(response, root); // create portal root
         
         portalId = root.getPortalId();
         
         defaultTheme = (Theme) portalService.getThemesByPortal(portalId).get(0);
         Assert.assertFalse( defaultTheme.equals(null) );
         Assert.assertFalse( defaultTheme.equals(root) );
         
 		try {
 			new HTMLGenerator( new PortalNode(root), null);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("no page is visible", true);
        }
         
         // 新建页面、版面
         page1 = createPageOrSection(root, "页面一", "page1", Structure.TYPE_PAGE);
         page2 = createPageOrSection(root, "页面二", "page2", Structure.TYPE_PAGE);
         section1 = createPageOrSection(page1, "版面一", "section1", Structure.TYPE_SECTION);
         section11 = createPageOrSection(section1, "子版面一", "section11", Structure.TYPE_SECTION, defaultLayout2);
         createPageOrSection(section1, "子版面二", "section12", Structure.TYPE_SECTION, defaultLayout2);
         
         Structure section2 = createPageOrSection(page2, "版面二", "section2", Structure.TYPE_SECTION);
         Component portlet = createTestPortlet();
         createPortletInstance(section11, "portletInstance1", "portletInstance1", portlet);
         createPortletInstance(section11, "portletInstance0", "portletInstance0", portlet);
         createPortletInstance(section2, "portletInstance2", "portletInstance2", portlet);
         createPortletInstance(section2, "portletInstance3", "portletInstance3", portlet);
         portletInstance4 = createPortletInstance(section2, "portletInstance4", "portletInstance4", portlet);
    }
    
    @Test
    public void testPortalBrowse() {
        try {
        	request.addParameter("themeId", defaultTheme.getId() + "");
            portalAction.previewPortal(response, request, portalId);
        } 
        catch (Exception e) {
        	e.printStackTrace();
            assertFalse(e.getMessage(), true);
        }
        
        testPageBrowse();
        
        // 测试门户流量查看
        portalAction.getFlowRate(response, portalId);
    	
    	// 测试门户缓存管理
        portalAction.cacheManage(response, portalId);
        portalAction.flushCache(response, portalId, defaultTheme.getId());
    }
    
    @Test
    public void testPortalBrowse2() {
    	page2.setSupplement("<page><property><name>Jon的门户</name><description><![CDATA[]]></description></property><script><file><![CDATA[]]></file><code><![CDATA[]]></code></script><style><file><![CDATA[]]></file><code><![CDATA[]]></code></style></page>");
        request.removeParameter("pageId");
        request.addParameter("pageId", page2.getId() + "");
        portalAction.previewPortal(response, request, portalId);
    }
    
    private void testPageBrowse() {
        try {
            request.addParameter("themeId", defaultTheme.getId() + "");
            request.addParameter("pageId", page1.getId() + "");
            portalAction.previewPortal(response, request, portalId);
            
            request.removeParameter("pageId");
            request.addParameter("pageId", page2.getId() + "");
            portalAction.previewPortal(response, request, portalId);
        } 
        catch (Exception e) {
        	e.printStackTrace();
            assertFalse(e.getMessage(), true);
        }
        
        // 重复访问，以出发流量记录
        for(int i = 0; i < 50; i++) {
        	portalAction.previewPortal(response, request, portalId);
        }
        
        request.addParameter("pageId", section1.getId() + "");
        portalAction.previewPortal(response, request, portalId);
        
        request.addParameter("pageId", portletInstance4.getId() + "");
        portalAction.previewPortal(response, request, portalId);
 
        // 测试门户流量查看
        portalAction.getFlowRate(response, portalId);
    	
    	// 测试门户缓存管理
        portalAction.cacheManage(response, portalId);
        portalAction.flushCache(response, portalId, defaultTheme.getId());
    }
}
