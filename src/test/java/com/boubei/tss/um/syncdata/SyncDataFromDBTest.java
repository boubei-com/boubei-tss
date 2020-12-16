/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.syncdata;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.H2DBServer;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.Progressable;
import com.boubei.tss.um.AbstractTest4UM;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.action.GroupAction;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.UserDTO;
import com.boubei.tss.um.service.IResourceService;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;

/**
 * 测试用户同步
 */
public class SyncDataFromDBTest extends AbstractTest4UM {

	@Autowired GroupAction groupAction;
	
	@Autowired ISyncService syncService;
	@Autowired IResourceService resourceService;
	
	Group mainGroup;
	
	public void init() {
		super.init();
		
		mainGroup = new Group();
		mainGroup.setParentId(UMConstants.MAIN_GROUP_ID);
		mainGroup.setName("他山石" + System.currentTimeMillis());
		mainGroup.setDomain("D1");
		mainGroup.setGroupType(Group.MAIN_GROUP_TYPE);
		
		URL template = URLUtil.getResourceFileUrl("template/um/syncdata/template_DB.xml");
        String paramDesc = FileHelper.readFile(new File(template.getPath()));
        mainGroup.setSyncConfig(paramDesc);
        mainGroup.setFromGroupId("1");
		
		groupService.createNewGroup(mainGroup, "", "-1");
		log.debug(mainGroup + "\n");
 
        // 准备数据
        initSourceTable();
	}
 
	private void initSourceTable() {
		Connection conn = dbserver.getH2Connection();
		
        try {
        	if( conn.isClosed() ) {
        		dbserver = new H2DBServer();
        		conn = dbserver.getH2Connection();
        	}
        	
        	if( conn.isClosed() ) {
        		Assert.fail("H2 connection is closed!");
        	}
        		
            Statement stat = conn.createStatement();
            stat.execute("create table if not exists xxx_group" + 
                    "(" + 
                        "id            NUMBER(19) not null, " + 
                        "parentId      NUMBER(19), " + 
                        "seqNo         NUMBER(10), " + 
                        "name          VARCHAR2(50 CHAR), " + 
                        "description   VARCHAR2(255 CHAR), " + 
                        "disabled      NUMBER(1)" + 
                    ");" +
                    " alter table xxx_group add primary key (id); ");
            
            stat.execute("create table if not exists xxx_user" + 
                    "(" + 
                        "id           NUMBER(19) not null, " + 
                        "groupId      NUMBER(19) not null, " + 
                        "sex          NUMBER(1), " + 
                        "birthday     TIMESTAMP(6), " + 
                        "employeeNo   VARCHAR2(255 CHAR), " + 
                        "loginName    VARCHAR2(255 CHAR), " + 
                        "userName     VARCHAR2(255 CHAR), " + 
                        "password     VARCHAR2(255 CHAR), " + 
                        "email        VARCHAR2(255 CHAR), " + 
                        "disabled     NUMBER(1), " + 
                        "telephone    VARCHAR2(255 CHAR)" + 
                    ");" +
                    " alter table xxx_user add primary key (id); ");
            
            PreparedStatement ps = conn.prepareStatement("insert into xxx_group values (?, ?, ?, ?, ?, ?)");
            ps.setObject(1, 1L);
            ps.setObject(2, 0L);
            ps.setObject(3, 1);
            ps.setObject(4, "它山石");
            ps.setObject(5, "test test");
            ps.setObject(6, 0);
            ps.executeUpdate();
            
            ps.setObject(1, 2L);
            ps.setObject(2, 1L);
            ps.setObject(3, 1);
            ps.setObject(4, "子组1");
            ps.setObject(5, "test 1");
            ps.setObject(6, 0);
            ps.executeUpdate();
            
            ps.setObject(1, 3L);
            ps.setObject(2, 1L);
            ps.setObject(3, 2);
            ps.setObject(4, "子组2");
            ps.setObject(5, "test 2");
            ps.setObject(6, 0);
            ps.executeUpdate();
            
            ps = conn.prepareStatement("insert into xxx_user values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)");
            ps.setObject(1, 1L);
            ps.setObject(2, 1L);
            ps.setObject(3, 1);
            ps.setObject(4, new Timestamp(new Date().getTime()));
            ps.setObject(5, "BL00618-001");
            ps.setObject(6, "JonKing-001");
            ps.setObject(7, "易水寒");
            ps.setObject(8, "123456");
            ps.setObject(9, "boubei@163.com");
            ps.setObject(10, 0);
            ps.setObject(11, "13588833834");
            ps.executeUpdate();
            
            ps.setObject(1, 2L);
            ps.setObject(2, 3L);
            ps.setObject(3, 1);
            ps.setObject(4, new Timestamp(new Date().getTime()));
            ps.setObject(5, "BL00619");
            ps.setObject(6, "Waitwint");
            ps.setObject(7, "苦行");
            ps.setObject(8, "123456");
            ps.setObject(9, "waitwind@gmail.com");
            ps.setObject(10, 1);
            ps.setObject(11, "13588833833");
            ps.executeUpdate();
            
            ps.setObject(1, 3L);
            ps.setObject(2, 4L);
            ps.setObject(3, 1);
            ps.setObject(4, new Timestamp(new Date().getTime()));
            ps.setObject(5, "BL00619");
            ps.setObject(6, "Waitwint");
            ps.setObject(7, "过河卒子");
            ps.setObject(8, "123456");
            ps.setObject(9, "jon@gmail.com");
            ps.setObject(10, 0);
            ps.setObject(11, "13588833832");
            ps.executeUpdate();
            
            // 邮箱、手机重复
            ps.setObject(1, 4L);
            ps.setObject(2, 3L);
            ps.setObject(3, 1);
            ps.setObject(4, new Timestamp(new Date().getTime()));
            ps.setObject(5, "BL00619");
            ps.setObject(6, "Waitwint2");
            ps.setObject(7, "过河卒子");
            ps.setObject(8, "123456");
            ps.setObject(9, "waitwind@gmail.com");
            ps.setObject(10, 0);
            ps.setObject(11, "13588833833");
            ps.executeUpdate();
            
        } catch (Exception e) {
            logger.error(e);
        }  
    }
 
