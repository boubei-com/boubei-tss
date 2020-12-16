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

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordAction;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;

public class DataServiceListTest extends AbstractTest4DM {
    
    @Autowired private ReportAction action1;
    @Autowired private RecordAction action2;
 
    @Test
    public void test1() {  
    	
        HttpServletResponse response = Context.getResponse();
        
        Report report0 = new Report();
        report0.setType(Report.TYPE1);
        report0.setParentId(Report.DEFAULT_PARENT_ID);
        report0.setName("report-0");
        report0.setScript(" select id, name from dm_report");
        action1.saveReport(response, report0);
        
        Report report1 = new Report();
        report1.setType(Report.TYPE1);
        report1.setParentId(Report.DEFAULT_PARENT_ID);
        report1.setName("report-1");
        report1.setScript(" select id, name from dm_report where ${testMacro} and createTime > ? " +
        		" <#if param2 != '-1'> and 1=1 </#if> ");
        report1.setParam("[{'label':'x1'},{'label':'x2'}]");
        action1.saveReport(response, report1);
        
        Report group = new Report();
        group.setType(Report.TYPE0);
        group.setParentId(Report.DEFAULT_PARENT_ID);
        group.setName("report-group");
        action1.saveReport(response, group);
        
        Report report2 = new Report();
        report2.setType(Report.TYPE1);
        report2.setParentId(group.getId());
        report2.setName("report-2");
        report2.setScript(" select count(*) cnum from dm_report");
        action1.saveReport(response, report2);
        
        action1.getDataServiceList(response);
        
        if(paramService.getParam(PX.DATA_SERVICE_CONFIG) == null) {
        	String dsVal = "/tss/btr/orgs|分公司列表,/tss/btr/centers|分拨列表,X";
        	ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.DATA_SERVICE_CONFIG, "特殊数据服务", dsVal);
        }
        
        action1.getDataServiceList(response);
        action1.delete(response, report1.getId());
        
        // create record
        Record record1 = new Record();
        record1.setType(Record.TYPE1);
        record1.setParentId(Record.DEFAULT_PARENT_ID);
        record1.setName("record-" + System.currentTimeMillis());
        record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
        record1.setTable("t_" + System.currentTimeMillis());
        record1.setDefine("[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'f2', 'type':'string'}]");
        action2.saveRecord(response, record1);
        
        Record group2 = new Record();
        group2.setType(Record.TYPE0);
        group2.setParentId(Record.DEFAULT_PARENT_ID);
        group2.setName("group2-" + System.currentTimeMillis());
        action2.saveRecord(response, group2);
        
        Record record2 = new Record();
        record2.setType(Record.TYPE1);
        record2.setParentId(group2.getId());
        record2.setName("record2-" + System.currentTimeMillis());
        record2.setDatasource(DMConstants.LOCAL_CONN_POOL);
        record2.setTable("t2_" + System.currentTimeMillis());
        record2.setDefine("[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'name', 'type':'string'}]");
        action2.saveRecord(response, record2);
        
        action1.getDataServiceList(response);
    }
    
}