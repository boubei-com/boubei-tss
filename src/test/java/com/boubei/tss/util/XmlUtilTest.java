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

import org.junit.Test;

public class XmlUtilTest {
 
	@Test
    public void testToXmlForm() {
		new XmlUtil();
		assertEquals("", XmlUtil.toFormXml(null));
        assertEquals("&lt;qqq&amp;www\\&quot;&gt;", XmlUtil.toFormXml("<qqq&www\\\">"));
        
        System.out.println( XmlUtil.toFormXml(FileHelper.readFile( URLUtil.getResourceFileUrl("testdata/1.xml" ).getPath() )) );
    }
    
	@Test
    public void testReplaceXMLPropertyValue() {
        assertEquals("&lt;qqq&amp;www code=&quot;TSS&quot; \\&gt;", XmlUtil.replaceXMLPropertyValue("<qqq&www code=\"TSS\" \\>"));
    }
    
	@Test
    public void testStripNonValidXMLCharacters() {
		assertEquals("", XmlUtil.stripNonValidXMLCharacters(null));
		
        String value = XmlUtil.stripNonValidXMLCharacters("<server code=\"TSS\" name=\"tss\" />");
        assertEquals("<server code=\"TSS\" name=\"tss\" />", value);
    }
}

