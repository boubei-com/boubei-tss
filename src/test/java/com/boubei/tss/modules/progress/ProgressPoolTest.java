package com.boubei.tss.modules.progress;

import org.junit.Test;

import com.boubei.tss.EX;

import junit.framework.Assert;

public class ProgressPoolTest {
	
	@Test
	public void test1() {
		
//		ProgressPool.checkRepeat();
		
		Progress obj = new Progress(100);
		ProgressPool.putSchedule("xxx", obj );
		
		try {
			ProgressPool.checkRepeat();
		} catch ( Exception e ) {
			Assert.assertEquals(EX.CACHE_6, e.getMessage());
		}
	}

}
