/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.Group;

import junit.framework.Assert;

public class PermissionHelperTest {
	
	@Test
	public void test() {
		
		String condition = "";
		PermissionHelper.permissionHQL(Group.class.getName(), "GroupPermission", condition, false);
		
		PermissionHelper ph = new PermissionHelper();
		Assert.assertEquals("", ph.genRankCondition4DeleletePermission(UMConstants.IGNORE_PERMISSION));
		Assert.assertEquals("", ph.genRankCondition4SelectPermission("ACDS"));
		
		Assert.assertEquals(ParamConstants.TRUE, ph.convertRank(UMConstants.PASSON_AUTHORISE_PERMISSION)[1]);
		
		List<Long> permitedResourceIds = Arrays.asList(1L, 2L);
		List<Group> resources = new ArrayList<Group>();
		Group g1 = new Group();
		g1.setId(3L);
		resources.add(g1);
		ph.filtrateResourcesByPermission(permitedResourceIds, resources);
		
		try {
			List<Long> list = new ArrayList<Long>();
			list.add(1L);
			List<?> permitedList = new ArrayList<Long>();
			
			PermissionHelper.vsSize(permitedList, list, "有权限的个数少于总数");
        	Assert.fail("should throw exception but didn't.");
        } catch (Exception e) {
        	Assert.assertEquals("有权限的个数少于总数", e.getMessage());
        }
	}

}
