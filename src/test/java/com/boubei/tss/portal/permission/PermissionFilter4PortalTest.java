/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.permission;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss.portal.engine.model.PageNode;
import com.boubei.tss.portal.engine.model.PortalNode;
import com.boubei.tss.portal.entity.Structure;

public class PermissionFilter4PortalTest {
	
	@Test
	public void test() {
		
		PermissionFilter4Portal t = new PermissionFilter4Portal();
		
		Structure root = new Structure();
		root.setId(1L);
		root.setName("root");
		PortalNode node = new PortalNode(root);
		
		Structure page1 = new Structure();
		page1.setId(2L);
		PageNode node2 = new PageNode(page1, node);
		node.getChildren().add( node2 );
		
		Structure page2 = new Structure();
		page2.setId(3L);
		PageNode node3 = new PageNode(page2, node);
		node.getChildren().add( node3 );
		
		node.getNodesMap().put(node2.getId(), node2);
		node.getNodesMap().put(node3.getId(), node3);
		
		
		List<Long> permitedResouceIds = new ArrayList<Long>();
		permitedResouceIds.add(2L);
		
	   	try {
	   		t.doFiltrate(node, permitedResouceIds );
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.parse(EX.P_10, node.getName()), e.getMessage());
		}
		
		permitedResouceIds.add(1L);
		
		t.doFiltrate(node, permitedResouceIds );
	}

}
