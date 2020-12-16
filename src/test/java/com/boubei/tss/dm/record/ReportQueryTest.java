package com.boubei.tss.dm.record;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportQuery;
import com.boubei.tss.modules.sn.SerialNOer;

import junit.framework.Assert;

public class ReportQueryTest extends AbstractTest4DM {
	
	@Test
	public void test() {
		Report report;
		report = new Report();
		report.setName( SerialNOer.get("RP") );
		report.setDatasource( DMConstants.LOCAL_CONN_POOL );
		
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("noCache", "true");
		
		report.setScript( "select * from um_user where loginName = 'Admin'" );
		SQLExcutor ex = ReportQuery.excute(report, paramsMap, 1, 1);
		Assert.assertEquals(1, ex.count);
		
		try {
			paramsMap.put("code", "Admin");
			report.setScript( "select * from um_user where loginName = ?" );
			ex = ReportQuery.excute(report, paramsMap, 1, 1);
			Assert.assertEquals(1, ex.count);
			Assert.fail();
		} catch( Exception e) {
			
		}
		
		paramsMap.put("code", "Admin,Admin");
		report.setScript( "select * from um_user where loginName in (${code})" );
		ex = ReportQuery.excute(report, paramsMap, 1, 1);
		Assert.assertEquals(1, ex.count);
	}

}
