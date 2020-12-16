/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.web.json;

import java.util.Date;

import org.codehaus.jackson.JsonGenerator;
import org.junit.Test;

import com.boubei.tss.util.DateUtil;

import junit.framework.Assert;

public class DateConvertTest {
	
	@Test
	public void test() {
		Date now = new Date();
		Date d = new DateConvert().convert(DateUtil.format(now));
		Assert.assertEquals(DateUtil.noHMS(now), d);
	}
	
	@Test
	public void test2() {
		TSSObjectMapper tom = new  TSSObjectMapper();
		Assert.assertNotNull(tom);
		
		TSSJsonSerializer tjs = new TSSJsonSerializer();
		JsonGenerator jsonGenerator = null;
		try {
			tjs.serialize(new Date(), jsonGenerator , null);
		} catch (Exception e) {
		}
	}

}
