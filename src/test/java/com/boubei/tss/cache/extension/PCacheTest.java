/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cache.extension;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.boubei.tss.AbstractTest4TSS;
import com.boubei.tss.PX;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamManager;

public class PCacheTest extends AbstractTest4TSS {
	
    String poolConfig1 = "{" +
			"\"customizerClass\":\"com.boubei.tss.framework.persistence.connpool.ConnPoolCustomizer\"," +
			"\"poolClass\":\"com.boubei.tss.cache.ReusablePool\"," +
			"\"code\":\"pool_1_pct\"," +
			"\"name\":\"DB连接池-1_pct\"," +
			"\"cyclelife\":\"180000\"," +
			"\"paramFile\":\"H2.properties\"," +
			"\"interruptTime\":\"1000\"," +
			"\"poolSize\":\"10\"," +
			"\"disabled\":\"1\"" +
			"}";
	
	@Test
	public void test() {
		PCache pc = new PCache();
		
		Param p = new Param();
		p.setId(-1L);
		
		pc.afterChange(p);
		
		Param parent = ParamManager.addParamGroup(0L, PX.CACHE_PARAM);
		p = ParamManager.addSimpleParam(parent.getId(), "X-1", "X-1", poolConfig1);
		p.setDisabled(1);
		commonDao.update(p);
		
		pc.afterChange(null);
		
		p.setDisabled(0);
		pc.afterChange(null);
		
		p.setValue("{i'm wrong}");
		try {
			pc.afterChange(p);
            fail("应该发生异常而没有发生");
        } catch (Exception e) {
            assertTrue(true);
        }
	}

}
