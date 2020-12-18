/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.servlet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportService;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.service.IUserService;

public class RegisterTest extends AbstractTest4UM {
	
	@Autowired IUserService userService;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		request = new MockHttpServletRequest(); 
        response = new MockHttpServletResponse();
        
        request.addParameter(SSOConstants.RANDOM_KEY, "1234");
        request.getSession().setAttribute(SSOConstants.RANDOM_KEY, 1234);
	}
    
	@Test
    public void testRegUser() {
        request.addParameter("loginName", "JinPujun");
        request.addParameter("password", "JinPujun");
        request.addParameter("userName", "JinPujun");
        request.addParameter("employeeNo", "JinPujun");
        request.addParameter("sex", "1");
        request.addParameter("email", "jinpujun@hotmail.com");
        request.addParameter("birthday", "1983-06-22");
        request.addParameter("address", "hangzhou zhejiang");
        request.addParameter("telephone", "88888888");
        request.addParameter("postalCode", "317000");
        request.addParameter("passwordQuestion", "");
        request.addParameter("passwordAnswer", "");
        
        Register regServlet = new Register();
        
        try {
            regServlet.init();
            regServlet.doGet(request, response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
            regServlet.destroy();
        }
    }
	
	@Test
    public void testRegBusiness() {
		String domain = "金禾";
        request.addParameter("loginName", "jk@boubei.com");
        request.addParameter("password", "www.boubei.com");
        request.addParameter("userName", domain);
        request.addParameter("domain", domain);
        
        Register regServlet = new Register();
        
        try {
            regServlet.init();
            regServlet.doGet(request, response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
            regServlet.destroy();
        }
        
        try {
        	regServlet.doGet(request, response);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.U_19, domain), e.getMessage());
        }
    }
	
	@Test
    public void testRegBusiness2() {
		String domain = "BD";
        request.addParameter("loginName", "bd@boubei.com");
        request.addParameter("password", "www.boudata.com");
        request.addParameter("userName", domain);
        request.addParameter("domain", domain);
        
        Register regServlet = new Register();
        
        try {
            regServlet.init();
            regServlet.doGet(request, response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
            regServlet.destroy();
        }
        
        try {
        	regServlet.doGet(request, response);
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.parse(EX.U_19, domain), e.getMessage());
        }
    }
	
	@Autowired private ReportService reportService;
    @Autowired private RecordService recordService;
	
	@Test
    public void testRegDevloper() {
        request.addParameter("loginName", "13588889999");
        request.addParameter("password", "www.boubei.com");
        request.addParameter("userName", "JK");
        request.addParameter("isDev", "true");
        
        Report group1 = new Report();
    	group1.setName("我的报表");
    	group1.setRemark("在此目录下创建你自己的报表吧");
    	group1.setType(Report.TYPE0);
    	group1.setParentId(Report.DEFAULT_PARENT_ID);
    	reportService.createReport(group1);
    	reportService.startOrStop(group1.getId(), 0);
    	
    	Record group2 = new Record();
    	group2.setName("我的功能");
    	group2.setRemark("在此目录下创建你自己的功能数据表吧");
    	group2.setType(Report.TYPE0);
    	group2.setParentId(Report.DEFAULT_PARENT_ID);
    	recordService.createRecord(group2);
    	recordService.startOrStop(group2.getId(), 0);
        
        Register regServlet = new Register();
        
        try {
            regServlet.init();
            regServlet.doGet(request, response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
            regServlet.destroy();
        }
        
        // test delete devloper
        Long userId = userService.getUserByLoginName("13588889999").getId();
        userService.deleteUser(UMConstants.DEV_GROUP_ID, userId);
        
        // 企业兼开发者注册
        Param p1 = ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.REGABLE, "开放注册", "REG_BDEV");
		String domain = "BD2";
        request.addParameter("loginName", "bd2@boubei.com");
        request.addParameter("password", "www.boudata.com");
        request.addParameter("userName", domain);
        request.addParameter("domain", domain);
        request.removeParameter("isDev");
        try {
            regServlet.init();
            regServlet.doGet(request, response);
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
            regServlet.destroy();
        }
        commonDao.delete( commonDao.getEntity(Param.class, p1.getId()) );
    }
	
	@Test
    public void testRegDevloperErr() {
		// 随机数验证失败
		request.removeParameter(SSOConstants.RANDOM_KEY);
		
        request.addParameter("loginName", "13588889999");
        request.addParameter("password", "www.boubei.com");
        request.addParameter("userName", "JK");
        request.addParameter("isDev", "true");
        
        Register regServlet = new Register();
        
        try {
            regServlet.init();
            regServlet.doGet(request, response);
            
        } catch (Exception e) {
        	Assert.assertFalse("Test servlet error:" + e.getMessage(), true);
        } finally {
            regServlet.destroy();
        }
    }
	
//	@Test
    public void testForbidReg() {
		Param p1 = ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.REGABLE, "开放注册", "false");
		Param p2 = ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.REGABLE_DEV, "开放开发者注册", "false");
		
		try {
			userService.regDeveloper(new User());
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.U_47, e.getMessage());
        }
		
		try {
			userService.regUser(new User());
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals(EX.U_46, e.getMessage());
        }
		
		p2.setValue("true");
		commonDao.delete( commonDao.getEntity(Param.class, p1.getId()) );
		commonDao.delete( commonDao.getEntity(Param.class, p2.getId()) );
	}
}
