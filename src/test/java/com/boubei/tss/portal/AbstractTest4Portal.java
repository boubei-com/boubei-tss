/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4TSS;
import com.boubei.tss.PX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.portal.helper.CreateComponent;
import com.boubei.tss.portal.helper.MovePortalFile;
import com.boubei.tss.portal.service.IComponentService;
import com.boubei.tss.portal.service.IPortalService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;
import com.boubei.tss.util.XMLDocUtil;

public abstract class AbstractTest4Portal extends AbstractTest4TSS { 
 
    @Autowired protected IComponentService componentService;
    @Autowired protected IPortalService portalService;
    
    protected static String MODEL_PORTAL_DIR;
    protected static String MODEL_LAYOUT_DIR;
    protected static String MODEL_DECORATOR_DIR;
    protected static String MODEL_PORTLET_DIR;
  
    protected static Component layoutGroup;
    protected static Component defaultLayout;
    protected static Long defaultLayoutId;
    protected static Component defaultLayout2;
    protected static Long defaultLayoutId2;
    
    protected static Component decoratorGroup;
    protected static Component defaultDecorator;
    protected static Long defaultDecoratorId;
    
    protected void init() {
    	super.init();
        
        /* 初始化默认的修饰器，布局器 */
		initializeDefaultElement();
		
		if(paramService.getParam(PX.USER_DEFINED_PARAMS) == null) {
        	Param cp = ParamManager.addComboParam(ParamConstants.DEFAULT_PARENT_ID, PX.USER_DEFINED_PARAMS, "userdefinedParams");
            ParamManager.addParamItem(cp.getId(), "TSS-BI", "SysName", ParamConstants.COMBO_PARAM_MODE);
        }
    }
    
    /**
     * 初始化默认的修饰器，布局器
     */
    private void initializeDefaultElement() {
    	if(componentService.getAllComponentsAndGroups().size() >= 4) return;
    	
        // 初始化Portal测试的组件模型存放目录（model目录）及freemarker文件的目录
        String portalTargetPath = _TestUtil.getProjectDir() + "/target";
        
        MODEL_PORTAL_DIR = FileHelper.createDir(portalTargetPath + "/portal/model/portal").getPath();
        MODEL_PORTLET_DIR = FileHelper.createDir(portalTargetPath + "/portal/model/portlet").getPath();
        MODEL_LAYOUT_DIR = FileHelper.createDir(portalTargetPath + "/portal/model/layout").getPath();
        MODEL_DECORATOR_DIR = FileHelper.createDir(portalTargetPath + "/portal/model/decorator").getPath();
        
        File freemarkerDir = FileHelper.createDir(portalTargetPath + "/freemarker");
        FileHelper.writeFile(new File(freemarkerDir + "/common.ftl"), "");
        
        layoutGroup = new Component();
        layoutGroup.setName("布局器组");
        layoutGroup.setType(Component.LAYOUT_TYPE);
        layoutGroup.setIsGroup(true);
        layoutGroup.setParentId(PortalConstants.ROOT_ID);   
        layoutGroup = componentService.saveComponent(layoutGroup);
        
        defaultLayout = new Component();
        defaultLayout.setIsDefault(ParamConstants.TRUE);
        defaultLayout.setParentId(layoutGroup.getId());   
        Document document = XMLDocUtil.createDoc("template/portal/defaultLayout.xml");
        org.dom4j.Element propertyElement = document.getRootElement().element("property");
        String layoutName = propertyElement.elementText("name");
        defaultLayout.setName(layoutName);
        defaultLayout.setPortNumber(new Integer(propertyElement.elementText("portNumber")));
        defaultLayout.setDefinition(document.asXML());
        componentService.saveComponent(defaultLayout);
        defaultLayoutId = defaultLayout.getId();
        
        defaultLayout2 = new Component();
        BeanUtil.copy(defaultLayout2, defaultLayout, "id,pk".split(","));
        defaultLayout2.setName("L2");
        defaultLayout2.setPortNumber(1);
        defaultLayout2.setIsDefault(ParamConstants.FALSE);
        componentService.saveComponent(defaultLayout2);
        defaultLayoutId2 = defaultLayout2.getId();
        
        decoratorGroup = new Component();
        decoratorGroup.setName("修饰器组");
        decoratorGroup.setIsGroup(true);
        decoratorGroup.setType(Component.DECORATOR_TYPE);
        decoratorGroup.setParentId(PortalConstants.ROOT_ID);  
        decoratorGroup = componentService.saveComponent(decoratorGroup);
        
        defaultDecorator = new Component();
        defaultDecorator.setIsDefault(ParamConstants.TRUE);
        defaultDecorator.setParentId(decoratorGroup.getId());
        
        document = XMLDocUtil.createDoc("template/portal/defaultDecorator.xml");
        propertyElement = document.getRootElement().element("property");
        String decoratorName = propertyElement.elementText("name");
        defaultDecorator.setName(decoratorName);
        defaultDecorator.setDefinition(document.asXML());
        componentService.saveComponent(defaultDecorator);
        defaultDecoratorId = defaultDecorator.getId();
    }
    
