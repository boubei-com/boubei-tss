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
import java.util.Map;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.framework.web.display.xform.XFormDecoder;
import com.boubei.tss.framework.web.display.xform.XFormEncoder;
import com.boubei.tss.framework.web.display.xform.XFormTemplet;
import com.boubei.tss.modules.log.Log;
import com.boubei.tss.modules.log.LogAction;
import com.boubei.tss.um.entity.Group;

import org.junit.Assert;

public class XFormTest {
	
	@Test
	public void testXForm() {
		MockHttpServletResponse response = new MockHttpServletResponse();
		
		Log entity = new Log();
		entity.setId(12L);
		
		XFormEncoder encoder = new XFormEncoder(LogAction.LOG_XFORM_TEMPLET_PATH, entity);
		
		entity = (Log) XFormDecoder.decode(encoder.toXml(), Log.class);
		Assert.assertEquals(new Long(12), entity.getId());
		
		try {
			XmlPrintWriter writer = new XmlPrintWriter(response.getWriter());
			encoder.print(writer);
		} 
		catch (IOException e) {
			Assert.fail();
		}
		
		XFormDecoder.decode(null, Group.class);
		
    	try {
    		new XFormTemplet("");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("没有定义XForm模板文件的路径", true);
		}
    	
    	IXForm entity2 = null;
    	new XFormEncoder("template/cache/strategy_xform.xml", entity2);
    	
    	Map<String, Object> attributesMap = null; 
    	new XFormEncoder("template/cache/strategy_xform.xml", attributesMap);
    	
    	XFormEncoder xe = new XFormEncoder("template/cache/strategy_xform.xml");
    	try {
    		xe.setColumnAttribute("x1", "x1", "x1");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("字段在XFORM模板里不存在，设置属性失败！", true);
		}
    	
    	xe.fixCombo("x1", null);
    	
	}
}
