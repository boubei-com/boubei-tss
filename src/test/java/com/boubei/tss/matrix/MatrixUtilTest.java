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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MatrixUtilTest {
	
	@Test
	public void test() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("f1", "测试中文");
		
		MatrixUtil.remoteRecord(-8L, params );
	}

}
