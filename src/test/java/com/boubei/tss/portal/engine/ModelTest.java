/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.engine;

import org.dom4j.Document;
import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss.portal.engine.model.LayoutNode;
import com.boubei.tss.portal.engine.model.PageNode;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.engine.model.PortletInstanceNode;
import com.boubei.tss.portal.engine.model.SectionNode;
import com.boubei.tss.portal.entity.Component;
import com.boubei.tss.portal.entity.Structure;
import com.boubei.tss.util.XMLDocUtil;

public class ModelTest {
	
	@Test
	public void test() {
		Structure x = new Structure();
		new PortalNode( x ).getParent();
		new SectionNode( x ).getLayoutNode();
		new PortletInstanceNode( x ).getPortletNode();
		new PortletInstanceNode( x ).addChild(null);
		new PageNode(x, null).getLayoutNode();
		new PageNode(x, null).setPortal(null);
		new PageNode(x, null).setPage(null);
		
		Component l = new Component();
		l.setType(1);
		l.setName("l1");
		l.setParentId(0L);
		l.setSeqNo(1);
		Document document = XMLDocUtil.createDoc("template/portal/defaultLayout.xml");
		l.setDefinition(document.asXML());
		LayoutNode ln = new LayoutNode(l, new PageNode(x, null), null);
		ln.getPortal();
		ln.getPage();
		ln.addChild(null);
		
		Structure root = new Structure();
		root.setName("I'm not root");
		root.setType(Structure.TYPE_PAGE);
		try {
			PortalGenerator.genPortalNode(root, null, null);
	    	Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertEquals(EX.parse(EX.P_07, root.getName()), e.getMessage());
	    }
	}

}
