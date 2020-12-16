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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.mvc.BaseActionSupport;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.portal.AbstractTest4Portal;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.action.ComponentAction;
import com.boubei.tss.portal.dao.IComponentDao;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

import junit.framework.Assert;

/**
 * Portlet相关模块的单元测试。
 */
public class ComponentModuleTest extends AbstractTest4Portal {
    
    @Autowired ComponentAction componentAction;
    
    @Autowired IComponentDao componentDao;
 
    @Test
    public void testComponentGroupFunctions() {
        Component group1 = new Component();
        group1.setName("测试布局器组1");
        group1.setIsGroup(true);
        group1.setType(Component.LAYOUT_TYPE);
        group1.setParentId(layoutGroup.getId());   
        componentAction.save(response, group1);
        
        componentAction.renameGroup(response, group1.getId(), "测试布局器组-1");
        
        Long groupId = group1.getId();
        assertNotNull(groupId);
        
        Component group2 = new Component();
        group2.setName("测试布局器组2");
        group2.setIsGroup(true);
        group2.setType(Component.LAYOUT_TYPE);
        group2.setParentId(layoutGroup.getId());   
        componentAction.save(response, group2);
 
        request.addParameter("type", Component.LAYOUT_TYPE + "");
        componentAction.getComponentInfo(response, BaseActionSupport.DEFAULT_NEW_ID, group1.getId());
        
        componentAction.sort(Context.getResponse(), groupId, group2.getId(), -1);
        
        String hql = "from Component o where o.isGroup = 1 and o.type=? order by o.decode";
        List<?> groups = permissionHelper.getEntities(hql, Component.LAYOUT_TYPE);
        assertTrue(groups.size() >= 3);
 
        componentAction.getAllComponents4Tree(response);
        
        for(Object temp : groups) {
            Component group = (Component)temp;
            if( !layoutGroup.getId().equals(group.getId()) ){
                componentAction.delete(response, group.getId());
            }
        }
        
        Assert.assertNotNull(componentDao.getDefaultDecorator());
        Assert.assertNotNull(componentDao.getDefaultLayout());
        
        Assert.assertTrue(componentDao.getDefaultDecorator().isDecorator());
    }

    @Test
    public void testDecoratorFunctions() {
    	Component group1 = new Component();
        group1.setName("测试修饰器组1");
        group1.setIsGroup(true);
        group1.setType(Component.DECORATOR_TYPE);
        group1.setParentId(decoratorGroup.getId());   
        componentAction.save(response, group1);
        
        Long groupId = group1.getId();
        componentAction.getComponentInfo(response, BaseActionSupport.DEFAULT_NEW_ID, groupId);
        
		String file = URLUtil.getResourceFileUrl("testdata/DemoDecorator.zip").getPath();
		super.importComponent(groupId, file);

		List<?> list = componentService.getEnabledComponentsAndGroups(Component.DECORATOR_TYPE);
		assertTrue(list.size() >= 2);
		Component decorator1 = (Component) list.get(list.size() - 1);
		Long id = decorator1.getId();
		
		componentAction.exportComponent(response, id);
		componentAction.getComponentInfo(response, id, groupId);
		
		try{
			componentAction.getComponentInfo(response, -99L, groupId);
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("组件不存在，或已经被删除，修改失败", true);
	    }

		Component decorator2 = new Component();

		BeanUtil.copy(decorator2, decorator1, "id,code".split(","));
		decorator2.setName("copy_" + decorator1.getName());
		componentAction.save(response, decorator2);
		componentAction.save(response, decorator2); // update

		for (int i = 0; i < 5; i++) {
			componentAction.disable(response, id, ParamConstants.TRUE);
			componentAction.disable(response, id, ParamConstants.FALSE);
		}

		componentAction.sort(Context.getResponse(), id, id + 1, 1);
		componentAction.moveTo(response, id, group1.getId());
		
		try {
			componentAction.moveTo(response, id, id + 1);
			Assert.fail("should throw exception but didn't.");
		} catch(Exception e) {
		}

		componentAction.getEnabledComponents4Tree(response, Component.DECORATOR_TYPE);

		componentAction.getDefaultParams4Xml(response, id);
		
		request.addParameter("configXML", " bgColor=red \n menuId=12");
		componentAction.saveComponentParamsConfig(response, request, id);

		componentAction.delete(response, id);

		componentAction.delete(response, groupId);

		assertTrue(_TestUtil.printLogs(logService) > 0);
	}
    
    @Test
    public void testLayoutFunctions() {
        Component group1 = new Component();
        group1.setName("测试布局组1");
        group1.setIsGroup(true);
        group1.setType(Component.LAYOUT_TYPE);
        group1.setParentId(layoutGroup.getId());   
        componentAction.save(response, group1);
        
        Long groupId = group1.getId();
        
        String file = URLUtil.getResourceFileUrl("testdata/DemoLayout.zip").getPath();
        super.importComponent(groupId, file);
        
        List<?> list = componentService.getEnabledComponentsAndGroups(Component.LAYOUT_TYPE);
        assertTrue(list.size() >= 2);
        Component layout1 = (Component) list.get(list.size() - 1);
        Long id = layout1.getId();
        
        request.addParameter("paramsItem", "tableWidth=1000 \n column1Width=300 \n column1Width=700");
        componentAction.getComponentParamsConfig(response, request, id);
        
        String configFilePath = URLUtil.getWebFileUrl(layout1.getResourcePath() + "/" + Component.PARAM_FILE).getFile();
        FileHelper.deleteFile(new File(configFilePath));
        componentAction.getComponentParamsConfig(response, request, id);
        
        try {
            componentAction.previewComponent(response, id);
        } catch (Exception e) {
            assertFalse("预览组件出错" + e.getMessage(), true);
        }
        
        componentAction.getDefaultParams4Xml(response, id);
        
        request.addParameter("configXML", "");
		componentAction.saveComponentParamsConfig(response, request, id);
		
		componentAction.delete(response, id);
		
		try {
			componentAction.delete(response, defaultLayoutId);
		} catch(Exception e) {
			Assert.assertTrue("删除默认的布局器失败", true);
		}
		
		try {
			componentAction.disable(response, defaultLayoutId, ParamConstants.TRUE);
		} catch(Exception e) {
			Assert.assertTrue("停用默认的布局器失败", true);
		}
    }

    @Test
    public void testPortletFunctions() {
        Component group1 = new Component();
        group1.setName("测试portlet组1");
        group1.setIsGroup(true);
        group1.setType(Component.PORTLET_TYPE);
        group1.setParentId(PortalConstants.ROOT_ID);   
        componentAction.save(response, group1);
        
        Long groupId = group1.getId();
        
        String file = URLUtil.getResourceFileUrl("testdata/DemoPortlet.zip").getPath();
        super.importComponent(groupId, file);
        
        List<?> list = componentService.getEnabledComponentsAndGroups(Component.PORTLET_TYPE);
        assertTrue(list.size() >= 2);
        Component portlet1 = (Component) list.get(list.size() - 1);
        Long id = portlet1.getId();
        Assert.assertTrue(portlet1.isportlet());
        
        try {
            componentAction.previewComponent(response, id);
        } catch (Exception e) {
            assertFalse("预览组件出错" + e.getMessage(), true);
        }
        
        request.addParameter("configXML", "");
		componentAction.saveComponentParamsConfig(response, request, id);
		
		componentAction.delete(response, id);
    }

}

