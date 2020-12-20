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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import org.junit.Test;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.UserAgent;
import eu.bitwalker.useragentutils.Version;
import org.junit.Assert;

public class URLUtilTest {
	
	@Test
    public void testParseBrowser() {
		String  s = "mozilla/5.0 (windows nt 6.1) applewebkit/537.36 (khtml, like gecko) chrome/58.0.3029.110 safari/537.36 se 2.x metasr 1.0";
//		s = "mozilla/5.0 (macintosh; intel mac os x 10_13_1) applewebkit/537.36 (khtml, like gecko) chrome/68.0.3440.106 safari/537.36";
//		s = "user-agent = Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0";
		
		UserAgent userAgent = UserAgent.parseUserAgentString( s );
        Browser browser = userAgent.getBrowser();
   
        System.out.println(browser.getName()); // 浏览器名称
        System.out.println(browser.getGroup().getName()); // 浏览器大类
        System.out.println(userAgent.getOperatingSystem());// 访问设备系统
        System.out.println(userAgent.getOperatingSystem().getDeviceType());// 访问设备类型
        System.out.println(userAgent.getOperatingSystem().getManufacturer());// 访问设备制造厂商
        
        Version browserVersion = userAgent.getBrowserVersion(); // 详细版本
        System.out.println( "version = " + browserVersion );
        String version = browserVersion.getMajorVersion();      // 浏览器主版本
        System.out.println( "major_version = " + version );
        
        System.out.println( URLUtil.parseBrowser(s) );
        System.out.println( URLUtil.parseBrowser("微信") );
        
        s = "user-agent = Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0";
        System.out.println( URLUtil.parseBrowser(s) );
        
        s = "mozilla/5.0 (macintosh; intel mac os x 10_13_1) applewebkit/537.36 (khtml, like gecko) chrome/68.0.3440.106 safari/537.36";
        System.out.println( URLUtil.parseBrowser(s) );

        // QQ浏览器
        s = "mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.36 (khtml, like gecko) chrome/39.0.2171.95 safari/537.36 micromessenger/6.5.2.501 nettype/wifi windowswechat qbcore/3.43.901.400 qqbrowser/9.0.25";
        System.out.println( URLUtil.parseBrowser(s) );
        
        // 微信浏览器
        s = "mozilla/5.0 (iphone; cpu iphone os 11_4_1 like mac os x) applewebkit/605.1.15 (khtml, like gecko) mobile/15g77 micromessenger/6.7.2 nettype/4g language/zh_cn";
        System.out.println( URLUtil.parseBrowser(s) );
        
        // 搜狗
        s = "mozilla/5.0 (windows nt 6.1) applewebkit/537.36 (khtml, like gecko) chrome/58.0.3029.110 safari/537.36 se 2.x metasr 1.0";
        System.out.println( URLUtil.parseBrowser(s) );
        
        // edge
        s = "mozilla/5.0 (windows nt 10.0; win64; x64) applewebkit/537.36 (khtml, like gecko) chrome/64.0.3282.140 safari/537.36 edge/17.17134";
        System.out.println( URLUtil.parseBrowser(s) );
        
        System.out.println( URLUtil.parseBrowser("unkown") );
        System.out.println( URLUtil.parseBrowser(null) );
	}

	@Test
    public void testGetResourceFileUrl() {
		URLUtil.getResourceFileUrl(null);
		URLUtil.getResourceFileUrl("");
		
        URL url = URLUtil.getResourceFileUrl("log4j.properties");
        System.out.println(url);
        System.out.println(url.getPath()); 
        
        String path = "/D:/Temp/apps/pms/WEB-INF/classes/cn/";
        int lastIndex = path.lastIndexOf("WEB-INF");
        try {
            path = path.substring(0, lastIndex) + "core";
            url = new URL(url, path);
        } catch (MalformedURLException e) {
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + path);
            e.printStackTrace();
        }
        System.out.println(url);
        System.out.println(url.getPath());
        
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println(URLUtil.getClassesPath().getPath());
    }
 
    @Test
    public void testGetWebFileUrl() {
    	URL url = null;
        try {
            url = URLUtil.getWebFileUrl("log4j.properties");
            System.out.println(url);
            System.out.println(url.getPath()); 
        } catch (Exception e) {
            Assert.assertFalse("WEB-INF path not exists", false);
        }
        
        try {
            URLUtil.getURL(url.getPath(), "httpX");
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().indexOf("unknown protocol: httpx") >= 0);
        }
    }
    
    @Test
    public void testGetClassesPath() {
        URL url = URLUtil.getClassesPath();
        System.out.println(url);
        System.out.println(url.getPath()); 
    }
    
    @Test
    public void testParseQueryString() throws UnsupportedEncodingException {
        String queryString = "rpName=%E7%BB%9F%E8%AE%A1%E6%9C%8D%E5%8A%A1%E8%AE%BF%E9%97%AE%E5%8D%A0%E6%AF%94&xx=12&&yy&&name=测%试123&&kk=null&&ss=undefined";
        Map<String, String> result = URLUtil.parseQueryString(queryString );
        
        Assert.assertTrue( result.size() == 3 );
        Assert.assertEquals("统计服务访问占比", result.get("rpName")); 
        Assert.assertEquals("测%试123", result.get("name")); 
        
        Assert.assertTrue( URLUtil.parseQueryString(null).isEmpty() );
        Assert.assertTrue( URLUtil.parseQueryString(" ").isEmpty() );
        
        Assert.assertEquals("fields=distinct plate_num as name", URLDecoder.decode("fields=distinct%20plate_num%20as%20name", "UTF-8"));
        Assert.assertEquals("fields=distinct plate_num as name", URLDecoder.decode("fields=distinct plate_num as name", "UTF-8"));
        
        // test encode 两次
        String s = URLEncoder.encode("x=撒%旦", "UTF-8");
        System.out.println( s );
		String ss = URLEncoder.encode(s, "UTF-8");
        System.out.println( ss );
		Assert.assertEquals("撒%旦", URLUtil.parseQueryString( ss ).get("x"));
		
		// test 加号
		Assert.assertEquals("+1-", URLUtil.parseQueryString( "x=+1-" ).get("x"));
    }
}
