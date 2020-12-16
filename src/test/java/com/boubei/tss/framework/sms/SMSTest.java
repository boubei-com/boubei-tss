package com.boubei.tss.framework.sms;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.EasyUtils;

public class SMSTest extends AbstractTest4F {
	
	@Test
	public void test() {
		
		AbstractSMS sms3 = AbstractSMS.create("BD", "key", "123456", "BD-SOFT");
		AbstractSMS sms4 = AbstractSMS.create("BD", "key", "123456", "BD-SOFT");
		Assert.assertEquals(sms3, sms4);
		 
		AbstractSMS sms = AbstractSMS.create();
		AbstractSMS sms2 = AbstractSMS.create();
		Assert.assertEquals(sms, sms2);
		
		String mobile = "13588889999";
		Assert.assertTrue( AbstractSMS.isChinaPhoneLegal(mobile) );
		Assert.assertFalse( AbstractSMS.isChinaPhoneLegal(null) );
		
		Object code = sms.sendRandomNum(mobile);
		
		Assert.assertTrue( sms.checkCode(mobile, code) );
		Assert.assertFalse( sms.checkCode(mobile, "abcd") );
		
		sms.logException( new BusinessException("test error") );
		
		SMSLog log = new SMSLog();
		log.setId((Long) log.getPK());
		log.setMsg("test");
		EasyUtils.obj2Json(log);
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("xxx", 123456);
		String tlParam = EasyUtils.obj2Json(data);
		
		Map<String, Object> params = new HashMap<>();
		params.put("tlCode", "t123");
		params.put("tlParam", tlParam);
		params.put("outId", "1111111111");
		MsgSender.send(params, "Admin", SendBySMS.class.getName());
		
		try { Thread.sleep(2000); } catch (InterruptedException e) { }
		
		params = new HashMap<>();
		params.put("tlCode", "t123");
		params.put("tlParam", tlParam);
		params.put("outId", "1111111111");
		params.put("phone", mobile);
		MsgSender.send(params , null, SendBySMS.class.getName());
		
		// 等待异步发送完成
		try { Thread.sleep(2000); } catch (InterruptedException e) { }
		
		try {
			SendBySMS _sms = new SendBySMS();
			_sms.params = new HashMap<>();
			_sms.params.put("phone", "13899887766");
			_sms.send();
		 } catch (Exception e) { }
	}

}
