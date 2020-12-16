package com.boubei.tss.modules.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.dm.record.file.RecordAttach;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;

/**
 * 远程调用接口 for 匿名用户。 需要把 /aapi 加到白名单里
 * 
 */
@Controller
@RequestMapping( {"/aapi"} )
public class AAPI {
	
	@Autowired ICommonService commService;
	@Autowired RecordService recordService;
	
	@RequestMapping(value = "/checkOnline")
	@ResponseBody
	public int checkOnline(HttpServletRequest request) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		String userCode = params.get("userCode");
		String token = params.get("token");
		
		String sql = "select ou.id from online_user ou, um_user u "
				+ " where ou.userId = u.id and u.loginName = ? and ? in (ou.token, ou.sessionId)"; 
		return SQLExcutor.queryL(sql, userCode, token).size();
	}
	
	@RequestMapping(value = "/domain/pics")
	@ResponseBody
	public Map<String, Object> getDomainPic(String domain) {
		List<Map<String, Object>> list = SQLExcutor.queryL("select * from x_domain where domain = ?", domain);
		if( list.isEmpty() ) {
			return new HashMap<String, Object>();
		}
		
		String picFields = "logo,ggpic,payment_code";
		Map<String, Object> row = SQLExcutor.queryL("select " +picFields+ " from x_domain where domain = ?", domain).get(0);
		fixPicField(row, picFields.split(","));
		
		list.get(0).putAll(row);
		
		return list.get(0);
	}
	
	private void fixPicField(Map<String, Object> row, String...fields) {
		for(String field : fields) {
			String val = (String) row.get(field);
			List<String> ids = new ArrayList<>();
			if( !EasyUtils.isNullOrEmpty(val) ) {
				String[] arr = val.split(","); // WechatIMG16.jpeg#1295,WechatIMG17.jpeg#1296
				for( String item : arr) {
					ids.add( item.split("#")[1] );
				}
			}

			row.put("_" + field, EasyUtils.list2Str(ids));
		}
	}
	
	/**
	 * 通常匿名访问（注：不支持多张图片）
	 * https://wb.boudata.com/tss/aapi/domain/pic?domain=BD&field=logo
	 * http://localhost:9000/tss/aapi/domain/pic?domain=HQ&field=logo
	 */
	@RequestMapping(value = "/domain/pic")
	public void domainPic(HttpServletResponse response, String domain, String field) throws Exception {
		String v = (String) SQLExcutor.queryVL("select " +field+ " from x_domain where domain = ?", field, domain);
		Long picId = EasyUtils.obj2Long( v.split("#")[1] );
		
		RecordAttach attach = recordService.getAttach(picId);
		if(attach != null) {
			FileHelper.downloadFile(response, attach.getAttachPath(), attach.getName());
		}
	}
}
