/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework;

import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.boubei.tss.H2DBServer;
import com.boubei.tss._TestUtil;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.DemoOperator;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.log.LogService;
import com.boubei.tss.modules.param.ParamService;

@ContextConfiguration(
	  locations={
		    "classpath:META-INF/spring-test.xml",  
		    "classpath:META-INF/spring-framework.xml",
		    "classpath:META-INF/spring-mvc.xml"
	  }   
) 
@WebAppConfiguration
@Rollback
@Transactional
public abstract class AbstractTest4F extends AbstractTransactionalJUnit4SpringContextTests { 
 
    protected static Logger log = Logger.getLogger(AbstractTest4F.class);    
    
    @Autowired protected H2DBServer dbserver;
    
    @Autowired protected LogService logService;
    @Autowired protected ParamService paramService;
    @Autowired protected ICommonDao commonDao;
    
    protected HttpServletResponse response;
    protected MockHttpServletRequest request;
    
    @Before
    public void setUp() throws Exception {
        Global.setContext(super.applicationContext);
        
        if(paramService.getParam(0L) == null) {
			String sqlpath = _TestUtil.getInitSQLDir();
	    	log.info( " sql path : " + sqlpath);
	        _TestUtil.excuteSQL(sqlpath + "/framework");
	        _TestUtil.excuteSQL(sqlpath + "/um");
	        _TestUtil.excuteSQL(sqlpath + "/dm");
	    	_TestUtil.excuteSQL(sqlpath + "/cms");
	    	_TestUtil.excuteSQL(sqlpath + "/portal");
    	}
        
        String token = TokenUtil.createToken(new Random().toString(), 12L); 
        IdentityCard card = new IdentityCard(token, new DemoOperator(12L));
        Context.initIdentityInfo(card);
        
        Context.initRequestContext(request = new MockHttpServletRequest());
        
        request = new MockHttpServletRequest();
        Context.setResponse(response = new MockHttpServletResponse());
    }
 
    @After
    public void tearDown() throws Exception {
        dbserver.stopServer();
    }

}
