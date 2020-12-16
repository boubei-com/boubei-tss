/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.util.DateUtil;

public class DMUtilTest {
	
	@Test
	public void test() {
		Assert.assertEquals("", DMUtil.preTreatVal(null));
		Assert.assertEquals("；'123'，", DMUtil.preTreatVal(";'123',\""));
		
		Assert.assertNull(DMUtil.preTreatValue(null, _Field.TYPE_STRING));
		
		Assert.assertEquals("JK", DMUtil.preTreatValue("JK", null));
		Assert.assertEquals("JK", DMUtil.preTreatValue("JK", _Field.TYPE_HIDDEN));
		
		Assert.assertEquals("'JK'", DMUtil.preTreatValue("'JK'", null));
		
		try {
			DMUtil.preTreatValue("xxx", _Field.TYPE_NUMBER);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("【xxx】不是有效数字", e.getMessage());
		}
		try {
			DMUtil.preTreatValue("xxx", _Field.TYPE_INT);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("【xxx】不是有效数字", e.getMessage());
		}
		
		Assert.assertEquals(12.0d, DMUtil.preTreatValue("12", _Field.TYPE_NUMBER));
		Assert.assertEquals(12.2, DMUtil.preTreatValue("12.2", _Field.TYPE_NUMBER));
		Assert.assertEquals(.2, DMUtil.preTreatValue(".2", _Field.TYPE_NUMBER));
		Assert.assertEquals(12888.2, DMUtil.preTreatValue("￥12,888.2", _Field.TYPE_NUMBER));
		
		Assert.assertEquals("'s1','s2','s3'", DMUtil.insertSingleQuotes("s1,s2,s3"));
		Assert.assertEquals("'s1','s2','s3'", DMUtil.insertSingleQuotes("'s1','s2','s3'"));
		Assert.assertEquals("'s1'", DMUtil.insertSingleQuotes("s1"));
		Assert.assertNull(DMUtil.insertSingleQuotes(null));
		Assert.assertEquals("'s1','s2','s3'", DMUtil.insertSingleQuotes("'s1','s2','s3'"));
		Assert.assertEquals("'s1'", DMUtil.insertSingleQuotes("'s1'"));
		
		Assert.assertNull(DMUtil.preTreatValue(" ", _Field.TYPE_DATE));
		Assert.assertEquals(DateUtil.parse("2015-04-06"), DMUtil.preTreatValue("2015-04-06", _Field.TYPE_DATE));
		Assert.assertEquals(DateUtil.parse("2015-04-06 06:06:06"), DMUtil.preTreatValue("2015-04-06 06:06:06", _Field.TYPE_DATETIME));
		
		try {
			DMUtil.preTreatValue("2015-04-06T06:06:06", _Field.TYPE_DATETIME);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.DM_01, "2015-04-06T06:06:06") , e.getMessage());
		}
		
		// test freemarker error print
		// Expression param1 is undefined on line 1, column 22 in t.ftl
		Map<String, String> map = new HashMap<String, String>();
		map.put("report.info", "报表【120, 报表120, 创建人, 修改人】");
		map.put("x.x", "1");
		
		DMUtil.freemarkerParse("<#if p1??> <#else> ${param1} </#if>", map);
		
		String s = "1=1 <#if userRoles != '-10000'> and creator = '${userCode}' </#if>";
		map.put("userCode", "BD0000");
		map.put("userRoles", "-10000");
		Assert.assertEquals("1=1 ", DMUtil.freemarkerParse(s, map));
		
		map.put("userRoles", "-10000,9");
		Assert.assertEquals("1=1  and creator = 'BD0000' ", DMUtil.freemarkerParse(s, map));
		
		Assert.assertEquals(0, DMUtil.preTreatScopeValue(null).length);
		Assert.assertEquals(1, DMUtil.preTreatScopeValue("[2016-10-01]").length);
		Assert.assertEquals(1, DMUtil.preTreatScopeValue("[2016-10-01,]").length);
		Assert.assertEquals(2, DMUtil.preTreatScopeValue("[,2016-10-01]").length);
		Assert.assertEquals(0, DMUtil.preTreatScopeValue("[,]").length);
		Assert.assertEquals(2, DMUtil.preTreatScopeValue("[2016-10-01,2016-10-31]").length);
		Assert.assertEquals(2, DMUtil.preTreatScopeValue("[1,100,108]").length);
		
		StringBuffer params = new StringBuffer();
		for(int i = 0; i < 600; i++) {
			params.append(i);
		}
		Assert.assertEquals(500, DMUtil.cutParams(params.toString()).length());
		
		// test sql inject check
		Assert.assertEquals("12,ab,cd,w23", DMUtil.checkSQLInject("12,ab,cd,w23") );
		Assert.assertEquals("2016-10-01,2016-10-31", DMUtil.checkSQLInject("2016-10-01,2016-10-31") );
		Assert.assertEquals("2016-10-01 10:01:37", DMUtil.checkSQLInject("2016-10-01 10:01:37") );
		Assert.assertEquals("inject-word:delete", DMUtil.checkSQLInject(" delete from t1") );
		Assert.assertEquals("inject-word:or|and", DMUtil.checkSQLInject("'admin' or 1=1") );
		Assert.assertEquals("'admin'or(1-1 0)* +  admin'or(1-1 0)* +  %", DMUtil.checkSQLInject("'admin'or(1-1=0)*;+<>admin'or(1-1=0)*;+<>%") );
		
		Assert.assertEquals("(select * from t1 where 1=1 <#if DOMAIN??> and domain = '${DOMAIN}' </#if>) x",  DMUtil.wrapTable("t1", true, false) );
		Assert.assertEquals("(select * from t1 where 1=1 <#if userCode??> and creator = '${userCode}' </#if>) x", DMUtil.wrapTable("t1", false, false) );
		Assert.assertEquals("t1", DMUtil.wrapTable("t1", true, true) );
		
		Assert.assertNull( DMUtil.getExtendAttr(null, "icon") );
		Assert.assertNull( DMUtil.getExtendAttr("test", "icon") );
		Assert.assertEquals("<div class='icon icon-key'></div>", DMUtil.getExtendAttr("test \n icon:=<div class='icon icon-key'></div>", "icon") );
		Assert.assertEquals("images/key.png", DMUtil.getExtendAttr("test \n icon:=images/key.png", "icon") );
		
		// 带emoj表情符号的字符串
		String text = "This is a smiley \uD83C\uDFA6 face\uD860\uDD5D \uD860\uDE07 \uD860\uDEE2 \uD863\uDCCA \uD863\uDCCD \uD863\uDCD2 \uD867\uDD98 ";
		Assert.assertEquals("This is a smiley * face𨅝 𨈇 𨋢 𨳊 𨳍 𨳒 𩶘", DMUtil.preTreatValue(text, null));
		
		// test double parse
		String script = "${x}";
		Map<String, Object> m = new HashMap<>();
		m.put("x", "${y}");
		m.put("y", "abc");
		Assert.assertEquals("abc", DMUtil.fmParse(script, m , false));
		
		Assert.assertEquals("abc", DMUtil.fmParse("<#if 网点财务!0 == 0>abc</#if>", m , false));  // 判断是否没有“网点财务”这个角色
	} 
	
	static class TIMESTAMP {
		
	}

}