	@Test
	public void testSyncDB() {
		Long mainGroupId = mainGroup.getId();
		Map<String, Object> datasMap = syncService.getCompleteSyncGroupData(mainGroupId);
		List<?> groups = (List<?>)datasMap.get("groups");
        List<?> users  = (List<?>)datasMap.get("users");
        int totalCount = users.size() + groups.size();
	    
        Assert.assertEquals(7, totalCount); // 3个组 + 4个用户
		Progress progress = new Progress(totalCount);
		((Progressable)syncService).execute(datasMap, progress );
		
		List<User> userList = groupService.getUsersByGroupId(mainGroupId); 
		Assert.assertTrue(userList.size() >= 1);
		User user1 = userList.get(0);
		Assert.assertEquals("JonKing-001", user1.getLoginName());
		Assert.assertEquals("易水寒", user1.getUserName());
		Assert.assertEquals("BL00618-001", user1.getEmployeeNo());
		Assert.assertEquals("1", user1.getSex());
		Assert.assertEquals("boubei@163.com", user1.getEmail());
		
		// 再增加同步一次，相同账号用户更更新。及重名组已经存在的情况（新建的子组，非同步过来）
		groups = permissionHelper.getEntities("from Group g where g.groupType=1 order by  g.decode");
		Group tg = (Group) groups.get(groups.size() - 1);
		tg.setFromGroupId(null);
		groupService.editExistGroup(tg, "-1", "-1");
		
		((Progressable)syncService).execute(datasMap, progress );
		((Progressable)syncService).execute(datasMap, progress ); // 同步两边
		
		userList = groupService.getUsersByGroupId(mainGroupId); 
		Assert.assertTrue(userList.size() >= 1);
		
		// testSyncUserJob
		SyncUserJob job = new SyncUserJob();
		job.excuteJob(mainGroupId + ",tss");
		
		job.excuteJob(" ");
		
		try {
			job.excuteJob( "999,tss"); // id=999的组不存在
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals("用户同步配置异常，找不到组，jobConfig=999,tss", e.getMessage());
		}
		
		Group customerGroup = super.createGroup(Group.CUSTOMER_GROUP, mainGroup.getId());
		customerGroup.setDomain("D1");
		userService.moveUser(user1.getId(), customerGroup.getId());
		Assert.assertEquals(Group.CUSTOMER_GROUP, loginSerivce.getMainGroup(user1.getId())[1]);
		
		UserDTO d1 = new UserDTO();
		d1.setUserName(user1.getUserName());
		d1.setDisabled(1);
		syncService.updateUser(user1, customerGroup, d1);
		
		
		((Progressable)syncService).execute(datasMap, progress );
		Assert.assertEquals("它山石", loginSerivce.getMainGroup(user1.getId())[1]);
		
		Group nowGroup = super.createGroup("D2-G1", mainGroup.getParentId());
		nowGroup.setDomain("D2");
		syncService.updateUser(user1, nowGroup, d1);
	}

	@Test
	public void syncData() {
		try {
			groupAction.syncData(response, mainGroup.getId());
		} catch (Exception e) {
			Assert.assertTrue("进度条需要单独起线程，里面没有事务.no transaction is in progress", true);
		}
		
		try { Thread.sleep(1000); } catch (InterruptedException e) { }
	}
}
