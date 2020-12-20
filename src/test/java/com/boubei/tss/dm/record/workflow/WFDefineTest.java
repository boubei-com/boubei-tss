/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss._TestUtil;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordAction;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record._Recorder;
import com.boubei.tss.framework.SecurityUtil;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.entity.RoleUser;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MacrocodeCompiler;

import org.junit.Assert;

/**
 * 企业域流程自定义Test
 */
public class WFDefineTest extends AbstractTest4DM {
	
	@Autowired RecordService recordService;
	@Autowired WFService wfService;
	@Autowired _Recorder recorder;
	@Autowired private RecordAction action;
	
	Long recordId;
	Map<String, String> context;
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		SecurityUtil.LEVEL_6 = 6;
	}
	
	private void initTable(String tableName) {
		super.init();
		Logger.getLogger("com.boubei").setLevel(Level.INFO);
		SecurityUtil.LEVEL_6 = 2;
		
		super.initDomain();
		
        List<Map<String, String>> rule = new ArrayList<>();
        Map<String, String> m = new HashMap<>();
        m.put("roleId", role1.getId().toString());
        rule.add(m);
		wfService.getUsers(rule , true);
        
        context = new HashMap<String, String>();
        context.put("role0", role0.getId().toString());
        context.put("role1", role1.getId().toString());
        context.put("role2", role2.getId().toString());
        context.put("role3", role3.getId().toString());
        
        _TestUtil.printEntity(commonDao, RoleUser.class.getName());
        try { Thread.sleep(100L); } catch ( Exception e ) { }
        
        Assert.assertTrue( loginSerivce.getRoleIdsByUserId( jk.getId() ).contains(role1.getId()) );
        Assert.assertTrue( loginSerivce.getRoleIdsByUserId( staff1.getId() ).contains(role0.getId()) );
        Collection<Long> l3 = loginSerivce.getRoleIdsByUserId( ceo.getId() );
		Assert.assertTrue( l3.contains(role3.getId()) );
		
		String tblDefine = "[ " +
				"	{'label':'申请人', 'code':'applier', 'type':'string'}," +
        		"	{'label':'日期', 'code':'fromDay', 'type':'date'}," +
        		"	{'label':'天数', 'code':'days', 'type':'number'}," +
        		"	{'label':'状态', 'code':'state'}," +
        		"   {'label':'实请天数','code':'wf_realdays','type':'number'}" +
        		"]";
		
		Record record = new Record();
		record.setName("请假提报");
		record.setType(Record.TYPE1);
		record.setParentId(Record.DEFAULT_PARENT_ID);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable(tableName);
		record.setDefine(tblDefine);
		String wfDef = FileHelper.readResource("workflow/wf_define1");
		record.setWorkflow( MacrocodeCompiler.run(wfDef, context) );
		record.setLogicDel(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		recordId = record.getId();
		
		// 授权给角色，审批角色需要有浏览权限
		String permissions = role1.getId() + "|01100, " +role2.getId()+ "|01100, " +role3.getId()+ "|01100, " +Anonymous._ID+ "|00100";
        permissionService.saveResource2Roles("tss", Record.RESOURCE_TYPE, recordId, "1", permissions);
	}
	
	@Test
	public void test() {
        
		initTable("t_qj_102");
 
        super.logout();
        super.login(staff1);
        
        // 普通员工请假（自定义前）
        request = new MockHttpServletRequest();
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "5");
		request.addParameter("state", "新建");
		Long itemID1 = (Long) recorder.createAndReturnID(request, recordId);
		
		WFStatus ws = wfService.getWFStatus(recordId, itemID1);
		Assert.assertEquals(2, ws.getTo().split(",").length);
		
		String define = FileHelper.readResource("workflow/wf_define2");
		define = MacrocodeCompiler.run(define, context);
		action.saveWFDef4Domain(recordId, domain, define);
		action.saveWFDef4Domain(recordId, domain, define);
		
		WFDefine wd = action.queryWFDef4Domain(recordId, domain);
		Assert.assertNotNull( wd );
		EasyUtils.obj2Json(wd);
		
		// 普通员工请假（自定义后）
		request = new MockHttpServletRequest();
		request.addParameter("applier", "staff1");
		request.addParameter("from", "2018-07-15");
		request.addParameter("days", "5");
		request.addParameter("state", "新建");
		Long itemID2 = (Long) recorder.createAndReturnID(request, recordId);

		ws = wfService.getWFStatus(recordId, itemID2);
		Assert.assertEquals(1, ws.getTo().split(",").length);
	}
	
	

}
