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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.um.service.IRoleService;
import com.boubei.tss.um.service.IUserService;
import com.boubei.tss.util.EasyUtils;

import junit.framework.Assert;

public class _RecorderDomainTest extends AbstractTest4DM {
	
	@Autowired IRoleService roleService;
    @Autowired IUserService userService;
    @Autowired IGroupService groupService;
	
	@Autowired LogService logService;
	@Autowired RecordService recordService;
	@Autowired _Recorder recorder;
	
	Long recordId;
	
	private void initTable(String tableName) {
		super.init();
		
		String tblDefine = "[ {'label':'门店', 'code':'f1', 'type':'string'}," +
        		"	{'label':'日期', 'code':'f2', 'type':'date'}," +
        		"	{'label':'金额', 'code':'f3', 'type':'number'}," +
        		"	{'label':'提成', 'code':'f4', 'type':'int', 'role2': '老板'}," +
        		"	{'label':'客户', 'code':'f5', 'type':'string'}" +
        		"]";
		
		Record record = new Record();
		record.setName("record" + tableName);
		record.setType(1);
		record.setParentId(0L);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable(tableName);
		record.setDefine(tblDefine);
		
		recordService.createRecord(record);
		recordId = record.getId();
	}

	@Test
	public void test() {
		initTable("tj_13");
 
        // 新建两个企业用户组
        Group pGroup = new Group();
        pGroup.setParentId(UMConstants.DOMAIN_ROOT_ID);
        pGroup.setName("卜贝");
        pGroup.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(pGroup , "", "");
        
        pGroup.setDomain( pGroup.getName() );
        commonDao.update(pGroup);
        Assert.assertEquals(pGroup.getDomain(), pGroup.getName());
        
        Group sGroup = new Group();
        sGroup.setParentId(UMConstants.DOMAIN_ROOT_ID);
        sGroup.setName("卜数");
        sGroup.setGroupType( Group.MAIN_GROUP_TYPE );
        groupService.createNewGroup(sGroup , "", "");
        
        sGroup.setDomain( sGroup.getName() );
        commonDao.update(pGroup);
        Assert.assertEquals(sGroup.getDomain(), sGroup.getName());
        
        // 管理员直接在主组下新增用户
        User jk = new User();
        jk.setLoginName("JK2");
        jk.setUserName("JK");
        jk.setPassword("123456");
        jk.setGroupId(pGroup.getId());
        userService.createOrUpdateUser(jk , pGroup.getId()+"", "-1"); // 给与Admin，可以不用再单独授权录入表
        
        User Jenny = new User();
        Jenny.setLoginName("Jenny2");
        Jenny.setUserName("Jenny");
        Jenny.setPassword("123456");
        Jenny.setGroupId(sGroup.getId());
        userService.createOrUpdateUser(Jenny , sGroup.getId()+"", "-1");
        
        // start test
        super.logout();
        super.login(jk.getId(), jk.getLoginName());
        
        for(int i = 1; i <= 2; i++) {
			request = new MockHttpServletRequest();
			request.addParameter("_version", String.valueOf(i + 2));
			request.addParameter("f2", "2015-04-05");
			request.addParameter("f3", "12.0");
			request.addParameter("f4", "" + i);
			
			recorder.create(request, response, recordId);
		}
        
        super.logout();
        super.login(Jenny.getId(), Jenny.getLoginName());
        
        for(int i = 1; i <= 3; i++) {
			request = new MockHttpServletRequest();
			request.addParameter("_version", String.valueOf(i + 2));
			request.addParameter("f2", "2018-04-05");
			request.addParameter("f3", "22.0");
			request.addParameter("f4", "" + i);
			
			recorder.create(request, response, recordId);
		}
		
		request.removeAllParameters();
		request.addParameter("domain", "卜数");
		List<?> list = (List<?>) recorder.showAsJSON(request, recordId, 1);
		Assert.assertEquals(3, list.size()); 
		
		super.logout();
        super.login(-1L, "Admin");
        
        request.removeAllParameters();
		request.addParameter("domain", "卜贝");
		list = (List<?>) recorder.showAsJSON(request, recordId, 1);
		Assert.assertEquals(2, list.size());

		recorder.showAsGrid(request, response, recordId, 1);
		
		super.login(Jenny.getId(), Jenny.getLoginName());
		RecordField rf = new RecordField();
		rf.setId(null);
        rf.setCode("f2");
        rf.setTable("tj_13");
        rf.setUser(null);
        rf.setLabel("自定义F2");
    	rf.setType(null);
    	rf.setOptions(null);
    	rf.setDefaultValue(null);
    	rf.setCheckReg(null);
    	rf.setUnique(null);
    	rf.setReadonly(null);
    	rf.setAlign(null);
    	rf.setCwidth(null);
    	rf.setNullable(null);
    	rf.setRole1(null);
    	rf.setRole2(null);
    	rf.setUdf1(null);
    	rf.setUdf2(null);
    	rf.setUdf3(null);
    	rf.setUser_org(null);
    	rf.setSeqNo(1);
    	rf.containsRole1(Arrays.asList(1L, 3L));
    	rf.containsRole2(Arrays.asList(1L, 3L));
    	EasyUtils.obj2Json(rf);
    	
    	_Database db = recorder.getDB(recordId);
    	Assert.assertEquals("日期", db.cnm.get("f2"));
    	
    	String sql = "INSERT INTO `dm_record_field` (`creator`, `domain`, `code`, `label`, `tbl`, `options`, `user`, `createTime`) VALUES (?,?,?,?,?,?,?,?)";
    	Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
    	paramsMap.put(1, Environment.getUserCode());
    	paramsMap.put(2, Environment.getDomain());
    	paramsMap.put(3, "f2");
    	paramsMap.put(4, "自定义F2");
    	paramsMap.put(5, "tj_13");
    	paramsMap.put(6, null);
    	paramsMap.put(7, null);
    	paramsMap.put(8, new Date());
		SQLExcutor.excute(sql, paramsMap , DMConstants.LOCAL_CONN_POOL);
		
		paramsMap.put(6, "选项A 选项B 选项C");
		SQLExcutor.excute(sql, paramsMap , DMConstants.LOCAL_CONN_POOL);
		
		_Database db2 = recorder.getDB(recordId);
		Assert.assertEquals("自定义F2", db2.cnm.get("f2"));
	}
}
