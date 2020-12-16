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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.boubei.tss.dm.ddl._DatabaseTest;
import com.boubei.tss.dm.dml.SQLExcutorH2Test;
import com.boubei.tss.dm.dml.SqlConfigTest;
import com.boubei.tss.dm.record.BuildTableTest;
import com.boubei.tss.dm.record.RecordTest;
import com.boubei.tss.dm.record._RecorderTest;
import com.boubei.tss.dm.record.file.ImportCSVTest;
import com.boubei.tss.dm.report.DataServiceListTest;
import com.boubei.tss.dm.report.QueryCacheReportTest;
import com.boubei.tss.dm.report.ReportActionTest;
import com.boubei.tss.dm.report.ScriptParseTest;
import com.boubei.tss.dm.report._ReporterTest;
import com.boubei.tss.dm.report.log.AccessLogTest;
import com.boubei.tss.dm.report.timer.ReportJobTest;
import com.boubei.tss.dm.util.Json2ListTest;
import com.boubei.tss.dm.util.RegexTest;
import com.boubei.tss.modules.DataSourceManagerTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	DMUtilTest.class,
	Json2ListTest.class,
	RegexTest.class,
	AccessLogTest.class,
	_ReporterTest.class,
	DataServiceListTest.class,
	ReportActionTest.class,
	ScriptParseTest.class,
	QueryCacheReportTest.class,
	ReportJobTest.class,
	_DatabaseTest.class,
	_RecorderTest.class,
	BuildTableTest.class,
	ImportCSVTest.class,
	RecordTest.class,
	DataSourceManagerTest.class,
	SqlConfigTest.class,
	SQLExcutorH2Test.class,
	DataExportTest.class,
})
public class _AllDMTests {
 
}
