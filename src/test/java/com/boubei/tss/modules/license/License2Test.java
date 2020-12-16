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
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.util.URLUtil;

public class License2Test {
 
	@Test
    public void testOther() throws Exception {
		
		List<License> licenses = new ArrayList<>();
		
		File file = new File(URLUtil.getResourceFileUrl("cpu2.txt").getPath());
		LicenseManager.getInstance().checkLicense(file, licenses );
		
		file = new File(URLUtil.getResourceFileUrl("cpu3.txt").getPath());
		LicenseManager.getInstance().checkLicense(file, licenses );
		
		Assert.assertTrue(licenses.isEmpty());
	}
}

