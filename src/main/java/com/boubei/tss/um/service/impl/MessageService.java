/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.service.impl;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.persistence.pagequery.PaginationQueryByHQL;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.entity.Message;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.MessageQueryCondition;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.service.IMessageService;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
 
@Service("MessageService")
public class MessageService implements IMessageService {
	
	Logger log = Logger.getLogger(this.getClass());
	
	@Autowired ILoginService loginService;
	@Autowired private ICommonDao commonDao;
	
	public void sendMessage(String title, String content, String receivers){
		sendMessage(title, content, receivers, Message.CATEGORY_NOTIFY, Message.LEVEL_LIST[0]);
	}
 
	public void sendMessage(String title, String content, String receivers, String category, String level){
		
		if( EasyUtils.isNullOrEmpty(receivers) ) return;
		
		category = (String) EasyUtils.checkNull(category, Message.CATEGORY_NOTIFY);
		level = (String) EasyUtils.checkNull(level, Message.LEVEL_LIST[0]);
    	
    	String[] ids = loginService.getContactInfos(receivers, true);
    	if(ids == null || ids.length == 0) {
    		ids = receivers.split(","); // 有可能接收人为ID（如：站内信直接回复）
    	}

		for(String receiverId : ids) {
			try {
				Long _receiveId = EasyUtils.obj2Long(receiverId);
				User u = (User) commonDao.getEntity(User.class, _receiveId);
				
				Message temp = new Message();
				temp.setReceiverId(_receiveId);
				temp.setReceiver( u.getUserName() + " > " + receivers);
				temp.setTitle( title );
				temp.setContent( content );
				temp.setCategory(category);
				temp.setLevel(level);
				
	            commonDao.createWithoutFlush(temp);
	            
			} catch(Exception e) { }
		}
		commonDao.flush();
	}
 
	public Message viewMessage(Long id) {
		Message message = (Message) commonDao.getEntity(Message.class, id);
		message.setReadTime( (Date) EasyUtils.checkTrue(Environment.getUserId().equals(message.getReceiverId()), new Date(), null) );
		commonDao.update(message);
		
		return message;
	}
	
	public void batchRead(String ids) {
		if("view_all".equals(ids)) { // 设置用户的所有站内消息为已阅
			String hql = "update Message m set m.readTime = ?1  where m.receiverId = ?2 and readTime is null";
			commonDao.executeHQL(hql, new Date(), Environment.getUserId());
			return;
		}
		
		String[] idArray = ids.split(",");
		for(String _id : idArray) {
			viewMessage( EasyUtils.obj2Long(_id) );
		}
	}
	
	public void deleteMessage(String ids){
		if("del_all".equals(ids)) { // 清空用户的站内消息
			List<?> list = commonDao.getEntities("from Message m where m.receiverId = ?1", Environment.getUserId());
			commonDao.deleteAll(list);
			return;
		}
		
		String[] idArray = ids.split(",");
		for(String _id : idArray) {
			commonDao.delete( Message.class, EasyUtils.obj2Long(_id) );
		}
	}
	
	public int getUnReadMsgNum() {
		Long userId = Environment.getUserId();
		String hql = " select count(m) from Message m where m.receiverId = ?1 and sendTime > ?2 and readTime is null ";
		List<?> list = commonDao.getEntities(hql, userId, DateUtil.subDays(new Date(), 3));
		return EasyUtils.obj2Int( list.get(0) );
	}
	
	public List<?> getUnReadHignLevelMsg(int days) {
		String hql = "from Message where level = ?1 and receiverId = ?2 and sendTime > ?3 and readTime is null order by id desc";
		Object userId = Environment.getNotnullUserId();
		return commonDao.getEntities(hql, Message.LEVEL_LIST[2], userId, DateUtil.subDays(new Date(), days));
	}
	
	public PageInfo getBoxList(MessageQueryCondition condition) {
		Long userId = Environment.getUserId();
		if( condition.getSenderId() == null ) {
			condition.setReceiverId(userId);
		}
		else {
			condition.setSenderId(userId);
		}
		
        String hql = " from Message o " 
        		+ " where 1=1 " + condition.toConditionString() 
        		+ " order by o.id desc ";
 
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(commonDao.em(), hql, condition);
        return pageQuery.getResultList();
    }
}
