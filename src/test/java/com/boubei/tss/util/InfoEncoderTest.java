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

import org.junit.Assert;
import org.junit.Test;

public class InfoEncoderTest {
    
	@Test
    public void testInfoEncoder() {
        InfoEncoder test = new InfoEncoder();
        
        String encodedMsg = test.createEncryptor("Jon.King");
		assertEquals("0q7lY7PAMeMid9RK9PA7ew==", encodedMsg);
        assertEquals("Jon.King", test.createDecryptor(encodedMsg));
        
        String md5PWD = InfoEncoder.string2MD5("Admin_123456");
        assertEquals("E5E0A2593A3AE4C038081D5F113CEC78", md5PWD);
        
        System.out.println( InfoEncoder.string2MD5( "Admin_boubei.COM2015" ));
        System.out.println( InfoEncoder.string2MD5("试试中文") );
        
        String testValue = "Jon.King!@#$%^&*()";
        int key = 869;
        String encodeValue = InfoEncoder.simpleEncode(testValue, key);
        System.out.println(encodeValue);
        assertEquals(testValue, InfoEncoder.simpleDecode(encodeValue, key));
        
        System.out.println('X' ^ 't' ^ 't');
		System.out.println('!' ^ 't' ^ 't');
		
		assertEquals("", InfoEncoder.simpleEncode("", 12));
		assertEquals("", InfoEncoder.simpleDecode("", 12));
		Assert.assertNull(InfoEncoder.simpleEncode(null, 12));
		Assert.assertNull(InfoEncoder.simpleDecode(null, 12));
		
		String s1 = "13588884444";
		String s2 = "鲁智深";
		String s3 = "浙江省杭州市灵隐寺123号";
		Assert.assertNull( InfoEncoder.cover(null, 7, 8) );
		assertEquals("135888**444", InfoEncoder.cover(s1, 7, 8) );
		assertEquals("1358888*444", InfoEncoder.cover(s1, 17, 8) );
		assertEquals("13588884***", InfoEncoder.cover(s1, 9, 80) );
		assertEquals("*智深", InfoEncoder.cover(s2, 1, 1) );
		assertEquals("浙江省杭州市灵隐寺12**", InfoEncoder.cover(s3, 100, 100) );
    }

	public static void main(String[] args) {
		InfoEncoder ie = new InfoEncoder();
		System.out.println( InfoEncoder.simpleEncode("361000", 12));
		
		System.out.println( ie.createEncryptor("1212") );
		System.out.println( ie.createDecryptor("Xsz/s+VvOgY=") );
		System.out.println( ie.createDecryptor( "3u1dt5T33PNCgByaGoyMFOMw7N973TrsYbIN9AjcpUILtQjEGWFju4mdid01QmHK9o5ipEzBVO3xfREypahK5w==" ) );
	}
}
