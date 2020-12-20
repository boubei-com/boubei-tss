/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.util.FileHelper;

import org.junit.Assert;

public class DataExportTest extends AbstractTest4DM {
	
	@Test
	public void test() {

		List<String> cnFields = Arrays.asList("h1", "h2");

		String tmpDir = FileHelper.ioTmpDir() + "temp";
		DataExport.exportCSV(tmpDir + "/456.csv", "h1,h2\n1,2");
		
		List<Object[]> data2 = new ArrayList<Object[]>();
		data2.add(new Object[] { 1, 2 } );
		DataExport.exportCSV(data2, cnFields); 
		DataExport.exportCSV(data2, null); 
		
		Object[][] result2 = DataExport.convertList2Array(null);
		Assert.assertEquals(0, result2.length);
		
		try {
			DataExport.exportCSV(tmpDir + "/inv_2.xlsx", null);
			Assert.fail("should throw ex");
		 } 
		 catch (Exception e) {
		 }
		
		try {
			DataExport.exportCSV(tmpDir + "/inv_2.xlsx", data2, null, null);
			Assert.fail("should throw ex");
		 } 
		 catch (Exception e) {
		 }
	}
}

