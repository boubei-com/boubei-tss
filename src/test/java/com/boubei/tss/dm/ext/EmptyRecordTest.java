package com.boubei.tss.dm.ext;

import org.junit.Test;

import com.boubei.tss.dm.record.file.RecordImpTL;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.util.BeanUtil;

import junit.framework.Assert;

public class EmptyRecordTest {
	
	@Test
	public void test() {
		EmptyRecord o = new EmptyRecord();
		o.setId(1L);
		o.setX("test");
		o.getPK();
		o.getX();
		
		Assert.assertTrue( o.equals(o) );
		Assert.assertFalse( o.equals(null) );
		Assert.assertFalse( o.equals(this) );
		
		EmptyRecord o2 = new EmptyRecord();
		o2.setId(12L);
		Assert.assertFalse( o.equals(o2) );
		
		new Report(null, null, null);
		new Report(null, null, 1L);
		
		RecordImpTL tl1 = new RecordImpTL();
		RecordImpTL tl2 = new RecordImpTL();
		BeanUtil.copy(tl1, tl2);
		tl1.getPK();
	}

}
