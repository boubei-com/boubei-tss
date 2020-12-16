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

import java.util.Date;

import org.junit.Test;

import junit.framework.Assert;

public class DateUtilTest {
	
	@Test
	public void test() {
		
		Date date = new Date();
		
		String formatStr = DateUtil.format(date);
		System.out.println(formatStr);
		
		String formatStr2 = DateUtil.format(date, "yyyy-MM-dd HH:mm");
		System.out.println(formatStr2);
		
		Assert.assertEquals(formatStr.replaceAll("-", ""), DateUtil.format(date, "yyyyMMdd"));
		
		Assert.assertTrue(formatStr2.startsWith(formatStr));
		
		formatStr = DateUtil.formatCare2Second(date);
		System.out.println(formatStr);
		Assert.assertTrue(formatStr.startsWith(formatStr2));
		
		Assert.assertNull(DateUtil.parse(""));
		Assert.assertTrue(date.after(DateUtil.parse("2013-11-07")));
		
		Assert.assertEquals("", DateUtil.formatCare2Second(null));
		Assert.assertTrue( DateUtil.format(date, "").length() > 0 );
		Assert.assertEquals("", DateUtil.format(null, "yyyy-MM-dd HH:mm"));
		Assert.assertEquals("", DateUtil.formatCare2Second(null));
		
		Assert.assertNotNull(DateUtil.parse("2013-11-07 11:26:05"));
		Assert.assertNotNull(DateUtil.parse("2013/11/07 11:26:05"));
		Assert.assertNotNull(DateUtil.parse("2013-11-07"));
		Assert.assertNotNull(DateUtil.parse("2013/11/07"));
		
		Assert.assertNotNull(DateUtil.parse("2013.11.07"));
		Assert.assertNotNull(DateUtil.parse("2013.11.7"));
		Assert.assertNotNull(DateUtil.parse("2013.7.7"));
		
		Assert.assertNotNull(DateUtil.parse("2013/11/07 11:26"));
		Assert.assertEquals(DateUtil.parse("2013-11-07 11:26"), DateUtil.parse("2013/11/07 11:26"));
		
		Assert.assertNull( DateUtil.parse("2013/、11-07") );
	}
	
	@Test
	public void test2() {
		Date today = DateUtil.today();
		Date day1 = today;
		Date day2 = DateUtil.addDays(day1, 10);
		Date day3 = DateUtil.subDays(day1, 10);
		DateUtil.now();
		
		Assert.assertEquals(21, DateUtil.daysBetweenFromAndTo(day3, day2).size());
		
		DateUtil.getDay(day1);
		int month = DateUtil.getMonth(day1);
		int year = DateUtil.getYear(day1);
		
		Date day4 = DateUtil.noHMS(new Date());
		Assert.assertEquals(0, DateUtil.getHour(day4));
		
		
		Assert.assertEquals(2, DateUtil.getDayOfWeek( DateUtil.parse("2017-04-04") ));
		Assert.assertEquals(6, DateUtil.getDayOfWeek( DateUtil.parse("2017-04-08") ));
		Assert.assertEquals(7, DateUtil.getDayOfWeek( DateUtil.parse("2017-04-09") ));
		
		System.out.println(DateUtil.getMinute(new Date()));
		
		DateUtil.isMonthEnd(day1);
		
		Assert.assertEquals("202006", DateUtil.toYYYYMM(2020, 6, ""));
		Assert.assertEquals("202012", DateUtil.toYYYYMM(2020, 12, ""));
		
		Assert.assertEquals(DateUtil.toYYYYMM(year, month, ""), DateUtil.toYYYYMM(day1, ""));
		
		Assert.assertEquals( DateUtil.format(today), DateUtil.fastCast("today-0"));
		Assert.assertEquals( DateUtil.format(today), DateUtil.fastCast("today+0"));
		
		Assert.assertEquals( DateUtil.getYear(today)+"-01-01", DateUtil.fastCast("cur_year_01"));
		Assert.assertEquals( DateUtil.toYYYYMM(today, "-")+"-01", DateUtil.fastCast("cur_month_01"));
		System.out.println( DateUtil.fastCast("cur_week_01") );
		Assert.assertEquals( "2018-01-01", DateUtil.fastCast("2018-01-01"));
	}

}
