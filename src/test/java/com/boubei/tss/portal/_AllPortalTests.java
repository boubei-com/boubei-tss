 /* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.boubei.tss.portal.engine.FMSupportActionTest;
import com.boubei.tss.portal.engine.FreeMarkerParserTest;
import com.boubei.tss.portal.module.ComponentModuleTest;
import com.boubei.tss.portal.module.NavigatorModuleTest;
import com.boubei.tss.portal.module.PortalBrowseTest;
import com.boubei.tss.portal.module.PortalFileOperationTest;
import com.boubei.tss.portal.module.PortalModuleTest;
import com.boubei.tss.portal.zlast.EntityTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	FMSupportActionTest.class,
	FreeMarkerParserTest.class,
	EntityTest.class,
	
	ComponentModuleTest.class,
	NavigatorModuleTest.class,
	PortalBrowseTest.class,
	PortalFileOperationTest.class,
	PortalModuleTest.class,
})
public class _AllPortalTests {
 
}
