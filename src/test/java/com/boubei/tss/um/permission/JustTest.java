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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.permission.GroupPermission;
import com.boubei.tss.um.entity.permission.GroupResource;
import com.boubei.tss.um.entity.permission.RoleResource;
import com.boubei.tss.um.entity.permission.RoleUserMapping;
import com.boubei.tss.um.entity.permission.RoleUserMappingId;
import com.boubei.tss.um.entity.permission.ViewRoleUser;
import com.boubei.tss.um.entity.permission.ViewRoleUser4SubAuthorize;
import com.boubei.tss.um.entity.permission.ViewRoleUserId;
import com.boubei.tss.util.EasyUtils;

public class JustTest {
	
	@Test
	public void test5() {
		List<String> l = new ArrayList<String>();
		l.add("运单");
		l.add("国");
		l.add("省");
		l.add("市");
		l.add("区");
		l.add(" 国 ");
		l.add("省 ");
		l.add(" 市");
		l.add("区");
		l.add("国_2");
		l.add("省_2");
		l.add("市_2");
		l.add("区_2");
		l.add("国");
		l.add("省");
		l.add("市");
		l.add("区");
		
		EasyUtils.fixRepeat(l);
		Assert.assertEquals("[运单, 国, 省, 市, 区, 国_2, 省_2, 市_2, 区_2, 国_2_2, 省_2_2, 市_2_2, 区_2_2, 国_3, 省_3, 市_3, 区_3]", l.toString());
		System.out.println( l );
		EasyUtils.fixRepeat( null );
		
		String ss = "运单号,回单号,寄件日期,寄件客户,寄件网点,寄件公司,国家,省,市,区县,详细地址,电话,手机,收件客户,目的网点,收件公司,国家,省,市,区县,收件地址信息,电话,物品名称,产品类型,件数,实际重量,体积";
		l = Arrays.asList(ss.split(","));
		EasyUtils.fixRepeat( l );
		System.out.println( l );
		
		System.out.println( "*哈哈*".substring(1, "*哈哈*".length() - 1) );
	}
	
	@Test
	public void test0() {
		GroupResource g1 = new GroupResource();
		g1.setDecode("00001");
		g1.setId(1L);
		g1.setName("Root");
		g1.setParentId(0L);
		
		g1.getSeqNo();
		
		GroupPermission p1 = new GroupPermission();
		p1.setId(1L);
	}

	@Test
	public void test1() {
		RoleUserMapping rum1 = new RoleUserMapping();
		
		RoleUserMappingId id1 = new RoleUserMappingId();
		id1.setRoleId(2L);
		id1.setUserId(2L);
		rum1.setId(id1);
		
		RoleUserMapping rum2 = new RoleUserMapping();
		
		RoleUserMappingId id2 = new RoleUserMappingId();
		id2.setRoleId(2L);
		id2.setUserId(2L);
		rum2.setId(id2);
		
		Set<RoleUserMappingId> set = new HashSet<RoleUserMappingId>();
		set.add(id1);
		set.add(id2);
		
		Assert.assertEquals(rum1.toString(), rum1.toString());
		
		Assert.assertEquals(rum1.getId().hashCode(), rum2.getId().hashCode());
	}

	@Test
	public void test2() {
		ViewRoleUser ru1 = new ViewRoleUser();
		
		ViewRoleUserId id1 = new ViewRoleUserId();
		id1.setRoleId(2L);
		id1.setUserId(2L);
		ru1.setId(id1);
		
		ViewRoleUser ru2 = new ViewRoleUser();
		
		ViewRoleUserId id2 = new ViewRoleUserId();
		id2.setRoleId(2L);
		id2.setUserId(2L);
		ru2.setId(id2);
		
		Set<ViewRoleUserId> set = new HashSet<ViewRoleUserId>();
		set.add(id1);
		set.add(id2);
		
		Assert.assertEquals(ru1.toString(), ru1.toString());
		
		Assert.assertEquals(ru1.getId().hashCode(), ru2.getId().hashCode());
	}
	
	@Test
	public void test3() {
		ViewRoleUser4SubAuthorize t = new ViewRoleUser4SubAuthorize();
		t.setId(null);
		t.getId();
		
		Assert.assertEquals(UMConstants.ROLE_RESOURCE_TYPE_ID, new RoleResource().getResourceType());
		
		PermissionDTO pt = new PermissionDTO(new Object[6]);
		pt.getRoleId();
		pt.getPermissionState();
		
		new PermissionServiceImpl().calState(1, 1);
		new PermissionServiceImpl().calState(2, 1);
	}
}
