/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ext;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.Commit;

import com.boubei.tss.AbstractTest4DM;


@Commit
public class CleanPermissionJobTest extends AbstractTest4DM {

	@Test
	public void testJob() {
		
		try {
			CleanPermissionJob job = new CleanPermissionJob();
        	job.excuteJob("X");
        	Assert.assertTrue( job.needSuccessLog() );
		} 
		catch(Exception e) {
			log.error(e);
			Assert.assertFalse(true);
        }
	}
}
