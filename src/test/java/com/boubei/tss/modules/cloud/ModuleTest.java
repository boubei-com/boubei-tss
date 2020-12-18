/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.cloud;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.cloud.entity.DomainInfo;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.modules.cloud.entity.ModuleUser;
import com.boubei.tss.modules.menu.INavigatorService;
import com.boubei.tss.modules.menu.Navigator;
import com.boubei.tss.modules.menu.PortalConstants;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.SubAuthorizeAction;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

import junit.framework.Assert;

public class ModuleTest extends AbstractTest4DM {
	
	@Autowired ModuleAction action;
	@Autowired SubAuthorizeAction saAction;
	@Autowired IGroupService groupService;
	@Autowired INavigatorService menuService;
	
	@Test
	public void test() {
		
		Record rc1 = null;
		Report rp1 = null;
		Navigator menu1 = null;
		for(int i = 0; i <= 12; i++) {
			rc1 = new Record();
			rc1.setType(1);
			rc1.setName("rc" + i);
			rc1.setParentId(Record.DEFAULT_PARENT_ID);
			commonDao.create(rc1);
			_TestUtil.mockPermission("dm_permission_record", rc1.getName(), rc1.getId(), UMConstants.DOMAIN_ROLE_ID, Record.OPERATION_VDATA, 2, 0, 0);
			
			rp1 = new Report();
			rp1.setType(1);
			rp1.setName("rp" + i);
			rp1.setDisabled(ParamConstants.FALSE);
			rp1.setParentId(Report.DEFAULT_PARENT_ID);
			commonDao.create(rp1);
			_TestUtil.mockPermission("dm_permission_report", rp1.getName(), rp1.getId(), UMConstants.DOMAIN_ROLE_ID, Report.OPERATION_VIEW, 2, 0, 0);
			
			Long pId = menu1 == null ? PortalConstants.ROOT_ID : menu1.getId();
			menu1 = new Navigator();
			menu1.setCode("menu" + i);
			menu1.setName(menu1.getCode());
			menu1.setType(Navigator.TYPE_MENU);
			menu1.setParentId(pId);
			menuService.saveNavigator(menu1);
			_TestUtil.mockPermission("portal_permission_navigator", menu1.getName(), menu1.getId(), UMConstants.DOMAIN_ROLE_ID, PortalConstants.NAVIGATOR_VIEW_OPERRATION, 2, 0, 0);
		}
		
		ModuleDef module = new ModuleDef();
		module.setId(null);
		module.setKind("记账,进销存");
		module.setModule("店铺管理");
		module.setCode("MD-001");
		module.setRoles("-8,-9, 12");
		module.setInner_base_role("1");
		module.setReports("1,2,3," + rp1.getId());
		module.setRecords("4,5,6," + rc1.getId());
		module.setMenus("1,2,3" + menu1.getId());
		
		module.setProduct_class("");
		module.setResource("pages/ywms,pages/images");
		module.setStatus("opened");
		module.setDescription("简介");
		module.setSeqno(100);
		module.setTry_days(100);
		module.setAccount_limit("1,99");
		module.setMonth_limit("12,36");
		module.setExperience_gold("10%");
		module.setRemark("test");
		commonDao.createObject(module);
		Long moduleId = module.getId();
		Assert.assertEquals(module.getPK(), moduleId);
		BeanUtil.getProperties(module);
		
		Assert.assertNull(module.cal_experience_gold(0D));
		Assert.assertEquals(10D, module.cal_experience_gold(100D));
		module.setExperience_gold("10.8");
		Assert.assertEquals(10.8D, module.cal_experience_gold(100D));
		
		action.listAvaliableModules();
		
		// no permission, only domainAdmin can select|unselect modules
		try {
			action.selectModule(moduleId);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.MODULE_1, e.getMessage());
        }
		
