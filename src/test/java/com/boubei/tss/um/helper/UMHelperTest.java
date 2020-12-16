/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Application;
import com.boubei.tss.um.entity.Operation;
import com.boubei.tss.um.entity.ResourceType;
import com.boubei.tss.um.permission.dispaly.TreeNodeOption4Permission;

public class UMHelperTest {

	@Test
	public void test1() {
		new GroupTreeParser().parse(null);
		
		new ApplicationTreeParser().parse( new Object[]{ null, null, null, null} );
		
		List<Application> apps = new ArrayList<Application>();
		List<?> resourceTypes = new ArrayList<ResourceType>();;
		List<?> operations = new ArrayList<Operation>();;
		
		Application app1 = new Application();
		app1.setApplicationType(UMConstants.OTHER_SYSTEM_APP);
		apps.add(app1);
		new ApplicationTreeParser().parse( new Object[]{ apps, resourceTypes, operations, null} );
		
		UMQueryCondition uc = new UMQueryCondition();
		uc.setCertificateNo("xx");
		uc.setEmployeeNo("yy");
		Assert.assertEquals("%xx%", uc.getCertificateNo());
		Assert.assertEquals("%yy%", uc.getEmployeeNo());
		
		MessageQueryCondition mc = new MessageQueryCondition();
		Assert.assertNull(mc.getTitle());
		Assert.assertNull(mc.getContent());
		
		TreeNodeOption4Permission tnp = new TreeNodeOption4Permission( new Operation() {
			private static final long serialVersionUID = 1L;
			public Map<String, Object> getOptionAttributes() {
				return null;
			}
		});
		Assert.assertEquals("", tnp.toXml());
	}
	
}
