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

import javax.servlet.http.Cookie;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.TokenUtil;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.context.RequestContext;
import com.boubei.tss.modules.api.APIService;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.helper.dto.OperatorDTO;

import junit.framework.Assert;

public class SystemInfoTest extends AbstractTest4UM {
	
	@Autowired SystemInfo si;
	
	@Test
	public void testGetVersion() {
		Object[] result = si.getVersion(null);
		Assert.assertEquals("test", result[1]);
		Assert.assertEquals(true, result[2]);
		
		Context.destroyIdentityCard(Context.getToken());
		Context.destroy();
		
		MockHttpServletRequest req = new MockHttpServletRequest();
		result = si.getVersion( req );
		Assert.assertEquals(false, result[2]);
		
		// test auto login by online_user
		String token = "1234567890";
		SQLExcutor.excuteInsert("insert into online_user(userId, token, clientIp) values(?,?,?)", new Object[] { -1L, token, "127.0.0.1" }, DMConstants.LOCAL_CONN_POOL);
		req.setCookies( new Cookie(RequestContext.USER_TOKEN, token) );
		
		Context.initRequestContext(req);
		Context.setResponse( new MockHttpServletResponse() ); 
		result = si.getVersion( req );
		Assert.assertEquals(true, result[2]);
		
		SQLExcutor.excuteInsert("insert into online_user(userId, token, clientIp) values(?,?,?)", new Object[] { -1L, token, "127.0.0.1" }, DMConstants.LOCAL_CONN_POOL);
		boolean success = SystemInfo.autoLogin(req, new MockHttpServletResponse() , (APIService) Global.getBean("APIService"));
		Assert.assertTrue(success);
		SystemInfo.autoLogin(req, new MockHttpServletResponse() , (APIService) Global.getBean("APIService"));
	}
	
	@Test
	public void testGetThreadInfos() {
		Object[] result = si.getThreadInfos();
		Assert.assertTrue( (Integer)result[0] > 0 );
	}

	@Test
	public void testGetLoginUser() {
		String token = TokenUtil.createToken("1234567890", UMConstants.ROBOT_USER_ID);
		IdentityCard card = new IdentityCard(token, new OperatorDTO(UMConstants.ROBOT_USER_ID, "Job.Robot"));
		Context.initIdentityInfo(card); 
		
		Object[] result = si.testSessionVal("token");
		Assert.assertNotNull(result[0]);
		
		result = si.getLoginUserInfo("token");
		Assert.assertNotNull(result[0]);
		
		Context.initIdentityInfo(null); 
	}

	@Test 
	public void testDenyMassAttack() {
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setServletPath("/tss/login.html");
		SecurityUtil.denyMassAttack(request );
		
		Config.setProperty(PX.DENY_MASS_ATTACK, "true");
		
		SecurityUtil.denyMassAttack(request );
		
		request.addHeader(RequestContext.USER_REAL_IP, "192.168.0.1");
		request.setAttribute(RequestContext.USER_ORIGN_IP, "unknown");
		request.setRemoteAddr("192.168.0.1");
		request.setServletPath("/test");
		
		SecurityUtil.denyMassAttack(request);
		
		
		Config.setProperty(PX.MAX_HTTP_REQUEST, "100");
		try {
			for( int i = 0; i < 120; i++ ) {
				SecurityUtil.denyMassAttack(request);
			}
			Assert.fail();
		} catch ( Exception e ) {
		}
		
		Config.setProperty(PX.MIN_REQUEST_INTERVAL, "100");
		try {
			for( int i = 0; i < 30; i++ ) {
				SecurityUtil.denyMassAttack(request);
			}
			Assert.fail();
		} catch ( Exception e ) {
		}
		
		request.setRemoteAddr("127.0.0.1");
		SecurityUtil.denyMassAttack(request);
		
		Config.setProperty(PX.IP_BLACK_LIST, "127.0.0.1");
		try {
			SecurityUtil.denyMassAttack(request);
			Assert.fail();
		} catch ( Exception e ) {
			
		}
		
		Config.setProperty(PX.DENY_MASS_ATTACK, "false");
		
	}
}
