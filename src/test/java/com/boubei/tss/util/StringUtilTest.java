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

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;

public class StringUtilTest {

	@Test
	public void test() {
		
		String emoji = "😁as撒旦法asdasd214🙃sadasdasd";
		Assert.assertEquals("*as撒旦法asdasd214*sadasdasd", StringUtil.replaceEmoji(emoji));
		Assert.assertNull( StringUtil.replaceEmoji(null) );
		
		Assert.assertEquals("a,b,c,d,e,f,g", StringUtil.fixSplit("a		b c，d    e,f、g", ",") );
		Assert.assertEquals(7, StringUtil.split("a		b c，d    e,f、g").length );
		
		String str = "Portletyyy~!@#$%^&*()_+-=[]{}\\|;':\",./<>?原型实现模型訾鄣迂蟓";

		Assert.assertEquals(str, StringUtil.GBKToUTF8(StringUtil.UTF8ToGBK(str)));
		String messyCode = StringUtil.UTF8ToGBK(StringUtil.GBKToUTF8(str));
		System.out.println(messyCode);
		
		try {
			StringUtil.convertCoding(str, "XXX", "GBK");
		} catch (Exception e) {
			Assert.assertTrue("字符串编码转换失败，不支持的编码方式：XXX", true);
		}
		
		try {
			StringUtil.convertCoding(str, "GBK", "XXX");
		} catch (Exception e) {
			Assert.assertTrue("字符串编码转换失败，不支持的编码方式：XXX", true);
		}
		
		new StringUtil();
		
		Assert.assertFalse( StringUtil.isMessyCode("字符串编码转换失败") );
		Assert.assertTrue( StringUtil.isMessyCode(messyCode) );
		Assert.assertFalse( StringUtil.isMessyCode(str + "ab?c") );
		
//		Assert.assertTrue( StringUtil.isMessyCode("ä»»å") );
		
		try {
			System.out.println( new String("%E6%B5%99A0001".getBytes("ISO-8859-1"), "UTF-8") );
		} catch (UnsupportedEncodingException e) {
		}
		
		try {
			System.out.println( Base64.encodeBase64String("我爱北京天安门".getBytes("GB2312")) );
		} catch (UnsupportedEncodingException e) {
		}
		
		System.out.println( "t_job".split(" ").length );
		System.out.println( "insert into t_job".split(" ").length );
	}

}
