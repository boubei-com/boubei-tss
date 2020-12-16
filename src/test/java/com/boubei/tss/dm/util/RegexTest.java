/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.util;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.util.DateUtil;

/**
 * Matcher.find    部分匹配
 * Matcher.matches 全局匹配
 */
public class RegexTest {
	
	@Test 
	public void test0() {
		
		Assert.assertTrue( Pattern.compile("^[\\d\\,-]*$").matcher("137,8,-8,89").matches() );
        Assert.assertTrue( Pattern.compile("[1-9]{11}").matcher("13788889999").matches() );
        
        Assert.assertTrue( "11".matches("^[0-9]*$") );
        Assert.assertFalse( "rp_11".matches("^[0-9]*$") );
        Assert.assertFalse( "-11".matches("^[0-9]*$") );
	}

	@Test
  	public void test() {
		Pattern pp = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");  
	    Assert.assertTrue( pp.matcher("卜贝数据").matches() == false );
	    Assert.assertTrue( pp.matcher("CX_as").matches() == true );
	    Assert.assertTrue( pp.matcher("CX-as").matches() == false );
		
  		String paramValue = "today - 7"; // "2012-12-12"
  		Date dateObj;
  		if (Pattern.compile("^today[\\s]*-[\\s]*\\d").matcher(paramValue).matches()) {
  			int deltaDays = Integer.parseInt(paramValue.split("-")[1].trim());
  			Date today = DateUtil.noHMS(new Date());
  			dateObj = DateUtil.subDays(today, deltaDays);
  		} 
		else {
			try {
				dateObj = DateUtil.parse(paramValue);
			} catch(Exception e) {
				dateObj = null;
			}
		}
  			
  		System.out.println(dateObj);
  		
  		String regExp = "^[1][3,4,5,6,7,8,9][0-9]{9}$";  
        Pattern p = Pattern.compile(regExp);  
//        Matcher m = p.matcher("13588833834");  
        Assert.assertTrue( p.matcher("13588833834").find() );
        Assert.assertFalse( p.matcher("12588833834").find() ); 
        
        Assert.assertTrue( p.matcher("13588833834").matches() ); 
        
        p = Pattern.compile("[a-z|A-Z|0-9]+");  
        System.out.println( p.matcher("CX12ab").matches() );
        System.out.println( p.matcher("CX12ab@#").matches() );
        System.out.println( p.matcher("慈溪12").matches() );
        
        String code = "p1", paramKey = "param1", val = "xxx in  (${ param1 })";
        System.out.println("1. " + Pattern.compile("in[\\s]*\\(\\$\\{[\\s]*(" +code+ "|" +paramKey+ ")[\\s]*\\}\\)").matcher(val).find() ); 
        
        val = "<#if param1==1> group by week </#if>";
        System.out.println("2. " + Pattern.compile("if[\\s]+(" +code+ "|" +paramKey+ ")").matcher(val).find() ); 
        System.out.println("2. " + Pattern.compile("if[\\s]+(" +code+ "|" +paramKey+ ")\\?\\?").matcher(val).find() ); 
        
        val = "<#if param1??> createTime > ? </#if>";
        System.out.println("3. " + Pattern.compile("if[\\s]+(" +code+ "|" +paramKey+ ")").matcher(val).find() ); 
        System.out.println("3. " + Pattern.compile("if[\\s]+(" +code+ "|" +paramKey+ ")\\?\\?").matcher(val).find() ); 
        
  	}
	
	@Test
  	public void test2() {
		String sql = "select u.id, u.userName Name from um_user u, um_groupuser gu " +
				"where u.id = gu.userId and gu.groupId = ?";
		
		Pattern p = Pattern.compile("text|name|id|pk", Pattern.CASE_INSENSITIVE); // 忽略大小写
		Matcher m = p.matcher(sql);
		Assert.assertTrue( m.find() );

  	}
	
	@Test
  	public void test3() {            
		// 剔除掉 ASCII 里 0-8、11-12、14-31、127 的控制字符
		Pattern p = Pattern.compile("[\\x00-\\x08\\x0b-\\x0c\\x0e-\\x1f\\x7f]");
        String value = "开发区负责人";
        value = p.matcher(value).replaceAll("");

        System.out.print(value);
	}
}