    protected void importComponent(Long groupId, String filePath) {
    	AfterUpload servlet = new CreateComponent();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    EasyMock.expect(mockRequest.getParameter("groupId")).andReturn(groupId.toString());
	    
	    try {
	        mocksControl.replay(); 
	        servlet.processUploadFile(mockRequest, filePath, null);
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
    }
    
    protected void uploadFile(String contextPath, File file) {
    	AfterUpload servlet = new MovePortalFile();
    	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    EasyMock.expect(mockRequest.getParameter("contextPath")).andReturn(contextPath);
	    
	    try {
	    	mocksControl.replay(); 
	        servlet.processUploadFile(mockRequest, file.getPath(), null);
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
    }
    
    protected Structure createPageOrSection(Structure parent, String name, String code, int type, Component...layout) {
        Structure newps = new Structure();
        newps.setName(name);
        newps.setCode(code);
        newps.setType(type);
        
        if(type == 1) {
            newps.setSupplement("<page><property><name>Jon的门户</name><description><![CDATA[]]></description></property>" +
            		"<script><file><![CDATA[1.js,2.js]]></file><code><![CDATA[]]></code></script>" +
            		"<style><file><![CDATA[1.css,2.css]]></file><code><![CDATA[]]></code></style></page>");
        }
        newps.setParameters("<params><layout>model/layout/***/paramsXForm.xml</layout><decorator>model/decorator/***/paramsXForm.xml</decorator></params>");
        
        newps.setPortalId(parent.getPortalId());
        newps.setParentId(parent.getId());
        newps.setDecorator(defaultDecorator);
        newps.setDefiner(layout.length == 0 ? defaultLayout : layout[0]);
        newps = portalService.createStructure(newps);
        
        return newps;
    }
    
    protected Structure createPortletInstance(Structure parent, String name, String code, Component portlet) {
        Structure newps = new Structure();
        newps.setName(name);
        newps.setCode(code);
        newps.setType(Structure.TYPE_PORTLET_INSTANCE);
 
        newps.setParameters("<params><portlet>model/portlet/***/paramsXForm.xml</portlet><decorator>model/decorator/***/paramsXForm.xml</decorator></params>");
        
        newps.setPortalId(parent.getPortalId());
        newps.setParentId(parent.getId());
        newps.setDecorator(defaultDecorator);
        newps.setDefiner(portlet);
        newps = portalService.createStructure(newps);
        
        return newps;
    }
    
    protected Component createTestPortlet() {
    	List<?> list = permissionHelper.getEntities("from Component o where o.type = ? and o.isGroup = ? order by o.decode", 
                Component.PORTLET_TYPE, false);
    	if( list != null && list.size() > 0 ) {
    		return (Component) list.get(list.size() - 1);
    	}
    	
        Component group = new Component();
        group.setName("测试Portlet组" + System.currentTimeMillis());
        group.setType(Component.PORTLET_TYPE);
        group.setParentId(PortalConstants.ROOT_ID);   
        group = componentService.saveComponent(group);
        
        String file = URLUtil.getResourceFileUrl("testdata/DemoPortlet.zip").getPath();
        importComponent(group.getId(), file);
        
        list = permissionHelper.getEntities("from Component o where o.type = ? and o.isGroup = ? order by o.decode", 
                Component.PORTLET_TYPE, false);
        
        assertTrue(list.size() >= 1);
        return (Component) list.get(list.size() - 1);
    }
}
