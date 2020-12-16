/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Assert;
import org.junit.Test;

public class XMLDocUtilTest {
 
	@Test
    public void testMap2DataNode() {
		
		new XMLDocUtil();
		
    	Map<String, Object> map = new HashMap<String, Object>();
        map.put("tel", new Object[] { "057188889999", "13588899889" });
        map.put("email", "jinpj@g-soft.com.cn");
        map.put("age", new Integer(24));
        map.put("id", new Object[] { new Integer(23), "<![CDATA[sss]]>" });
        map.put("addr", null);

        Element node = XMLDocUtil.map2DataNode(map, "row");
        
        String dataXml = "<row><id><![CDATA[23]]></id><id><![CDATA[&lt;![CDATA[sss]]&gt;]]></id><email><![CDATA[jinpj@g-soft.com.cn]]></email><age><![CDATA[24]]></age><tel><![CDATA[057188889999]]></tel><tel><![CDATA[13588899889]]></tel></row>";
        assertEquals(dataXml, node.asXML());

        Assert.assertEquals(4, XMLDocUtil.dataNodes2Map(node).size());
        Assert.assertNotNull(XMLDocUtil.dataXml2Doc(dataXml));
		
		Assert.assertEquals(0, XMLDocUtil.dataNodes2Map(null).size());
		Assert.assertNull(XMLDocUtil.dataXml2Doc(""));
		
		try {
			XMLDocUtil.dataXml2Doc(dataXml + "<error>");
		} catch (Exception e) {
			Assert.assertTrue("由dataXml生成doc出错", true);
		}
		
		try {
			XMLDocUtil.createDoc("META-INF/notExsits.xml");
		} catch (Exception e) {
			Assert.assertTrue("定义的文件没有找到", true);
		}
		
		Document doc = XMLDocUtil.createDoc("tss/cache.xml");
		XMLDocUtil.getNodeText(node);
		Assert.assertTrue( XMLDocUtil.selectNodes(doc, "//strategy").size() > 0 );
		Assert.assertTrue( XMLDocUtil.selectNodes(doc, "//tel1234").size() == 0 );
		
		try {
			XMLDocUtil.createDoc("cpu.license");
		} catch (Exception e) {
			Assert.assertTrue("读取XML文件出错", true);
		}
		
		String licenseFile = URLUtil.getResourceFileUrl("cpu.license").getFile();
		try {
			XMLDocUtil.createDocByAbsolutePath2( licenseFile );
		} catch (Exception e) {
			Assert.assertTrue("读取XML文件出错 + 由dataXml生成doc出错", true);
		}
		XMLDocUtil.createDocByAbsolutePath2( URLUtil.getResourceFileUrl("tss/cache.xml").getFile() );
    }
    
	@Test
    public void testMap2AttributeNode() {
    	Map<String, Object> map = new HashMap<String, Object>();
        map.put("email", "jinpj@g-soft.com.cn");
        map.put("age", new Integer(24));
        map.put("addr", null);

        Element node = XMLDocUtil.map2AttributeNode(map, "row");
        assertEquals("<row email=\"jinpj@g-soft.com.cn\" age=\"24\"/>", node.asXML());
    
        assertEquals(2, XMLDocUtil.dataNode2Map(node).size());
        assertEquals("", XMLDocUtil.getNodeText(node));
        
        assertEquals(0, XMLDocUtil.dataNode2Map(null).size());
	}
}

