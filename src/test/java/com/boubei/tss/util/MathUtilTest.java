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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MathUtilTest {
	
	@Test
	public void testMathUtil() {
		
		assertTrue( MathUtil.addDoubles(-2.021d, MathUtil.addDoubles(1.02d, 1.001d)) == 0d);
		assertTrue( MathUtil.subDoubles(2.021d, MathUtil.addDoubles(1.02d, 1.001d)) == 0d);
		
		assertTrue(3 == MathUtil.addInteger(1, 2));
		
		assertTrue(1.21d == MathUtil.multiply(1.1d, 1.1d));
		
		assertTrue(0d == MathUtil.multiply(null, null));
		
		int value = MathUtil.randomInt(10);
		assertTrue(value >= 0);
		assertTrue(value <= 10);
		
		value = MathUtil.randomInt6();
		assertTrue(value >= 100000);
		assertTrue(value <= 999999);
		
		assertTrue(0d == MathUtil.addDoubles(null, null));
		assertTrue(0 == MathUtil.addInteger(null, null));
		
		assertEquals("", MathUtil.formatNumber(null, ",###") );
		assertEquals("10,000", MathUtil.formatNumber(10000, ",###") );
		assertEquals("10000", MathUtil.formatNumber(10000, null) );
		
		double number = 111111123456.127;
		System.out.println(MathUtil.formatNumber(number, "###,####.00"));   
		System.out.println(MathUtil.formatNumber(number, "###,###.0000")); 
		System.out.println(MathUtil.formatNumber(number, "##,###.000")); 
		System.out.println(MathUtil.formatNumber(0.3052222, "#0.00%")); 
		
		System.out.println(MathUtil.formatNumber(0.8, "###,##0.00"));
		System.out.println(MathUtil.formatNumber(.899, "###,##0.00"));
		System.out.println(MathUtil.formatNumber(".899", "###,##0.00"));
		
		assertEquals(12, MathUtil.calPercent(12.0f, 100.0f) );
		assertEquals(0, MathUtil.calPercent(12.0f, 0) );
	}

}
