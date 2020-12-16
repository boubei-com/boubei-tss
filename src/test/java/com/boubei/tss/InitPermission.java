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

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;

import com.boubei.tss.dm.report.Report;
import com.boubei.tss.dm.report.ReportDao;
import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.permission.ResourcePermission;

/**
 * 将已经存在的资源授权给Admin。
 */
@ContextConfiguration(
        locations={
          "classpath:META-INF/spring-framework.xml",  
          "classpath:META-INF/spring.xml"
        } 
      )
@Rollback
@Transactional
public class InitPermission extends AbstractTransactionalJUnit4SpringContextTests { 
 
    Logger log = Logger.getLogger(this.getClass());    
    
    @Autowired private ResourcePermission resourcePermission;
    @Autowired private APIService apiService;
    @Autowired private ReportDao reportDao;
 
    @Before
    public void setUp() throws Exception {
        Global.setContext(super.applicationContext);
    }
    
//    @Test
    public void initPermission() {
        // 初始化虚拟登录用户信息
    	apiService.mockLogin(UMConstants.ADMIN_USER);
        
        // 先检查DB的初始化情况，正常情况下资源视图的root节点已经被默认授权给Admin角色了。
        List<?> resources   = reportDao.getEntities("from ReportResource");
        List<?> permissions = reportDao.getEntities("from ReportPermission");
        
        Assert.assertFalse("资源视图为空，请先确认DM数据库资源视图表是否初始化成功了，有可能连的是test/resources/application.properties下配置的H2测试库，" +
        		"先重命名该文件，确保连的是MySQL的DB。", resources.isEmpty());
        Assert.assertFalse("ReportPermission表为空，资源视图root节点没有被初始化权限。", permissions.isEmpty());
        
        List<?> list = reportDao.getEntities("from Report order by decode");
        for(Object temp : list) {
        	Report report = (Report) temp;
        	Long reportId = report.getId();
        	resourcePermission.addResource(reportId, Report.RESOURCE_TYPE);
        }
    }
}