		// mock domian role
		Context.initRequestContext(request);
		HttpSession session = request.getSession();
        Object userRights = session.getAttribute(SSOConstants.USER_RIGHTS_L);
        @SuppressWarnings("unchecked")
		List<Long> ownRoles = (List<Long>) EasyUtils.checkNull(userRights, new ArrayList<Long>());
        ownRoles.add( UMConstants.DOMAIN_ROLE_ID );
        session.setAttribute(SSOConstants.USER_RIGHTS_L, ownRoles);
		        
		action.selectModule(moduleId);
		List<?> list = action.listSelectedModules();
		Assert.assertEquals(1, list.size());
		
		Assert.assertTrue( groupService.findEditableRoles().size() >= 2 );
		
		// 为module添加（删除）新角色
		module.setRoles("-8,12,-1");
		commonDao.update(module);
		action.refreshModuleUserRoles(moduleId);
		Assert.assertTrue( groupService.findEditableRoles().size() >= 3 );
		
		List<?> saList = saAction.listMySubauth(response);
		Assert.assertEquals(1, saList.size());
		
		// 取消试用
		action.unSelectModule(moduleId);
		list = action.listSelectedModules();
		Assert.assertEquals(0, list.size());
		
		// test list resources
		action.listResource("dm_report");
		action.listResource("dm_record");
		action.listResource("portal_navigator");
		
		menuService.getMenuTree(0L);
		menuService.getMenuTree(1L);
		
		Assert.assertEquals(0, action.limitResource().length);
		
		super.initDomain();
		super.login(domainUser);
		Assert.assertEquals(3, action.limitResource().length);
		action.selectModule(moduleId);
		action.limitResource();
		
		menuService.getMenuTree(0L);
		menuService.getMenuTree(1L);
		
		List<?> roleIds = commonDao.getEntities("select roleId from RoleUser where moduleId = ?1 and userId = ?2", moduleId, domainUser.getId());
		Assert.assertEquals(3, roleIds.size());
		Assert.assertTrue(roleIds.contains(-8L));
		Assert.assertTrue(roleIds.contains(12L));
		Assert.assertTrue(roleIds.contains(-1L));
		
		module.setRoles("-8,-9,12");
		module.setRecords("");
		module.setReports("");
		module.setMenus("");
		commonDao.update(module);
		action.refreshModuleUserRoles(moduleId);
		roleIds = commonDao.getEntities("select roleId from RoleUser where moduleId = ?1 and userId = ?2", moduleId, domainUser.getId());
		Assert.assertEquals(3, roleIds.size());
		Assert.assertTrue(roleIds.contains(-8L));
		Assert.assertTrue(roleIds.contains(-9L));
		Assert.assertTrue(roleIds.contains(12L));
		Assert.assertTrue(!roleIds.contains(-1L)); // 角色被收回
		
		action.limitResource();
		
		domainInfo.setWhite_list("rc_1,rp_2");
		domainInfo.setBlack_list("rc_3,rp_4");
		commonDao.update(domainInfo);
		Assert.assertTrue( action.listAllResource().size() > 0 );
		
		action.unSelectModule(moduleId);
	}

	@Test
	public void test2() {
		ModuleUser mu = new ModuleUser(1L, 1L);
		mu.setId( mu.getId() );
		mu.getPK();
		EasyUtils.obj2Json(mu);
		
		DomainInfo info = new DomainInfo();
		info.setPayment_code("img/1.png");
		info.setName("CX");
		info.setGgpic("gg.png");
		info.setLocation("浙江省杭州市下沙 华美");
		info.setLogo("logo.png");
		info.setPrefix("CX");
		info.setSms_key("1234567890");
		info.setSms_secret("1234567890");
		info.setSms_sign("abcdefg");
		info.setSms_verify("123456");
		info.setSms_tosender(null);
		info.setSms_toreceiver(null);
		info.setUdf1("udf1");
		info.setUdf2("udf2");
		info.setUdf3("udf3");
		info.setType("EFF");
		info.setRebate(0.12D);
		info.setSub_domains(null);
		info.setId(null);
		
		DomainInfo info2 = new DomainInfo();
		info2.copy(info);
		
		log.debug( EasyUtils.obj2Json(info) );
	}
}
