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

import org.dom4j.Document;
import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.framework.web.display.grid.DefaultGridNode;
import com.boubei.tss.framework.web.display.grid.GridAttributesMap;
import com.boubei.tss.framework.web.display.grid.GridTemplet;
import com.boubei.tss.framework.web.display.grid.GridValueFilter;
import com.boubei.tss.framework.web.display.grid.SimpleGridParser;
import com.boubei.tss.util.XMLDocUtil;

public class GridTest {
	
	@Test
	public void test() {
		
		GridValueFilter filter = new GridValueFilter() {
			public Object pretreat(Object key, Object value) {
				return value;
			}
		};
		new DefaultGridNode(filter);
		
		GridTemplet gt;
    	try {
    		gt = new GridTemplet("");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("没有定义Gird模板文件的路径", true);
		}
    	
    	Document doc = XMLDocUtil.dataXml2Doc("<grid version=\"2\"></grid>");
		gt = new GridTemplet(doc );
		gt.getColumns();
		
		new  SimpleGridParser().parse(null);
		
		DefaultGridNode node = new DefaultGridNode();
		node.getAttrs().put("x1", null);
		node.getAttributes(new GridAttributesMap(new String[]{}));
		
	}

}
