/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.log;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.mock.model._User;
import com.boubei.tss.framework.mock.service._IUMSerivce;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.XmlPrintWriter;
import com.boubei.tss.framework.web.display.grid.GridDataDecoder;
import com.boubei.tss.framework.web.display.grid.GridDataEncoder;

public class LogActionTest extends AbstractTest4F {

	@Autowired private LogAction action;
	@Autowired private LogService logService;

	@Autowired private _IUMSerivce umSerivce;
 
	@Test
	public void testLogAction() {
		for (int i = 0; i < 10; i++) {
			final int index = i;
			for (int j = 0; j < 10; j++) {
				_Group group = new _Group();
				group.setCode("RD" + index + "_" + j);
				group.setName("研发");
				umSerivce.createGroup(group);

				_User user = new _User();
				user.setGroup(group);
				user.setUserName("JohnXa" + index + "_" + j);
				user.setPassword("123456");
				user.setAge(new Integer(25));
				user.setAddr("New York");
				user.setEmail("john@hotmail.com");
				umSerivce.createUser(user);
			}
		}

		try {
			Thread.sleep(3 * 1000);
		} catch (InterruptedException e) {
		}
		
		action.getAllApps4Tree(response);
		
		LogQueryCondition condition = new LogQueryCondition();
		condition.setOperateTable("用户");
		condition.setOperateTimeFrom(new Date(System.currentTimeMillis() - 1000*10));
		condition.setOperateTimeTo(new Date());
		condition.setOperatorName(Environment.getUserName());
		condition.setOperatorIP(null);
		condition.setOperationCode(null);
		
		action.queryLogs4Grid(null, condition, 1);
		Assert.assertTrue( action.queryLogs4Json(condition).size() > 0);
		
		PageInfo logsInfo = logService.getLogsByCondition(condition);
        List<?> logs = logsInfo.getItems();
        Assert.assertTrue(logs.size() > 0);
        action.getLogInfo(response, ((Log)logs.get(0)).getId());
        
        Assert.assertNotNull(((Log)logs.get(0)).getPK());
        
        // 顺带测试一下Grid的功能
        GridDataEncoder encoder = new GridDataEncoder(logs, LogAction.LOG_GRID_TEMPLET_PATH);
        try {
			encoder.print(new XmlPrintWriter(response.getWriter()));
		} catch (IOException e) {
			Assert.fail();
		}
        
        encoder.getTemplete().setColumnAttribute("id", "align", "center");
        
        logs = GridDataDecoder.decode(encoder.toXml(), Log.class);
        Assert.assertTrue(logs.size() > 0);
	}

}
