package com.boubei.tss.modules.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.dml.SqlConfig;
import com.boubei.tss.dm.record.RecordField;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.DomainConfig;

public class BITest extends AbstractTest4DM {

	@Autowired
	private BI bi;

	@Test
	public void test() {

		super.initDomain();

		request.addParameter("testFlag", "true");
		request.addParameter("id", "0");
		request.addParameter("x", "");
		Map<String, Object> result = (Map<String, Object>) bi.querySQL(request, "test3");
		log.debug(result);
		Assert.assertTrue(((int) result.get("total")) > 0);

		Object result2 = bi.querySQL_(request, "test3");
		log.debug(result2);

		request.addParameter("page", "1");
		bi.exportSQL(request, response, "test3");
		
		response = new MockHttpServletResponse();
		request.addParameter("cfields", "id,loginname,username");
		bi.exportSQL(request, response, "test3");
		
		response = new MockHttpServletResponse();
		request.addParameter("selectFields", "id X,loginname Y");
		bi.exportSQL(request, response, "test3");
		
		request.removeParameter("selectFields");
		request.addParameter("selectFields", "0");
		bi.exportSQL(request, new MockHttpServletResponse(), "test3");
		
		DomainConfig domainConfig = new DomainConfig("code", "[" 
				+ " {\"field\":\"ck\", \"title\":\"\", \"export\":\"\"}"
				+ ",{\"field\":\"id\", \"title\":\"X2\", \"export\":\"\"}" 
				+ ",{\"field\":\"loginname\", \"title\":\"Y2\", \"export\":\"√\"}" 
				+ ",{\"field\":\"loginname\", \"title\":\"Y3\", \"export\":\"√\"}" 
				+ "]");
		Global.getCommonService().create(domainConfig);
		
		request.removeParameter("selectFields");
		request.addParameter("selectFields", domainConfig.getId().toString());
		bi.exportSQL(request, new MockHttpServletResponse(), "test3");

		request.addParameter("rows", "10");
		Object result3 = bi.querySQL_(request, "test3");
		log.debug(result3);

		request.addParameter("id", "0");
		request.addParameter("x", "");
		result = (Map<String, Object>) bi.querySQL(request, "test4");
		log.debug(result);
		
		bi.querySQL4JSONP(request, "test4");
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
		Assert.assertTrue((boolean) rows.get(0).get("helloWorld"));
		
		log.debug( SqlConfig._getScript("test4", null).toString() );
		
		domainConfig.setContent("xxxxx");
		try {
			bi.exportSQL(request, new MockHttpServletResponse(), "test3");
			Assert.fail();
		} catch ( Exception e ) {
		}
	}
	
	@Test
	public void test2(){
		
		super.initDomain();
		
		ICommonService service = Global.getCommonService();
		
		List<Long> roles = Arrays.asList(new Long[]{1l,2l,-10000l});
		String domain = Environment.getDomain();
		
		RecordField recordField = new RecordField();
		recordField.setTable("um_user");
		recordField.setCode("sex");
		recordField.setLabel("我的锅1");
		recordField.setDomain(domain);
		service.create(recordField);
		Assert.assertTrue(recordField.containsRole2(roles));
		Assert.assertTrue(recordField.containsRole1(roles));
		
		recordField = new RecordField();
		recordField.setTable("um_user");
		recordField.setCode("telephone");
		recordField.setLabel("我的锅2");
		recordField.setDomain(domain);
		recordField.setRole1("-77");
		recordField.setRole2("-88,-77");
		service.create(recordField);
		Assert.assertTrue(!recordField.containsRole2(roles));
		Assert.assertTrue(!recordField.containsRole1(roles));
		
		recordField = new RecordField();
		recordField.setTable("um_user");
		recordField.setCode("udf");
		recordField.setLabel("我的锅3");
		recordField.setDomain(domain);
		recordField.setRole1("-10000");
		recordField.setRole2("-88,-77");
		service.create(recordField);
		Assert.assertTrue(recordField.containsRole1(roles));
		Assert.assertTrue(!recordField.containsRole2(roles));
		
		recordField = new RecordField();
		recordField.setTable("test_table");
		recordField.setCode("origin");
		recordField.setLabel("我的锅4");
		recordField.setDomain(domain);
		recordField.setRole1("-88,-77");
		recordField.setRole2("-10000");
		service.create(recordField);
		Assert.assertTrue(!recordField.containsRole1(roles));
		Assert.assertTrue(recordField.containsRole2(roles));
		
		recordField = new RecordField();
		recordField.setTable("test_table");
		recordField.setCode("address");
		recordField.setLabel("我的锅5");
		recordField.setDomain(domain);
		recordField.setType("hidden");
		service.create(recordField);
		
		recordField = new RecordField();
		recordField.setTable("um_user");
		recordField.setCode("xxxx");
		recordField.setLabel("我的锅6");
		recordField.setDomain(domain);
		recordField.setType("hidden");
		service.create(recordField);
		
		CacheHelper.getNoDeadCache().flush();
		Map<String,String> requestMap = new HashMap<>();
		BIDataProcess.addUdf(SqlConfig.getScript("test31"), requestMap);
		Assert.assertEquals(", s.sex 我的锅1, s.udf 我的锅3", requestMap.get("um_user_s_udf"));

		request.addParameter("id", "0");
		Map<String, Object> result = (Map<String, Object>) bi.querySQL(request, "test31");
		log.debug(result);
		
		bi.queryRecordFields("test_table");
	}
}
