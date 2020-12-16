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

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.AbstractTest4TSS;

public class MailUtilTest extends AbstractTest4TSS {
	
	@Test
	public void test1() {
		try {
			MailUtil.send("这是一封简单的测试邮件", "看到的人都会长命百岁");
		} catch(Exception e) {
		}
	}
	
	@Test
	public void test2() {
		try {
			MailUtil.send("test", "test", null, MailUtil.DEFAULT_MS);
		} catch(Exception e) {
		}
		
		try {
			MailUtil.send("test", "test", new String[]{}, MailUtil.DEFAULT_MS);
		} catch(Exception e) {
		}
		
		Assert.assertNotNull( MailUtil.getMailSender("tt") );
		String[] v = MailUtil.parseReceivers("boubei@163.com#sys");
		Assert.assertEquals("sys", v[0]);
		Assert.assertEquals("boubei@163.com", v[1]);
		
		String[] eamils = "bou bei @163.com,jk@boubei.com".split(",");
		String[] _emails = MailUtil.preCheatEmails(eamils);
		Assert.assertEquals(2, _emails.length);
		Assert.assertEquals("boubei@163.com", _emails[0]);
	}

}
