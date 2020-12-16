/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.engine;

import java.net.URL;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

public class FMSupportActionTest extends FMSupportAction {
	
	@Test
	public void test() {
		Context.setResponse(new MockHttpServletResponse());
		
		super.printHTML(12L, "${html}", true);
		super.printHTML(12L, "${html}", false);
		
		URL url = URLUtil.getWebFileUrl(PortalConstants.PORTAL_MODEL_DIR);
		FileHelper.createDir( url.getPath() + "/_12" );
		FileHelper.createDir( url.getPath() + "/_xx" );
		
		super.getPortalResourcesPath(12L);
	}
}
