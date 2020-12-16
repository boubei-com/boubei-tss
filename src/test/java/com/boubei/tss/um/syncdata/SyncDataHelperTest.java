/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.syncdata;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.UserDTO;

public class SyncDataHelperTest {
	
	@Test
	public void test1() {
		User user = new User();
		UserDTO userDTO = new UserDTO();
		userDTO.setLoginName("Jon.King");
		userDTO.setAuthMethod("test.authMethod");
		userDTO.setPassword(null);
		
		
		SyncDataHelper.setUserByDTO(user, userDTO);
		Assert.assertEquals(user.encodePassword(user.getLoginName()), user.getPassword());
		
		userDTO.setPassword(user.getPassword());
		userDTO.setDomain(null);
		userDTO.getDomain();
		SyncDataHelper.setUserByDTO(user, userDTO);
		Assert.assertEquals(user.encodePassword(user.getLoginName()), user.getPassword());
		
		Group g = new Group();
		g.setDomain("G1");
		SyncDataHelper.checkSecurity(g, userDTO);
		
		userDTO.setDomain("D001");
		try {
			SyncDataHelper.checkSecurity(g, userDTO);
			Assert.fail();
		} catch(Exception e) {
		}
	}

}
