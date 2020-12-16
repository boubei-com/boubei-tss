/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.httpclient.HttpException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.AbstractTest4DM;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.InfoEncoder;

public class _RecorderAPITest extends AbstractTest4DM {
    
    @Autowired private _Recorder _recorder;
    @Autowired private RecordService recordService;
    
	Long recordId;
	
	public void init() {
		super.init();
		
		String tblDefine = "[ {'label':'类型', 'code':'f1', 'type':'number', 'nullable':'false'}," +
        		"{'label':'名称', 'code':'f2', 'type':'string'}," +
        		"{'label':'时间', 'code':'f3', 'type':'datetime', 'nullable':'false'}]";
		
		Record record = new Record();
		record.setName("record-1-2");
		record.setType(1);
		record.setParentId(0L);
		
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setTable("x_tbl_22");
		record.setDefine(tblDefine);
		record.setNeedLog(ParamConstants.TRUE);
		record.setNeedFile(ParamConstants.TRUE);

		recordService.createRecord(record);
		recordId = record.getId();
	}
    
    @Test
    public void testRecordAPI() {    
    	    	
    	super.logout();
    	
    	String uToken = InfoEncoder.string2MD5(recordId + ":Admin");
    	
    	Object[] params = new Object[] { "Admin", recordId.toString(), "D2", uToken, DateUtil.parse("2099-01-01"), "test", "Admin", new Date() };
        SQLExcutor.excuteInsert("insert into um_user_token (user,resource,type,token,expireTime,remark,creator,createTime,version) " +
				"values (?,?,?,?,?,?,?,?,0)", params , getDefaultSource());
        
        request.setQueryString("xx=12&yy=测试&&zz");
    	request.addParameter("uName", UMConstants.ADMIN_USER);
        request.addParameter("uToken", InfoEncoder.string2MD5(recordId + ":" + UMConstants.ADMIN_USER));
        _recorder.showAsJSON(request, recordId, 1);
    	 
        // test call record api with error uToken
        request.removeAllParameters();
        request.addParameter("uName", UMConstants.ADMIN_USER);
		request.addParameter("uToken", "invalidToken");
        request.addParameter("uCache", "true");
        request.addParameter("xx", "abc");
        _recorder.showAsJSON(request, recordId, 1);
        /*
    	try {
    		_recorder.showAsJSON(request, recordId, 1);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.DM_11 + UMConstants.ADMIN_USER, e.getMessage());
		}
		*/
    }
    
    // test api call ( local java call jetty, set securityLevel = 5 ) 
    public static void main(String[] args) throws HttpException, IOException {
    	String uToken = InfoEncoder.string2MD5("25:Admin");  /* 令牌：md5(id|name:loginName) 大写  */
    	System.out.println(uToken);
    	
    	String url = "http://localhost:9000/tss/xdata/api/json/25/1";
    	callAPI(url, "Admin", uToken);
    	callAPI(url, "wrong-user", uToken);   // wrong user
    	callAPI(url, "Admin", "wrong-token"); // wrong token
    	callAPI(url, "", "");
    }
}