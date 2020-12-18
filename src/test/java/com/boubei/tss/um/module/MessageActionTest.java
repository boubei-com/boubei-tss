/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.module;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.grid.GridAttributesMap;
import com.boubei.tss.modules.api.API;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.MessageAction;
import com.boubei.tss.um.entity.Message;
import com.boubei.tss.um.helper.MessageQueryCondition;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MailUtil;

public class MessageActionTest extends AbstractTest4UM {
	
	@Autowired MessageAction messageAction;
	@Autowired API api;
	
	@Test
	public void test() {
		messageAction.sendEmail("test", "<html><body><h3>生命变的厚重</h3></body></html>", 
				MailUtil.getEmailTo(MailUtil.DEFAULT_MS)[0]);
		
		messageAction.sendEmail("test", "<html><body><h3>生命变的厚重</h3></body></html>",  "X1");
		
		// 等待邮件异步发送完成
		try { Thread.sleep(2000); } catch (InterruptedException e) { }
	}
	
	@Test
	public void test2() {
		messageAction.sendHtmlEmail("test", "<h3>生命变的厚重</h3>", 
				MailUtil.getEmailTo(MailUtil.DEFAULT_MS)[0]);
		
		messageAction.sendHtmlEmail("test", "<h3>生命变的厚重</h3>", "Admin");
		messageAction.sendHtmlEmail("test", "<h3>生命变的厚重</h3>", "-1");
		
		messageAction.sendHtmlEmail("test", "<h3>生命变的厚重</h3>", "X1");
		messageAction.sendHtmlEmail("test", "<h3>生命变的厚重</h3>", "X1.@");
		
		// 等待邮件异步发送完成
		try { Thread.sleep(3000); } catch (InterruptedException e) { }
	}
	
	@Test
	public void test3() {
		messageAction.sendMessage("test", "生命变的厚重", null, null, null);
		messageAction.sendMessage("test", "生命变的厚重", "Admin,-1234,-12345,X1.@", null, null);
		messageAction.sendMessage("test", "生命变的厚重", "-1234,-12345,X1.@", null, null);
		messageAction.sendMessage("test", "生命变的厚重", "", null, null);
		
		messageAction.sendMessage("test", "生命变的厚重", "Admin", null, null);
		List<Message> list = getInboxList();
		Assert.assertTrue(list.size() > 0);
		
		Assert.assertTrue( messageAction.getUnReadMsgNum() > 0 );
		
		MessageQueryCondition condition = new MessageQueryCondition();
		condition.setContent("厚重");
		condition.setTitle("test");
		condition.setSender(null);
		condition.setSearchTime1(DateUtil.parse("2015-01-01"));
		condition.setSearchTime2(new Date());
		condition.setCategory(Message.CATEGORY_NOTIFY);
		condition.setLevel(Message.LEVEL_LIST[0]);
		messageAction.listMessages(response, condition, 1);
		
		Assert.assertTrue( messageAction.listMessages2Json(condition, 1).size() > 0 );
		
		condition.setRead("yes");
		Assert.assertTrue( messageAction.listMessages2Json(condition, 1).size() == 0 );
		
		condition.setRead("no");
		Assert.assertTrue( messageAction.listMessages2Json(condition, 1).size() > 0 );
		
		condition.setRead("other");
		Assert.assertTrue( messageAction.listMessages2Json(condition, 1).size() == 0 );
		
		Message message1 = list.get(0);
		Assert.assertEquals(UMConstants.ADMIN_USER_ID, message1.getReceiverId());
		Assert.assertEquals("test", message1.getTitle());
		Assert.assertEquals("生命变的厚重", message1.getContent());
		Assert.assertEquals(Environment.getUserId(), message1.getSenderId());
		Assert.assertEquals(Environment.getUserName(), message1.getSender());
		Assert.assertNotNull(message1.getSendTime());
		Assert.assertNull(message1.getReadTime());
		
		Long message1Id = message1.getId();
		message1 = messageAction.getMessage(message1Id);
		Assert.assertNotNull(message1.getReadTime());
		
		messageAction.batchRead("view_all");
		messageAction.batchRead(message1Id.toString());
		
		api.hignLevelMessagegs();
		api.batchRead(message1Id.toString());
		
		messageAction.deleteMessage(message1Id.toString());
		messageAction.deleteMessage("del_all");
		
		list = getInboxList();
		Assert.assertTrue(list.size() == 0);
		
		messageAction.listMessages(response, condition, 1);
		
		message1.setId((Long) message1.getPK());
		message1.setWorkflow(null);
		message1.setWorkflowItem(null);
		log.debug(EasyUtils.obj2Json(message1));
		log.debug( message1.getReceiver() );
		
		Message msg2 = new Message();
		GridAttributesMap map = new GridAttributesMap(new String[]{});
		msg2.getAttributes(map);
		msg2.setReadTime(new Date());
		msg2.getAttributes(map);
		
		condition = new MessageQueryCondition();
		condition.setSenderId(Environment.getUserId());
		Assert.assertTrue( messageAction.listMessages2Json(condition, 1).size() == 0 ); // 清空了
	}
	
	@SuppressWarnings("unchecked")
	private List<Message> getInboxList(){
		Long userId = Environment.getUserId();
		String hql = " from Message m where m.receiverId = ?1 order by m.id desc ";
		return (List<Message>) commonDao.getEntities(hql, userId);
	}

}
