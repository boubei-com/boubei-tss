/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.file.CreateAttach;
import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.cloud.entity.DomainInfo;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.FileHelper;

public abstract class AbstractTest4DM extends AbstractTest4TSS { 
    
    protected String getDefaultSource(){
    	return "connectionpool";
    }

    protected void init() {
    	super.init();
    	
        if(paramService.getParam(PX.DATASOURCE_LIST) == null) {
        	Param dlParam = ParamManager.addComboParam(ParamConstants.DEFAULT_PARENT_ID, PX.DATASOURCE_LIST, "数据源列表");
            ParamManager.addParamItem(dlParam.getId(), "connectionpool-1", "数据源1", ParamConstants.COMBO_PARAM_MODE);
            
            Param dsParam2 = ParamManager.addParamItem(dlParam.getId(), "connectionpool-2", "数据源2", ParamConstants.COMBO_PARAM_MODE);
            paramService.saveParam(dsParam2);
            
            Param dsParam3 = ParamManager.addParamItem(dlParam.getId(), "connectionpool-3", "数据源3", ParamConstants.COMBO_PARAM_MODE);
            paramService.saveParam(dsParam3);
            
            SQLExcutor.excute("update component_param set creatorId = 12 where id = " + dsParam3.getId(), "connectionpool");
            DMUtil.setDSList(null);
        }
        if(paramService.getParam(PX.DEFAULT_CONN_POOL) == null) {
        	ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.DEFAULT_CONN_POOL, "默认数据源", getDefaultSource());
        }
        if(paramService.getParam(PX.ATTACH_PATH) == null) {
			String tmpDir = FileHelper.ioTmpDir() + "temp_noExists";
			
			Param p = ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.ATTACH_PATH, "临时文件导出目录", tmpDir);
			Assert.assertEquals( FileHelper.ioTmpDir(), DMUtil.getAttachPath() );
			
			paramService.delete(p.getId());
			tmpDir = FileHelper.ioTmpDir() + "temp";
			FileHelper.createDir(tmpDir);
			
			ParamManager.addSimpleParam(ParamConstants.DEFAULT_PARENT_ID, PX.ATTACH_PATH, "临时文件导出目录", tmpDir);
			Assert.assertEquals( tmpDir, DMUtil.getAttachPath() );
			log.info("临时文件导出目录：" + tmpDir);
        }
    }
    
    protected String domain = "BD";
    protected User domainUser;
    protected User staff0;
    protected User staff1;
    protected  User jk;  // 主管 
    protected User tom; // 主管 + 人事主管
    protected User ceo;
    protected Role role0 = null, role1 = null, role2 = null, role3 = null, role4 = null;
    protected DomainInfo domainInfo;
    protected Group domainGroup;
    protected Group mg;
    
    public void initDomain() {
    	
    	mg = super.createGroup("XX企业组2", UMConstants.DOMAIN_ROOT_ID);
    	
    	// 企业注册
		domainUser = new User();
		domainUser.setLoginName(domain);
		domainUser.setUserName(domain);
		domainUser.setPassword("123456");
		userService.regBusiness(domainUser, domain, mg.getId());
		
		domainGroup = (Group) commonDao.getEntities("from Group where name=?1", domain).get(0);
		long domainUserId = domainUser.getId();
		
		domainInfo =  (DomainInfo) commonDao.getEntities("from DomainInfo where name=?1", domain).get(0);
		Assert.assertNotNull(domainInfo);
		
		// 新增一个辅助组（审批流暂时用不到辅助组）
        createGroup("临时项目", UMConstants.ASSISTANT_GROUP_ID);
        
		login(domainUser);
		
		// 新建一个用户组
        Group pGroup = createGroup("卜贝数据", domainGroup.getId());
        Group sonGroup1 = createGroup("开发部", pGroup.getId());
        Group sonGroup2 = createGroup("人事部", pGroup.getId());
        
        // 企业域自己的辅助组
        createGroup("BD项目", UMConstants.ASSISTANT_GROUP_ID);
        
        // 切回Admin
        login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);

        // 新增用户
        staff0 = createUser("staff0", UMConstants.MAIN_GROUP_ID, null);
        staff1 = createUser("staff1", sonGroup1.getId(), null);
        jk  = createUser("JK", sonGroup1.getId(), null);  // 主管 
        tom = createUser("Tom", sonGroup2.getId(), null); // 主管 + 人事主管，  给与Admin，可以不用再单独授权录入表
        ceo = createUser("Jenny", pGroup.getId(), null);

        // 新建角色
        role0 = createRole("员工", staff0.getId() + "," + staff1.getId() + "," + jk.getId() + "," + tom.getId() + "," + domainUserId );
        role1 = createRole("部门主管", jk.getId() + ",-1," + tom.getId() + "," + domainUserId);
        role2 = createRole("人事主管", tom.getId() + "," + domainUserId);
        role3 = createRole("CEO", ceo.getId() + "," + domainUserId);
        role3.setDescription("freeUse");
        commonDao.update(role3);
        role4 = createRole("基础角色", "");
    }
    
    public static void callAPI(String url, String user, String uToken) throws HttpException, IOException {
    	PostMethod postMethod = new PostMethod(url + "?uName=" +user+ "&uToken=" + uToken);
    	postMethod.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "utf-8");

        // 最后生成一个HttpClient对象，并发出postMethod请求
        HttpClient httpClient = new HttpClient();
        int statusCode = httpClient.executeMethod(postMethod);
        if(statusCode == 200) {
            System.out.print("返回结果: ");
            String soapResponseData = postMethod.getResponseBodyAsString();
            System.out.println(soapResponseData);     
        }
        else {
            System.out.println("调用失败！错误码：" + statusCode);
        }
    }
    
    
	static String UPLOAD_PATH = FileHelper.ioTmpDir() + "/upload/record/";
	
	 // 上传附件
    protected void uploadDocFile(Object record, Object itemId) {
   		AfterUpload upload = new CreateAttach();
   	
	    IMocksControl mocksControl =  EasyMock.createControl();
	    HttpServletRequest mockRequest = mocksControl.createMock(HttpServletRequest.class);
	    
	    EasyMock.expect(mockRequest.getParameter("record")).andReturn(record.toString());
	    EasyMock.expect(mockRequest.getParameter("recordId")).andReturn(record.toString());
	    EasyMock.expect(mockRequest.getParameter("itemId")).andReturn(itemId.toString());
	    EasyMock.expect(mockRequest.getParameter("type")).andReturn(RecordAttach.ATTACH_TYPE_DOC.toString());
	    EasyMock.expect(mockRequest.getParameter("petName")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("refreshGrid")).andReturn(null);
	    EasyMock.expect(mockRequest.getParameter("uploadField")).andReturn(null);
	    
	    try {
	    	String filename = "123.txt";
			String filepath = UPLOAD_PATH + "/" + filename;
			FileHelper.writeFile(new File(filepath), "卜贝求真。");
	        
	        mocksControl.replay(); 
			upload.processUploadFile(mockRequest, filepath, filepath);
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.assertFalse(e.getMessage(), true);
		}
	    
	    _TestUtil.printEntity(super.permissionHelper, "RecordAttach"); 
   }
}
