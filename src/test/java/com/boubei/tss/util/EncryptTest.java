/* ==================================================================   
 * Created [2009-4-27 下午11:32:55] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) Jon.King, 2015-2018  
 * ================================================================== 
 */

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

import static org.junit.Assert.fail;

import org.junit.Test;
 
public class EncryptTest {

	InfoEncoder encrypt = new InfoEncoder();
	EncryptTestThreadGroup group = new EncryptTestThreadGroup();
 
	@Test
	public void testCreateEncryptor() {
		for (int i = 0; i < 10; i++) {
			new Thread(group, "encryptor") {
				public void run() {
					encrypt.createEncryptor("testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest");
				}
			}.start();
		}
	}
 
	@Test
	public void testCreateDecryptor() {
		try {
			final String str = encrypt.createEncryptor("testtesttesttesttesttest");
			for (int i = 0; i < 10; i++) {
				new Thread(group, "decryptor") {
					public void run() {
						encrypt.createDecryptor(str);
					}
				}.start();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}

class EncryptTestThreadGroup extends ThreadGroup {

	public EncryptTestThreadGroup() {
		super("EncryptTestThreadGroup");
	}

	/**
	 * 异常处理
	 * @see java.lang.ThreadGroup#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	public void uncaughtException(Thread t, Throwable e) {
		fail("Thread:" + t.getName() + " " + e.getMessage());
	}
}
