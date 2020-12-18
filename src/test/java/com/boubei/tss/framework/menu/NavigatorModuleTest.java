/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.menu;

import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.context.Theme;

import com.boubei.tss._TestUtil;
import com.boubei.tss.dm.ext.Export;
import com.boubei.tss.dm.ext.ImportMenu;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.menu.INavigatorService;
import com.boubei.tss.modules.menu.MenuDTO;
import com.boubei.tss.modules.menu.Navigator;
import com.boubei.tss.modules.menu.NavigatorAction;
import com.boubei.tss.modules.menu.NavigatorResource;
import com.boubei.tss.modules.menu.PortalConstants;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.portal.AbstractTest4Portal;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.entity.permission.PortalResource;
import com.boubei.tss.um.UMConstants;

/**
 * 导航栏模块的单元测试。
 */
public class NavigatorModuleTest extends AbstractTest4Portal {
    
    @Autowired NavigatorAction menuAction;
    @Autowired INavigatorService menuService;
    
    @Autowired Export export;
 
    @Test
    public void testMenuModule() {
        
        List<?> list = menuService.getAllNavigator();
        assertTrue(list.size() == 0);
        
        Navigator rootMenu = (Navigator) list.get(0);
        Long rootMenuId = rootMenu.getId();
        
        request.addParameter("parentId", rootMenuId + "");
        request.addParameter("type", Navigator.TYPE_MENU + "");
        menuAction.getNavigatorInfo(response, request, BaseActionSupport.DEFAULT_NEW_ID);
        
        menuAction.getNavigatorInfo(response, request, rootMenuId);
 
        // 创建各种类型的菜单项
        Navigator menu1 = new Navigator();
        menu1.setType(Navigator.TYPE_MENU_ITEM_3);
        menu1.setName("首页");
        menu1.setParentId(rootMenuId);
        menu1.setPortalId(portalId);
        menu1.setContent(root);
        menu1.setTarget("_blank");
        menu1.setDescription("unit test");
        menu1.setIcon("x.png");
        menuAction.save(response, menu1);
        menuAction.getNavigatorInfo(response, request, menu1.getId());
        Assert.assertNotNull(menu1.getContent());
        
        Navigator menu2 = new Navigator();
        menu2.setType(Navigator.TYPE_MENU_ITEM_7);
        menu2.setName("机构职责");
        menu2.setParentId(rootMenuId);
        menu2.setPortalId(portalId);
        menu2.setUrl("${common.articleListUrl}&channelId=38");
        menuService.saveNavigator(menu2);
        
        Navigator menu2_1 = new Navigator();
        menu2_1.setType(Navigator.TYPE_MENU_ITEM_6);
        menu2_1.setName("授权管理");
        menu2_1.setParentId(menu2.getId());
        menu2_1.setPortalId(portalId);
        menu2_1.setMethodName("jumpTo");
        menu2_1.setParams("appCode:\'UMS\',redirect:\'http://${PT_ip}/ums/redirect.html\',url:\'ums/permission.htm\'");
        menu2_1.setContent(root);
        menuService.saveNavigator(menu2_1);
        
        Navigator menu2_2 = new Navigator();
        menu2_2.setType(Navigator.TYPE_MENU_ITEM_4);
        menu2_2.setName("Google");
        menu2_2.setParentId(menu2.getId());
        menu2_2.setPortalId(portalId);
        menu2.setUrl("www.google.com");
        menuService.saveNavigator(menu2_2);
        
        Navigator menu3 = new Navigator();
        menu3.setType(Navigator.TYPE_MENU_ITEM_7);
        menu3.setName("二级页面");
        menu3.setParentId(rootMenuId);
        menu3.setPortalId(portalId);
        menu3.setContent(root);
        menuService.saveNavigator(menu3);
        
        Navigator menu4 = new Navigator();
        menu4.setType(Navigator.TYPE_MENU_ITEM_2);
        menu4.setName("按钮4");
        menu4.setCode("button1");
        menu4.setParentId(rootMenuId);
        menuService.saveNavigator(menu4);
        
        Assert.assertEquals(menu4.getCode(), menuService.getNavigator(menu4.getId()).getCode());
        
        List<?> sList = generalSearcher.searchResource("Navigator", "授权管理");
		Assert.assertEquals(1, sList.size() );
		Assert.assertEquals("测试门户4MenuTest > 机构职责", ((Object[])sList.get(0))[2] );
        
        // 测试停用启用
        for(int i = 0; i < 2; i++) {
	        menuAction.disable(response, rootMenuId, ParamConstants.TRUE);  // 停用这个枝
	        menuAction.disable(response, menu2_2.getId(), ParamConstants.FALSE); // 启用当前节点及其所有父节点
	        menuAction.disable(response, menu3.getId(), ParamConstants.FALSE);
        }
        
        // 排序、移动
        menuAction.sort(response, menu2.getId(), menu3.getId(), 1);

        menuAction.moveTo(response, menu3.getId(), menu1.getId());
        try{
        	menuAction.moveTo(response, menu3.getId(), menu3.getId());
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("节点不能移动到自身节点下", true);
	    }
        
        menuAction.getPortalNavigatorTree(response, menu2.getId()); // 移动的时候用到
        
        Navigator menu5 = new Navigator();
        menu5.setType(Navigator.TYPE_MENU);
        menu5.setName("BI菜单");
        menu5.setParentId(PortalConstants.ROOT_ID);
        menuAction.save(response, menu5);
        menuAction.getPortalNavigatorTree(response, menu5.getId());  // no portalId
        
        // 查询
        menuAction.getAllNavigator4Tree(response);
        menuAction.getOperations(response, rootMenuId);
        
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_SECTION);
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_PORTLET_INSTANCE);
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_PAGE);
        menuAction.getStructuresByPortal(response, portalId, Structure.TYPE_PORTAL);
        
        // 生成菜单XML格式
        menuAction.getNavigatorXML(response, rootMenuId);
        
        // 测试生成Json格式
        List<MenuDTO> mList = menuAction.getNavigatorJson(rootMenuId);
        Assert.assertTrue(mList.size() > 0);
        
        mList = api.menuJSON(rootMenuId);
        Assert.assertTrue(mList.size() > 0);
        
        request.addParameter("name", rootMenu.getName());
        mList = api.menuJSON(request);
        Assert.assertTrue(mList.size() > 0);
        
        // 匿名用户读取菜单（缓存）
        login(UMConstants.ANONYMOUS_USER_ID, "ANONYMOUS");
        menuAction.getNavigatorXML(response, rootMenuId);
        menuAction.getNavigatorXML(response, rootMenuId); // 访问两边以测试是否缓存成功
        
        menuAction.flushCache(response, rootMenuId);
        
        
        login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
        
        // 测试导出导入
        response = new MockHttpServletResponse();
        export.exportMenu(response, rootMenuId);
        String exportPath = export.exportMenu(null, rootMenuId);
        
        AfterUpload servlet = new ImportMenu();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    EasyMock.expect(mockRequest.getParameter("groupId")).andReturn(rootMenuId.toString());
	    
	    try {
	        mocksControl.replay(); 
	        servlet.processUploadFile(mockRequest, exportPath, null);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
        
	    // 删除
	    response = new MockHttpServletResponse();
        menuAction.delete(response, rootMenuId);
        
        mocksControl =  EasyMock.createControl();
	    mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    EasyMock.expect(mockRequest.getParameter("groupId")).andReturn(root.getId().toString());
	    
	    try {
	        mocksControl.replay(); 
	        servlet.processUploadFile(mockRequest, exportPath, null);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
        
        portalAction.delete(response, root.getId());
        
        assertTrue(_TestUtil.printLogs(logService) > 0);
        
        Assert.assertEquals(PortalConstants.NAVIGATOR_RESOURCE_TYPE, menu1.getResourceType());
        Assert.assertEquals(PortalConstants.NAVIGATOR_RESOURCE_TYPE, new NavigatorResource().getResourceType());
        
        Assert.assertEquals(PortalConstants.PORTAL_RESOURCE_TYPE, root.getResourceType());
        Assert.assertEquals(PortalConstants.PORTAL_RESOURCE_TYPE, new PortalResource().getResourceType());
    }
    
}
