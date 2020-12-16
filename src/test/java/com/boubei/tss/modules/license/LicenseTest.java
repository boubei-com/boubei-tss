/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.license;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.boubei.tss.framework.Config;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

import junit.framework.Assert;

public class LicenseTest {
	
	private String licenceName = "cpu.license";
	
	private final Log log = LogFactory.getLog(getClass());
    
	@Test
    public void testLicense() {
		FileHelper.deleteFilesInDir("key", new File(LicenseFactory.LICENSE_DIR));
		FileHelper.deleteFilesInDir("license", new File(LicenseFactory.LICENSE_DIR));
		LicenseManager.getInstance().licenses = null;
        
        // 第一步：生成公钥、私钥对。公钥公开，注意保管好私钥（如果泄露，则有可能被hacker随意创建license）。
        try {
            LicenseFactory.generateKey();
        } catch (Exception e) {
        	log.error(e);
            Assert.assertFalse(e.getMessage(), false);
        }
        
        // 第二步：根据产品、版本、Mac地址、有效期等信息，签名产生注册号，并将该注册号复制到license文件中。
        License license = null;
        System.out.println(URLUtil.getResourceFileUrl("cpu.license"));
        try {
            license = License.fromConfigFile(licenceName);
            LicenseFactory.sign(license);
        } catch (Exception e) {
        	log.error(e);
        	Assert.assertFalse(e.getMessage(), false);
        }
        
        FileHelper.writeFile(new File(LicenseFactory.LICENSE_DIR + "/" + licenceName), license.toString());
        log.debug("\n" + license + "\n");
        
        // 第三步：利用公钥对license进行合法性验证。可以在软件代码的重要模块中加入下面的验证，比如登录模块等。
        String appCode = Config.getAttribute("application.code").toLowerCase();
		String version = Config.getAttribute("application.version");
		
        LicenseManager manager = LicenseManager.getInstance();
        assertEquals(appCode, license.product);
        assertEquals(version, license.version);
        assertEquals(License.LicenseType.Evaluation.toString(), license.type);
        assertEquals(DateUtil.parse("2022-06-22"), license.expiry);
        assertEquals("Jon.King", license.owner);
        
        Assert.assertTrue( manager.validateLicense(license.product, license.version) );
        Assert.assertFalse( manager.validateLicense(license.product + "2", license.version) );
        
        String licenseType = manager.getLicenseType(license.product, license.version);
		assertEquals(License.LicenseType.Evaluation.toString(), licenseType);
		
		licenseType = manager.getLicenseType(license.product + "2", license.version);
		Assert.assertNull(licenseType);
        
        Assert.assertTrue( License.validate() );
    }
}

