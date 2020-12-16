/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss._TestUtil;
import com.boubei.tss.cms.AbstractTest4CMS;
import com.boubei.tss.cms.CMSConstants;
import com.boubei.tss.cms.entity.Channel;
import com.boubei.tss.cms.entity.permission.ChannelResource;
import com.boubei.tss.framework.sso.Anonymous;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.BeanUtil;

import junit.framework.Assert;

/**
 * 文章站点栏目相关模块的单元测试。
 */
public class ChannelModuleTest extends AbstractTest4CMS {
 
	@Test
    public void testChannelModule() {
    	Long channelId = -92L;
    	try {
    		channelDao.getChannelsBySiteIdNoPermission(channelId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
		}
    	
    	Channel site = new Channel();
    	site.setPath( super.tempDir1.getPath() + "/../application.properties");
    	site.setDocPath("doc");
        site.setImagePath("img");
        try {
    		channelService.createSite(site);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.CMS_12, e.getMessage());
		}
    	
        // 新建站点
        site.setName("我的门户" + System.currentTimeMillis());
        site.setPath(super.tempDir1.getPath());
        site.setOverdueDate("0");
        channelAction.saveSite(response, site);
        Long siteId = site.getId();
        assertNotNull(siteId);
        assertEquals(siteId, channelDao.getSiteByChannel(siteId).getId());
        
        channelAction.saveSite(response, site);
        
        channelAction.getSiteDetail(response, CMSConstants.DEFAULT_NEW_ID);
        channelAction.getSiteDetail(response, siteId);
        
        // 新建栏目
        channelAction.getChannelDetail(response, CMSConstants.DEFAULT_NEW_ID, siteId);
        
        Channel channel1 = new Channel();
        channel1.setName("时事评论");
        channel1.setParentId(siteId);
        channelAction.saveChannel(response, channel1);
        channelId = channel1.getId();
        assertNotNull(channelId);
        Assert.assertEquals(channel1.getResourceType(), new ChannelResource().getResourceType());
        
        channelAction.getChannelDetail(response, channelId, channel1.getParentId());
        channelAction.getChannelDetail(response, CMSConstants.DEFAULT_NEW_ID, channelId);
        
        channelAction.saveChannel(response, channel1); // update
        
        Channel channel2 = super.createChannel("体育新闻", channel1, siteId);
        Channel channel3 = super.createChannel("NBA战况", channel2, channel2.getId());
        
        channel2.setOverdueDate("1");
        channel3.setOverdueDate("2");
        
        List<?> list = channelService.getAllSiteChannelList();
        assertTrue(list.size() >= 3);
        for(Object temp : list) {
            log.debug(temp);
        }
        
        // 栏目排序
        channelAction.sortChannel(response, channelId, channel2.getId(), 1);
        log.debug(channel1);
        log.debug(channel2);
        assertTrue(channel1.getSeqNo() > channel2.getSeqNo());
        
        // 栏目移动
        channelAction.moveChannel(response, channel3.getId(), channelId);
        assertTrue(channel3.getDecode().startsWith(channel1.getDecode()));
        assertEquals(channel3.getParentId(), channelId);
        
        // 停用启用
        channelAction.disable(response, siteId);
        
        site = channelService.getChannelById(siteId);
        channel1 = channelService.getChannelById(channel1.getId());
        assertEquals(site.getDisabled(), ParamConstants.TRUE);
        assertEquals(channel1.getDisabled(), ParamConstants.TRUE);
        
        channelAction.enable(response, siteId);
        
        site = channelService.getChannelById(siteId);
        channel1 = channelService.getChannelById(channel1.getId());
        assertEquals(site.getDisabled(), ParamConstants.FALSE);
        assertEquals(channel1.getDisabled(), ParamConstants.FALSE);
        
        channelAction.disable(response, channelId);
        
        channel1 = channelService.getChannelById(channel1.getId());
        assertEquals(channel1.getDisabled(), ParamConstants.TRUE);
        
        channelAction.enable(response, channel3.getId());
        
        channel1 = channelService.getChannelById(channel1.getId());
        channel3 = channelService.getChannelById(channel3.getId());
        assertEquals(channel1.getDisabled(), ParamConstants.FALSE);
        assertEquals(channel3.getDisabled(), ParamConstants.FALSE);
        
        channelAction.getOperations(response, channelId);
        channelAction.getOperations(response, -99L);
        
        channelAction.getChannelAll(response);
        
        // 切换为匿名用户登陆，尝试删除、停用
        login(UMConstants.ANONYMOUS_USER_ID, Anonymous.one.getLoginName());
        try {
    		channelService.disable(channelId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.CMS_8, e.getMessage());
		}
        try {
    		channelService.deleteChannel(channelId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals(EX.CMS_7, e.getMessage());
		}
        // test permissionFilter for move/sort/create/update
        try {
        	Channel temp = new Channel();
        	BeanUtil.copy(temp, channel1, "id".split(","));
        	temp.setName("LSB");
        	channelService.createChannel(temp);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.U_14, siteId) , e.getMessage());
		}
        try {
        	channelService.updateChannel(channel1);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.U_18, channel1.getName()) , e.getMessage());
		}
        try {
        	channelAction.sortChannel(response, channel2.getId(), channel1.getId(), -1);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.U_17, siteId) , e.getMessage());
		}
        try {
        	channelAction.moveChannel(response, channel2.getId(), siteId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.U_15, siteId) , e.getMessage());
		}
        
        String permissions = UMConstants.ANONYMOUS_USER_ID + "|111110111";
        permissionService.saveResource2Roles("tss", CMSConstants.RESOURCE_TYPE_CHANNEL, siteId, "1", permissions);
        try {
        	channelAction.moveChannel(response, channel2.getId(), siteId);
			Assert.fail("should throw exception but didn't.");
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(EX.U_16, channel2.getId()) , e.getMessage());
		}
        
        // 栏目站点删除
        login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
        
        channelAction.delete(response, channel2.getId());
        channelAction.delete(response, siteId);
        
        list = channelDao.getEntities(" from Channel where site.id = ?", siteId);
        assertTrue(list.size() == 0);
        
        assertTrue(_TestUtil.printLogs(logService) > 0);
    }
    
}
