/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.permission.RecordResource;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.log.LogQueryCondition;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

public class RecordTest extends AbstractTest4DM {
    
    @Autowired private RecordAction action;
    @Autowired private LogService logService;
    @Autowired private _Recorder _recorder;
    @Autowired private RecordService service;
    
	@Test
	public void test1() {
		super.logout();
		request.addParameter("parentId", "_root");
		action.getRecord(request, response, Record.TYPE1);
	}
    
    @Test
    public void testRecordCRUD() {
    	
    	List<Record> list = service.getAllRecords();
    	for(Record t : list ) {
    		service.delete(t.getId());
    	}
    	
    	Long recordId = -92L;
    	try {
    		service.getRecord(recordId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
		}
    	Assert.assertEquals(Integer.valueOf(1), service.getAttachSeqNo(recordId, 1L));
        
        HttpServletResponse response = Context.getResponse();
        MockHttpServletRequest  request = new MockHttpServletRequest();
        
        request.addParameter("parentId", "_root");
        action.getRecord(request, response, Record.TYPE0);
        
        request.removeParameter("parentId");
        request.addParameter("parentId", Record.DEFAULT_PARENT_ID.toString());
        action.getRecord(request, response, Record.TYPE0);
        
        Record group1 = new Record();
        group1.setType(Record.TYPE0);
        group1.setParentId(Record.DEFAULT_PARENT_ID);
        group1.setName("record-group-1");
        action.saveRecord(response, group1);
        action.saveRecord(response, group1); // update group
        
        Record group11 = new Record();
        group11.setType(Record.TYPE0);
        group11.setParentId(group1.getId());
        group11.setName("record-group-1-1");
        action.saveRecord(response, group11);
        
        Record group12 = new Record();
        group12.setType(Record.TYPE0);
        group12.setParentId(group1.getId());
        group12.setName("record-group-1-2");
        action.saveRecord(response, group12);
        
        Record group2 = new Record();
        group2.setType(Record.TYPE0);
        group2.setParentId(Record.DEFAULT_PARENT_ID);
        group2.setName("record-group-2");
        group2.setRemark("open it");
        action.saveRecord(response, group2);
        
        group2.setDisabled(null);
        Assert.assertEquals(ParamConstants.FALSE, group2.getDisabled());
        
        // test create page
        Record record0 = new Record();
        record0.setType(Record.TYPE1);
        record0.setParentId(group1.getId());
        record0.setName("record-00");
        record0.setDatasource(DMConstants.LOCAL_CONN_POOL);
        record0.setTable("dm_empty");
        record0.setDefine("[{'label':'x', 'code':'x', 'type':'string'}]");
        record0.setCustomizePage("../xx0.html");
        record0.setIcon("<img src='/tss/images/icon_refresh.gif'/>");
        record0.setWxicon("/tss/images/icon_refresh.gif'");
        record0.setMobilable(ParamConstants.FALSE);
        record0.setLogicDel(ParamConstants.FALSE);
        record0.setWxurl(null);
        record0.setShowCreator(ParamConstants.FALSE);
        record0.setIgnoreDomain(ParamConstants.FALSE);
        action.saveRecord(response, record0);
        log.debug( EasyUtils.obj2Json(record0) );
        Assert.assertEquals("images/page_0.gif", record0.getAttributes().get("icon"));
        
        // test create record
        Record record1 = new Record();
        record1.setType(Record.TYPE1);
        record1.setParentId(group1.getId());
        record1.setName("record-1");
        record1.setDatasource(DMConstants.LOCAL_CONN_POOL);
        record1.setTable("t_" + System.currentTimeMillis());
        record1.setDefine("[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'f2', 'type':'string'}," +
        		"{'label':'时间', 'code':'f3', 'type':'datetime', 'nullable':'false'}]");
        record1.setCustomizePage("../xx.html");
        record1.setCustomizeJS(" function() f1() { } ");
        record1.setCustomizeGrid(" function() gf1() { } ");
        record1.setBatchImp(ParamConstants.TRUE);
        record1.setCustomizeTJ("");
        record1.setRemark("test record");
        action.saveRecord(response, record1);
        Assert.assertEquals(record1.getResourceType(), new RecordResource().getResourceType());
        
		Assert.assertNotNull(action.getRecordID(record1.getName()));
		Assert.assertEquals( record1.getId(), _recorder.getRecordIDs(record1.getName()).get(0) );
		
		try {
			action.getRecordID("not-Exsits");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertTrue("没有找到名为【not-Exsits】的数据表", true);
        }
        
        // test update record
		Record record1_ = new Record();
		BeanUtil.copy(record1_, record1);
		record1_.setTable("x_tbl_test");
        action.saveRecord(response, record1_);
        
        action.getAllRecordTree(response);
        action.getAllRecordGroups(response);
        action.getAllRecords(response);
        
        request.addParameter("recordId", record1.getId().toString());
        action.getRecord(request, response, Record.TYPE1);
        
        action.sort(response, group1.getId(), group2.getId(), 1);
        action.move(response, record1.getId(), group2.getId());
        action.move(response, group2.getId(), group1.getId());
        
        action.getAllRecordTree(response);
        Assert.assertEquals(6, action.getAllRecords(response).size());
        
        // test permission
        action.getOperations(response, record1.getId());
        
        action.startOrStop(response, record1.getId(), ParamConstants.TRUE);
        Assert.assertEquals(5, action.getAllRecords(response).size());
        try{
	        _recorder.getDefine(request, record1.getId());
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("该数据录入已被停用，无法再录入数据！", true);
	    }
        action.startOrStop(response, record1.getId(), ParamConstants.FALSE);
        _recorder.getDefine(request, record1.getId());
        
        Record group3 = new Record();
        group3.setType(Record.TYPE0);
        group3.setParentId(Record.DEFAULT_PARENT_ID);
        group3.setName("record-group-3");
        action.saveRecord(response, group3);
        action.startOrStop(response, group3.getId(), ParamConstants.TRUE);
        action.move(response, record1.getId(), group3.getId());
        
        action.delete(response, record1.getId());
        action.getAllRecordTree(response);
        
        try {
            Thread.sleep(1000); // 等待日志异步输出完毕
        } catch (InterruptedException e) {
        }
        
        LogQueryCondition condition = new LogQueryCondition();
        condition.setOperateTimeFrom(new Date(System.currentTimeMillis() - 1000*3600*3));
        PageInfo logsInfo = logService.getLogsByCondition(condition);
        List<?> logs = logsInfo.getItems();
        for(Object temp : logs) {
            log.debug(temp);
        }
        
        try {
        	logout();
	        action.delete(response, group1.getId());
	        Assert.fail("should throw exception but didn't.");
	    } catch (Exception e) {
	    	Assert.assertTrue("没有权限，删除失败！", true);
	    }
        action.getAllRecordTree(response);
        action.getAllRecords(response);
    }
}
