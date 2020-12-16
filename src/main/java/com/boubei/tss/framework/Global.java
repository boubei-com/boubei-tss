/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.boubei.tss.PX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.LoginCustomizerFactory;
import com.boubei.tss.matrix.MatrixUtil;
import com.boubei.tss.modules.param.ParamListener;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.param.ParamService;
import com.boubei.tss.modules.timer.SchedulerBean;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * 加载spring配置文件，以调用配置文件中配置的对象。
 */
public class Global {
	
	public  static SchedulerBean schedulerBean;
	private static ApplicationContext _ctx;

	private static String defaultContextPath = "META-INF/spring.xml";
	
	public final static Date restartTime = new Date(); // Global生成的时间，及系统重启的时间
 
	public static synchronized ApplicationContext getContext() {
		if (_ctx == null) {
			String contextPath = Config.getAttribute(PX.SPRING_CONTEXT_PATH);
			contextPath = (String) EasyUtils.checkNull(contextPath, defaultContextPath);
			
			_ctx = new ClassPathXmlApplicationContext(contextPath.split(","));
		}
		return _ctx;
	}
	
	public static Object getBean(String beanId) {
		return getContext().getBean(beanId);
	}

    public static ParamService getParamService() {
        return (ParamService) getBean("ParamService");
    }
    
    public static ICommonService getCommonService() {
    	return (ICommonService) getBean("CommonService");
    }

	public static synchronized void setContext(ApplicationContext context) {
		_ctx = context;
		
		// param缓存刷新监听器需要第一个执行，其它监听器里需要读取刷新后的Param信息
    	ParamManager.listeners.add(0, (ParamListener) ParamManager.getService());
    	getParamService().fireListener(null); // 系统启动时，自动触发一次所有的监听器，以完成缓存池等初始化。
    	
    	schedulerBean = SchedulerBean.getInstanse(); // 定时器初始化
    	
    	LoginCustomizerFactory.instance().getCustomizer();
    	
    	/* 清除在线用户库 & 历史取号器 */
    	try {
    		Map<Integer, Object> params = new HashMap<Integer, Object>();
    		params.put(1, MatrixUtil.getIpAddress());
    		params.put(2, DateUtil.today());
    		SQLExcutor.excute("delete from online_user where serverIp = ? and loginTime < ?", params, DMConstants.LOCAL_CONN_POOL);
    		
    		params = new HashMap<Integer, Object>();
    		params.put(1, DateUtil.today());
    		SQLExcutor.excute("delete from x_serialno where day < ? and day > '2018-08-24' ", params, DMConstants.LOCAL_CONN_POOL);
    		
    		params.put(1, DateUtil.subDays(DateUtil.today(), 31));
    		SQLExcutor.excute("delete from x_serialno where day = '2018-08-23' and createTime < ? and lastNum < 4 and precode like '%-%' and domain <> '无域'", params, DMConstants.LOCAL_CONN_POOL);
    	} 
    	catch( Exception e ) { }
	}

	public static synchronized void destroyContext() {
		_ctx = null;
	}
}
