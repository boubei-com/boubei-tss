/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.framework.web.display.tree.DefaultLevelTreeNode;
import com.boubei.tss.framework.web.display.tree.ILevelTreeNode;
import com.boubei.tss.framework.web.display.tree.LevelTreeParser;
import com.boubei.tss.framework.web.display.tree.TreeAttributesMap;
import com.boubei.tss.framework.web.display.tree.TreeEncoder;
import com.boubei.tss.framework.web.display.tree.TreeNodeOption;
import com.boubei.tss.framework.web.display.tree.TreeNodeOptionsEncoder;

public class TreeTest {
	
	@Test
	public void testTree() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		DefaultLevelTreeNode root = new DefaultLevelTreeNode(1L, "root");
		DefaultLevelTreeNode node1 = new DefaultLevelTreeNode(2L, root.getId(), "node1");
		DefaultLevelTreeNode node2 = new DefaultLevelTreeNode(3L, root.getId(), "node2");
		DefaultLevelTreeNode node3 = new DefaultLevelTreeNode(4L, root.getId(), "node3");
		DefaultLevelTreeNode node4 = new DefaultLevelTreeNode(5L, node2.getId(), "node4", "node2");
		
		Assert.assertEquals(node1, node1);
		
		List<ILevelTreeNode> list = new ArrayList<ILevelTreeNode>();
		list.add(root);
		list.add(node1);
		list.add(node2);
		list.add(node3);
		list.add(node4);
		
		TreeEncoder treeEncoder = new TreeEncoder(list, new LevelTreeParser());
		
		TreeNodeOptionsEncoder optionsEncoder = new TreeNodeOptionsEncoder();
		TreeNodeOption option = new TreeNodeOption();
		option.setId("1");
		option.setDependId("2");
		option.setText("编辑权限");
		option.toXml();
		optionsEncoder.add(option);
		optionsEncoder.toXml();
		treeEncoder.setOptionsEncoder(optionsEncoder );
		treeEncoder.setRootNodeName("ROOT");
		treeEncoder.setRootCanSelect(false);
		
		try {
			XmlPrintWriter writer = new XmlPrintWriter(response.getWriter());
			optionsEncoder.print(writer);
			treeEncoder.print(writer);
		} 
		catch (IOException e) {
			Assert.fail();
		}
		
		TreeAttributesMap attributes = root.getAttributes();
		Assert.assertEquals(root.getId(), attributes.get("id"));
		
		attributes.put("status", 1);
		attributes.putAll(attributes);
		
		Assert.assertEquals(4, attributes.getAttributes().size());
		Assert.assertEquals(4, attributes.size());
		Assert.assertTrue(attributes.containsKey("id"));
		Assert.assertTrue(attributes.containsValue("root"));
		
		attributes.remove("id");
		
		Assert.assertEquals(3, attributes.values().size());
		Assert.assertEquals(3, attributes.keySet().size());
		
		attributes.clear();
		Assert.assertTrue(attributes.isEmpty());
	}

}
