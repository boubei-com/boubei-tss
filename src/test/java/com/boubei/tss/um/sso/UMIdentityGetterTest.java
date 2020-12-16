/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.IdentityGetter;
import com.boubei.tss.um.AbstractTest4UM;

public class UMIdentityGetterTest extends AbstractTest4UM {
	
	@Test
	public void test() {
		 IdentityGetter ig = new UMIdentityGetter();
		 ig.getOperator(Environment.getUserId());
		 Assert.assertFalse( ig.indentify(null, "123456") );
	}

}
