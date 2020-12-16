/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._H2;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.um.UMConstants;

import junit.framework.Assert;

public class DomainReportTest extends AbstractTest4DM {
	
	@Autowired ReportService reportService;
	@Autowired RecordService recordService;
	@Autowired ReportDao reportDao;

	@Test
	public void test () {
		String tblDefine = 
				"[ " +
        		"	{'label':'名称', 'code':'name'} ," +
        		"	{'label':'收费', 'code':'fee', 'type':'number'}" +
        		"]";
		Record record1 = new Record();
		record1.setName("record-domain-1");
		record1.setType(1);
		record1.setParentId(0L);
		record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record1.setTable("domain_tbl_1");
		record1.setDefine(tblDefine);
		
		recordService.createRecord(record1);
		
		_Database db = new _H2(record1);
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("name", "AAA");
		valuesMap.put("fee", "12.0");
		valuesMap.put("domain", UMConstants.DEFAULT_DOMAIN);
		db.insert(valuesMap );
		
		valuesMap.put("name", "AA");
		valuesMap.put("fee", "10.0");
		valuesMap.put("domain", UMConstants.DEFAULT_DOMAIN);
		db.insert(valuesMap );
		
  		String hql = "select table from Record where type = 1 and datasource=? and table=? ";
  		List<?> list = commonDao.getEntities(hql, DMConstants.LOCAL_CONN_POOL, record1.getTable());
		Assert.assertEquals(1, list.size());
		Assert.assertEquals(record1.getTable(), list.get(0));
		
        Report report = new Report();
        report.setType(Report.TYPE1);
        report.setParentId(Report.DEFAULT_PARENT_ID);
        report.setName("report-domain-1");
        report.setScript(" select name, fee from ${domain_tbl_1} ");
        report.setDatasource(DMConstants.LOCAL_CONN_POOL);
        
        reportService.createReport(report);
		 
        Long reportId = report.getId();
        SQLExcutor ex = reportService.queryReport(reportId , new HashMap<String, String>(), 0, 0, -1);
        Assert.assertEquals(2, ex.result.size());
        
        ex = reportService.queryReport(reportId , new HashMap<String, String>(), 0, 0, -10000);
        Assert.assertEquals(2, ex.result.size());
	}
	
	public static void main(String[] args) {
		System.out.print(  Pattern.compile("from[\\s]*\\$\\{").matcher("select * from ${t1} where 1=1 ").find() );
	}
}
