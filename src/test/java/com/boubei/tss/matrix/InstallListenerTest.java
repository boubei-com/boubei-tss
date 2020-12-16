/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.matrix;

import java.io.File;

import org.junit.Test;

import com.boubei.tss.modules.license.LicenseFactory;
import com.boubei.tss.modules.license.LicenseManager;
import com.boubei.tss.util.FileHelper;

public class InstallListenerTest {
	
	@Test
	public void test() {
		
		FileHelper.deleteFilesInDir("license", new File(LicenseFactory.LICENSE_DIR));
		LicenseManager.getInstance().licenses = null;
		
		new InstallListener();
		
		new InstallListener();
	}

}
