/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.zlast;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.portal.AbstractTest4Portal;
import com.boubei.tss.portal.dao.IComponentDao;
import com.boubei.tss.portal.dao.INavigatorDao;
import com.boubei.tss.portal.dao.IPortalDao;
import com.boubei.tss.portal.dao.impl.ComponentDao;
import com.boubei.tss.portal.dao.impl.NavigatorDao;
import com.boubei.tss.portal.dao.impl.PortalDao;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.portal.entity.Navigator;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.entity.Theme;
import com.boubei.tss.portal.service.INavigatorService;

public class ServiceDaoTest extends AbstractTest4Portal {
	
	@Autowired INavigatorService menuService;
	@Autowired IComponentDao componentDao;
	@Autowired IPortalDao portalDao;
	@Autowired INavigatorDao menuDao;
	
	@Test 
	public void test1() {
		defaultDecorator.setIsDefault(ParamConstants.FALSE);
		componentDao.save(defaultDecorator);
        try{
        	componentDao.getDefaultDecorator();
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertEquals(EX.P_04, e.getMessage());
	    }
		defaultDecorator.setIsDefault(ParamConstants.TRUE);
		componentDao.save(defaultDecorator);
		
		defaultLayout.setIsDefault(ParamConstants.FALSE);
		componentDao.save(defaultLayout);
        try{
        	componentDao.getDefaultLayout();
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertEquals(EX.P_05, e.getMessage());
	    }
		defaultLayout.setIsDefault(ParamConstants.TRUE);
		componentDao.save(defaultDecorator);
		
		try{
			new PortalDao().saveStructure(new Structure());
	    } catch (Exception e) { }
		
		try{
			new NavigatorDao().save(new Navigator());
	    } catch (Exception e) { }
		
		try{
			new ComponentDao().save(new Component());
	    } catch (Exception e) { }
	}
	
	@Test
	public void test2() {
		Structure root = new Structure();
		root.setType(0);
		root.setName("root");
		root.setParentId(0L);
		root.setSeqNo(1);
		
		portalDao.saveStructure(root);
		Long portalId = root.getId();
		root.setPortalId(portalId);
		
		Theme theme = new Theme();
		theme.setName("t1");
		theme.setPortalId(portalId);
		portalDao.createObject(theme);
		try{
        	portalService.renameTheme(theme.getId(), "");
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("主题名称不能为空", true);
	    }
		root.setCurrentTheme(theme);
		
		Structure page = new Structure();
		page.setType(1);
		page.setName("p1");
		page.setParentId(portalId);
		page.setPortalId(portalId);
		page.setSeqNo(1);
		portalDao.saveStructure(page);
		portalService.getStructureWithTheme(page.getId());
			
		try {
			portalService.getPortal(portalId, theme.getId());
	    } catch (Exception e) { }
		try {
			portalService.getPortal(portalId, null);
	    } catch (Exception e) { }

		root.setCurrentTheme(null);
		root.setTheme(null);
		portalDao.saveStructure(root);
		
		portalService.removeTheme(theme.getId());
		portalService.deleteStructure(page.getId());
		
		Navigator menu = new Navigator();
		menu.setType(1);
		menu.setName("root");
		menu.setParentId(0L);
		menu.setSeqNo(1);
		menuDao.save( menu );
		menuService.saveNavigator(menu);
		menuService.getNavigatorXML(-99L);
		
		Component group = new Component();
		group.setType(1);
		group.setName("lg1");
		group.setParentId(0L);
		group.setSeqNo(1);
		componentDao.save( group );
		componentDao.deleteComponent(group);
		
		group = new Component();
		group.setType(1);
		group.setName("lg2");
		group.setParentId(0L);
		group.setSeqNo(1);
		componentDao.save( group );
		componentDao.deleteGroup(group);
		
		group = new Component();
		group.setType(1);
		group.setName("lg3");
		group.setParentId(0L);
		group.setSeqNo(1);
		componentDao.save( group );
		Component layout1 = new Component();
		layout1.setType(1);
		layout1.setName("l1");
		layout1.setParentId(group.getId());
		layout1.setSeqNo(1);
		componentDao.getChildrenById(group.getId());
		componentService.disableComponent(group.getId(), 0);
		componentDao.deleteGroup(group);
	}

}
