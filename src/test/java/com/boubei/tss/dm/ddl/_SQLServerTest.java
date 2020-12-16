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

public class _SQLServerTest {

	@Test
	public void test1() {
		_Database db = new _SQLServer( null );
		
		String sql = "select * from t order by id";
		Assert.assertEquals("select top 100 t.* from ( select ROW_NUMBER() over( order by id ) AS rn, x.* from (select * from t ) x ) t where t.rn > 0", 
				db.toPageQuery(sql, 1, 100));
		
		sql = "select * from t ";
		Assert.assertEquals("select top 100 t.* from ( select ROW_NUMBER() over(  order by (select 0)  ) AS rn, x.* from (select * from t ) x ) t where t.rn > 0", 
				db.toPageQuery(sql, 1, 100));
	}
}
