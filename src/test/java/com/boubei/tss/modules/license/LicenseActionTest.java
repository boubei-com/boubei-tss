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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4TSS;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.um.entity.UserToken;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.FileHelper;

import junit.framework.Assert;

public class LicenseActionTest extends AbstractTest4TSS {
	
	Log log = LogFactory.getLog(getClass());
	
	@Autowired LicenseAction action;
	@Autowired ICommonService commonService;
    
	@Test
    public void testLicense() throws Exception {
        
        // 第一步：生成公钥、私钥对。公钥公开，注意保管好私钥（如果泄露，则有可能被hacker随意创建license）。
        try {
            LicenseFactory.generateKey();
        } catch (Exception e) {
        	log.error(e);
            Assert.assertFalse(e.getMessage(), false);
        }
        
        // 第二步：根据产品、版本、Mac地址、有效期等信息，签名产生注册号，并将该注册号复制到license文件中。
        String expiry = "2022-06-22";
		String product = "tss";
		String owner = "Jon.King";
		String license = action.genLicense(owner, product, "4.3", expiry, null).get("license");
		
		FileHelper.writeFile(new File(LicenseFactory.LICENSE_DIR + "/cpu.license"), license);
        log.debug("\n" + license + "\n");
        
		// 第三步：下载令牌。
        UserToken userToken = new UserToken();
        userToken.setUser(owner);
        userToken.setType("License");
        userToken.setResource(product + "|4.3|Commercial");
        userToken.setExpireTime( DateUtil.parse(expiry) );
        userToken.setToken("");
        commonService.createWithLog(userToken);
        
        Long ut = userToken.getId();
		action.download(response, ut);
		
		response = new MockHttpServletResponse();
		action.download(response, ut);
        
        Assert.assertTrue( action.validate() );
    }
}
