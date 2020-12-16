/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.param;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.util.EasyUtils;

public class ParamActionTest extends AbstractTest4F {
	
	@Autowired private ParamAction action;
	@Autowired protected ParamDao paramDao;

    @Test
	public void testParamAction() {
    	ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, "app.code", "系统Code", "TSS");
    	
		// test param init view
		Object result = action.getAllValue("sysTitle,app.code");
		String json = EasyUtils.obj2Json(result);
		log.info(json);
		
		request.addParameter("json", json);
		try {
			action.saveParams(request);
		} catch (Exception e) {
			Assert.fail();
		}
    	
		Param paramGroup = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "测试参数组1");
        Param comboParam = ParamManager.addComboParam(paramGroup.getId(), "book", "可选书籍");
        
        ParamManager.addParamItem(comboParam.getId(), "Thinking in JAVA", "Thinking in JAVA", ParamConstants.COMBO_PARAM_MODE);
        ParamManager.addParamItem(comboParam.getId(), "Effictive JAVA", "Effictive JAVA", ParamConstants.COMBO_PARAM_MODE);
        ParamManager.addParamItem(comboParam.getId(), "Design Pattern", "Design Pattern", ParamConstants.COMBO_PARAM_MODE);
        
        Param paramGroup2 = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "测试参数组2");
        Param treeParam = ParamManager.addTreeParam(paramGroup2.getId(), "group", "用户组织");
        
        Param temp = ParamManager.addParamItem(treeParam.getId(), "group1", "研发部", ParamConstants.TREE_PARAM_MODE);
        ParamManager.addParamItem(temp.getId(), "group2", "IT部", ParamConstants.TREE_PARAM_MODE);
        ParamManager.addParamItem(treeParam.getId(), "group3", "财务部", ParamConstants.TREE_PARAM_MODE);
        
        Assert.assertTrue( paramDao.getParentsById(Param.class.getName(), -11L, paramGroup2.getId()).isEmpty() );
        Assert.assertTrue( paramDao.getParentsById(Param.class.getName(), temp.getId(), paramGroup2.getId()).size() == 3 );
        Assert.assertTrue( paramDao.getParentsById(Param.class.getName(), temp.getId(), -11L).size() >= 3 );
        
        try {
        	ParamManager.addParamItem(treeParam.getId(), "group3", "财务部", ParamConstants.TREE_PARAM_MODE);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("参数已存在，不要重复创建! ", true);
		}
        
        temp.setDescription("update param item");
        action.saveParam(response, temp); // update param item
        
		action.get2Tree(response);
		
		action.copyParam(response, treeParam.getId(), paramGroup.getId().toString());
		try {
			action.copyParam(response, paramGroup2.getId(), "_root");
		} catch (Exception e) { }
		
		action.getCanAddParamsTree(response, treeParam.getId());
		action.getCanAddParamsTree(response, paramGroup.getId());
		
		request.addParameter("parentId", "_root");
		action.getParamInfo(request, response, ParamConstants.NORMAL_PARAM_TYPE);
		action.getParamInfo(request, response, ParamConstants.GROUP_PARAM_TYPE);
		action.getParamInfo(request, response, ParamConstants.ITEM_PARAM_TYPE);
		
		request.removeParameter("parentId");
		request.addParameter("mode", ParamConstants.SIMPLE_PARAM_MODE.toString());
		request.addParameter("paramId", treeParam.getId().toString());
		action.getParamInfo(request, response, ParamConstants.NORMAL_PARAM_TYPE);
		
        try {
        	action.getParamInfo(request, response, 888);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("参数类型错误，Type=888", true);
		}
		
		action.saveParam(response, comboParam);
		action.initAppConfig(response, "test1", "test1_n", "v1");
		action.initAppConfig(response, "app.code", "app.code", "v2");
		
		assertEquals("<server code=\"test1\" framework=\"tss\" name=\"test1_n\" sessionIdName=\"JSESSIONID\" baseURL=\"v1\"/>", 
				ParamConfig.getAttribute("test1"));
		
		action.saveParamValue(response, "test1", "test test");
		assertEquals("test test", ParamConfig.getAttribute("test1"));
		
		Param paramGroup3 = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "测试参数组3");
		paramGroup3.setSeqNo( paramGroup2.getSeqNo() );
		action.saveParam(response, paramGroup3);
		Param paramGroup4 = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "测试参数组4");
		action.moveParam(response, treeParam.getId(), "_root");
		action.moveParam(response, treeParam.getId(), paramGroup3.getId().toString());
		
		action.sortParam(response, paramGroup.getId(), paramGroup4.getId(), -1);
		action.sortParam(response, paramGroup.getId(), paramGroup2.getId(), 1);
		
		 try {
			 action.sortParam(response, treeParam.getId(), paramGroup3.getId(), -1);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("排序节点和目标节点不属于同一层的节点（父节点不一致），不能排序。", true);
		}
		try {
			action.delParam(response, null);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
		}
		
		action.get2Tree(response);
		
		log.debug(action.getSimpleParam2Json("app.code"));
		log.debug(action.getComboParam2Json(request, "book", false));
		log.debug(action.getComboParam2Json(request, "book", true));
		action.getComboParam2XML(response, "book", false);
		action.getComboParam2XML(response, "group", true);
		
		log.debug(action.getTreeParam2Json("group"));
		log.debug(action.getTreeParam2Json("notExsit"));
		
        try {
        	paramService.getComboParam("app.code");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("不是下拉型参数!", true);
		}
        
        try {
        	ParamManager.getComboParam("app.code");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("获取参数信息失败!", true);
		}
        try {
        	ParamManager.getTreeParam("app.code");
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertTrue("获取参数信息失败!", true);
		}
		
        request.addParameter("valField", "id");
		log.debug(action.getComboParam2Json(request, "notExsit", true));
		log.debug(action.getComboParam2Json(request, "book", true));
		action.getComboParam2XML(response, "notExsit", true);
		action.getComboParam2XML(response, "book", true);
		
		action.startOrStopParam(response, paramGroup.getId(), 1);
		action.startOrStopParam(response, comboParam.getId(), 0);
		
		action.delParam(response, paramGroup.getId());
		action.delParam(response, paramGroup2.getId());
		
		action.get2Tree(response);
		
		_TestUtil.printLogs(logService);
	}
}
