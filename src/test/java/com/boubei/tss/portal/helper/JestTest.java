/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.helper;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.boubei.tss.portal.entity.Structure;

public class JestTest {

	@Test
	public void test() {
		PSTreeTranslator4CreateMenu t = new PSTreeTranslator4CreateMenu(3);
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("type", Structure.TYPE_PORTAL);
		t.translate( attributes );
		
		attributes.put("type", Structure.TYPE_PAGE);
		t.translate( attributes );
	}
}
