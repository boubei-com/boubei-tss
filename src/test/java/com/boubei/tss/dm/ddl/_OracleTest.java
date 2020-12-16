/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ddl;

import org.junit.Assert;
import org.junit.Test;

public class _OracleTest {

	@Test
	public void test1() {
		_Database db = new _Oracle( null );
		
		String sql = "select * from t";
		Assert.assertEquals("SELECT * FROM ( SELECT t.*, ROWNUM RN FROM (\n " +sql+ " \n) t WHERE ROWNUM <= 100) WHERE RN > 0",
				db.toPageQuery(sql, 1, 100));
	}
}
