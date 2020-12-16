/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.helper;

import org.junit.Assert;
import org.junit.Test;

public class PasswordRuleTest {

	@Test
	public void testPasswordRule() {
		Assert.assertEquals(PasswordRule.UNQUALIFIED_LEVEL, PasswordRule.getStrengthLevel("admin", "admin"));
		
		Assert.assertEquals(PasswordRule.UNQUALIFIED_LEVEL, PasswordRule.getStrengthLevel("123456", "admin"));
		
		Assert.assertEquals(PasswordRule.UNQUALIFIED_LEVEL, PasswordRule.getStrengthLevel("111111", "BL00618"));
		
		Assert.assertEquals(PasswordRule.HIGHER_LEVEL, PasswordRule.getStrengthLevel("123456789a", "jonking"));
		Assert.assertEquals(PasswordRule.MEDIUM_LEVEL, PasswordRule.getStrengthLevel("j123456j", "jonking"));
		Assert.assertEquals(PasswordRule.HIGHER_LEVEL, PasswordRule.getStrengthLevel("j123456J", "jonking"));
		Assert.assertEquals(PasswordRule.HIGHER_LEVEL, PasswordRule.getStrengthLevel("ax=1234567890", "jonking"));
		Assert.assertEquals(PasswordRule.UNQUALIFIED_LEVEL, PasswordRule.getStrengthLevel("jonking", "jonking"));
		
		PasswordRule rule = PasswordRule.getDefaultPasswordRule();
		Assert.assertEquals(0, PasswordRule.checkAvailable(rule , "123456"));
		
		Assert.assertEquals(PasswordRule.LOW_LEVEL, PasswordRule.getStrengthLevel("111111111", "jonking"));
		Assert.assertEquals(PasswordRule.LOW_LEVEL, PasswordRule.getStrengthLevel("111222222", "jonking"));
		Assert.assertEquals(PasswordRule.MEDIUM_LEVEL, PasswordRule.getStrengthLevel("111222333", "jonking"));
		Assert.assertEquals(PasswordRule.MEDIUM_LEVEL, PasswordRule.getStrengthLevel("1112223333", "jonking"));
		
		Assert.assertEquals(PasswordRule.UNQUALIFIED_LEVEL, PasswordRule.getStrengthLevel("111222", "jonking"));
	}
	
}
