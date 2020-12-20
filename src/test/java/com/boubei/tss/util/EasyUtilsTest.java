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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.junit.Assert;

public class EasyUtilsTest {

	@Test
	public void test() {
		
		Assert.assertTrue( EasyUtils.isDigit("012") );
		Assert.assertTrue( EasyUtils.isDigit("-8") );
		Assert.assertFalse( EasyUtils.isDigit("-8.22") );
		Assert.assertTrue( EasyUtils.isDigit("+8613588833833") );
		Assert.assertFalse( EasyUtils.isDigit("a12") );
		Assert.assertFalse( EasyUtils.isDigit(null) );
		
		Assert.assertFalse( EasyUtils.isTimestamp(null) );
		Assert.assertFalse( EasyUtils.isTimestamp(1234L) );
		Assert.assertTrue( EasyUtils.isTimestamp( System.currentTimeMillis() ) );
		
		List<Long> l = Arrays.asList(1L, 2L, 3L);
		Assert.assertTrue( EasyUtils.contains(l, "3,4") );
		Assert.assertTrue( EasyUtils.contains(l, "4, ,3") );
		Assert.assertFalse( EasyUtils.contains(l, "4,5,") );
		
		Assert.assertEquals(new Long(12), EasyUtils.str2Long("12"));
		Assert.assertNull(EasyUtils.str2Long("rp_12"));
		
		Assert.assertEquals("123", EasyUtils.checkNull(null, "123", null));
		Assert.assertNull(EasyUtils.checkNull(null, null, null));
		
		Assert.assertEquals("123", EasyUtils.checkNullI(null, "123", null));
		Assert.assertEquals("", EasyUtils.checkNullI(null, null, ""));
		
		Assert.assertEquals("123", EasyUtils.checkTrue(1>=0, "123", "456"));
		Assert.assertEquals("456", EasyUtils.checkTrue(1==0, "123", "456"));
		
		Assert.assertEquals(0, InfoEncoder.hexCharToByte('$'));
		Assert.assertTrue( !EasyUtils.fmParseError(null) );
		Assert.assertTrue( EasyUtils.fmParseError( EasyUtils.FM_PARSE_ERROR + "xxxxx" ) );
		
		try{
			EasyUtils.obj2Double("Jon");
		} catch ( Exception e) {
			Assert.assertTrue(e.getMessage(), true);
		}
		try{
			EasyUtils.obj2Int("Jon");
		} catch ( Exception e) {
			Assert.assertTrue(e.getMessage(), true);
		}
		
		System.out.println( EasyUtils.obj2Int("2147483647") );
		try{
			EasyUtils.obj2Int("2147483648" );
		} catch ( Exception e) {
			Assert.assertEquals("【2147483648】过大，不是int", e.getMessage());
		}
		try{
			EasyUtils.obj2Long("Jon");
		} catch ( Exception e) {
			Assert.assertTrue(e.getMessage(), true);
		}
		
		Assert.assertTrue(0d == EasyUtils.obj2Double(null));
		Assert.assertTrue(0 == EasyUtils.obj2Int(null));
		Assert.assertTrue(0l == EasyUtils.obj2Long(null));
		Assert.assertTrue(0d == EasyUtils.obj2Double(" "));
		Assert.assertTrue(0 == EasyUtils.obj2Int(" "));
		Assert.assertTrue(0l == EasyUtils.obj2Long(" "));
		Assert.assertEquals("", EasyUtils.obj2String(null));
		
		Assert.assertEquals("null", EasyUtils.obj2Json(null));
		Assert.assertEquals("{}", EasyUtils.obj2Json( new HashMap<String, String>() ));
		
		EasyUtils.obj2Json( new Object() ); // return exception
		
		Assert.assertTrue(EasyUtils.obj2Double("1.01") == 1.01d);
		Assert.assertTrue(EasyUtils.obj2Int("1") == 1);
		Assert.assertTrue(EasyUtils.obj2Long("1") == 1L);
		Assert.assertEquals("12", EasyUtils.obj2String(12));

		Assert.assertTrue(EasyUtils.isNullOrEmpty(""));
		Assert.assertTrue(!EasyUtils.isNullOrEmpty(new Object()));
		Assert.assertTrue(EasyUtils.isNullOrEmpty(new ArrayList<Object>()));

		String s = "Jinpujun|English|name|is|JonKinga";
		Assert.assertEquals(5, EasyUtils.split(s, "|").length);
		Assert.assertNull( EasyUtils.split(null, "|") );

		String encodeHex = InfoEncoder.encodeHex(s.getBytes());
		Assert.assertEquals(s, new String(InfoEncoder.decodeHex(encodeHex)));

		Collection<User> list = new ArrayList<User>();
		list.add(new User(1, "Jon1"));
		list.add(new User(2, "Jon2"));
		list.add(new User(3, "Jon3"));
		
		String[] result = EasyUtils.list2Combo(list, "id", "name", "|");
		Assert.assertEquals("1|2|3", result[0]);
		Assert.assertEquals("Jon1|Jon2|Jon3", result[1]);
		
		Assert.assertEquals("[Jon1, Jon2, Jon3]", EasyUtils.objAttr2List(list, "name").toString());

		Assert.assertEquals("Jon1,Jon2,Jon3", EasyUtils.list2Str(list));
		Assert.assertEquals("Jon1|Jon2|Jon3", EasyUtils.list2Str(list, "|"));
		Assert.assertEquals("", EasyUtils.list2Str(null));
		
		Collection<String> list2 = new ArrayList<String>();
		list2.add("");
		list2.add(null);
		list2.add("Jon3");
		list2.add("");
		Assert.assertEquals(",Jon3,", EasyUtils.list2Str(list2));
		
		Collection<Object[]> list3 = new ArrayList<Object[]>();
		list3.add(new Object[]{1, "G1"});
		list3.add(new Object[]{2, "G2"});
		list3.add(new Object[]{3, "G3"});
		Assert.assertEquals("G1,G2,G3", EasyUtils.list2Str(list3, 1));
		Assert.assertEquals("1,2,3", EasyUtils.list2Str(list3, 0));
		
		Collection<Map<String, Object>> list4 = new ArrayList<Map<String, Object>>();
		HashMap<String, Object> m = new HashMap<String, Object>();
		m.put("key1", "G1");
		list4.add( m );
		m = new HashMap<String, Object>();
		m.put("key1", "G2");
		list4.add( m );
		Assert.assertEquals("G1,G2", EasyUtils.attr2Str(list4, "key1"));
		
		List<String> list5 = EasyUtils.toList("1,2,3,4,5,");
		Assert.assertEquals(6, list5.size());
		Assert.assertEquals(0, EasyUtils.toList("").size());
		Assert.assertEquals(3, EasyUtils.toList(",,").size());
		
		Assert.assertEquals("12,33,44,66", EasyUtils.filterEmptyItem("12,,,33,44,,66"));

		Assert.assertEquals("%E8%BF%87%E6%B2%B3%E5%8D%92%E5%AD%90", StringUtil.toUtf8String("过河卒子"));
		
		Map<String, Object> data = m;
		data.put("x", 1001200);
		Assert.assertEquals("1001200", EasyUtils.fmParse("<#if x??>${x}</#if>", data));
		
		System.out.println( EasyUtils.fmParse("<#if x??>${x}</#if> 123", data) );
		System.out.println( EasyUtils.fmParse("<#if !x??>${x}</#if> 123", data) );
		System.out.println( EasyUtils.fmParse("<#if !y??>${x}</#if> 1234", data) );
		System.out.println( EasyUtils.fmParse(" ${y!-1} abc", data) );
		
		data.put("userId", null);
		System.out.println( EasyUtils.fmParse(" ${userId!-1} xxx", data) );
		
		data.put("x", "");
		System.out.println( EasyUtils.fmParse(" <#if x=''>x is empty</#if>", data) );
		
		data.put("xx", new HashMap<>());
		System.out.println( EasyUtils.fmParse(" ${xx} ", data) );  // 错误，xx不能是map
		
		data.put("yy", new HashMap<>().toString());
		System.out.println( EasyUtils.fmParse(" ${yy} ", data) );  // 正确
		
		Map<String, Object> d = new HashMap<>();
		d.put("${中64G}", 1234);
		System.out.println( EasyUtils.fmParse("${中64G}", d) ); // key 需是非数字开头
		
		// test eval js
		data.put("x", 1);
		data.put("y", 2.0);
		data.put("z", 3.0);
		data.put("a", "${z}");
		Assert.assertEquals(new Double(6), EasyUtils.eval("${x}*${y}*${z}", data) );
		Assert.assertEquals(new Double(9), EasyUtils.eval("${z}*${a}", data) );
		Assert.assertEquals(new Double(6), EasyUtils.eval("1*2*3", data) );
		Assert.assertEquals(new Double(6), EasyUtils.eval("1*2*3", null) );
		try{
			EasyUtils.eval("x*y*z", data);
			Assert.fail();
		} catch ( Exception e) {
			Assert.assertTrue(e.getMessage(), true);
			System.out.println(e.getMessage());
		}
		
		Map<String, String> map = new HashMap<String, String>();
        map.put("KFC", "kfc");
        map.put("WNBA", "wnba");
        map.put("nba", "nba");
        map.put("CBA", "cba");
        Map<String, String> resultMap = EasyUtils.sortMapByKey(map);    // 按Key进行排序
        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        
        Assert.assertTrue( EasyUtils.isProd() );  // test需要当prod对待，保证测试覆盖
	}
	
	public static class User {
		Integer id;
		String name;
		
		public User(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
		
		public String toString() {
			return name;
		}
	}

}
