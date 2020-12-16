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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.PX;
import com.boubei.tss.framework.AbstractTest4F;
import com.boubei.tss.framework.Config;
import com.boubei.tss.framework.mock.model._Group;
import com.boubei.tss.framework.mock.model._User;
import com.boubei.tss.framework.mock.service._IUMSerivce;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;

import freemarker.template.TemplateException;

/**
 * 测试了日志解析、日志Annotation、线程池、异步输出日志
 * 
 */
public class BusinessLogTest extends AbstractTest4F {
    
    @Autowired private _IUMSerivce umSerivce;
    @Autowired private LogService logService;
    
    @Test
    public void testParseMacro() throws IOException, TemplateException {
        String template = "<#if args[1]=1>停用<#else>启用</#if>了节点${args[0]}";
        Object[] objects = new Object[]{new Long(12), new Integer(0)};
        BusinessLogInterceptor intercepter = new BusinessLogInterceptor();
        Map<String, Object> data = new HashMap<String, Object>();
        
        assertEquals("启用了节点12", intercepter.parseMacro(template, objects, data));
        
        assertEquals("启用了节点12", intercepter.parseMacro("启用了节点12", new Object[]{}, data));
        assertEquals("启用了节点12", intercepter.parseMacro("启用了节点12", null, data));
        
        Config.setProperty(PX.LOG_FLUSH_MAX_SIZE, "100");
        assertTrue( new BusinessLogger().getMaxSize() > 0 );
        
        Config.setProperty(PX.LOG_FLUSH_MAX_SIZE, "xyz");
        assertTrue( new BusinessLogger().getMaxSize() > 0 );
    }
    
    /**
     * 测试日志Annotation、线程池、异步输出日志
     */
    @Test
    public void testUMToCreateLog() throws InterruptedException {
        log.info("test start......");
        
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 10; j++) {
                _Group group = new _Group();
                group.setCode("RD" + i + "_" + j);
                group.setName("研发");
                umSerivce.createGroup(group);
                
                _User user = new _User();
                user.setGroup(group);
                user.setUserName("JohnXa" + i + "_" + j);
                user.setPassword("123456");
                user.setAge(new Integer(25));
                user.setAddr("New York");
                user.setEmail("john@hotmail.com");
                umSerivce.createUser(user);
            }
        }
        
        Thread.sleep(3*1000);
        
        List<_User> result = umSerivce.queryAllUsers();
        assertTrue(result.size() > 50);
        
        LogQueryCondition condition = new LogQueryCondition();
        PageInfo logsInfo = logService.getLogsByCondition(condition);
        assertEquals(PageInfo.DEFAULT_PAGESIZE, logsInfo.getItems().size());
        assertTrue( logsInfo.getTotalRows() >= 180 ); // 还有10条没输出Test就over了
        
        condition.setContent("no no no");
        logsInfo = logService.getLogsByCondition(condition);
        assertTrue( logsInfo.getTotalRows() == 0 );
    }

}
