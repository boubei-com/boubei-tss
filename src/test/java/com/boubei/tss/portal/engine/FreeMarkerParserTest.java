/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.portal.engine;
 
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.boubei.tss.cms.service.IRemoteArticleService;
import com.boubei.tss.framework.sso.IdentityCard;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.portal.AbstractTest4Portal;
import com.boubei.tss.portal.PortalConstants;
import com.boubei.tss.portal.dao.INavigatorDao;
import com.boubei.tss.portal.entity.Navigator;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.XMLDocUtil;

public class FreeMarkerParserTest extends AbstractTest4Portal {
	
	@Autowired INavigatorDao navigatorDao;
	
	Navigator menu;
	
	public void init()  {
		super.init();
		
		menu = new Navigator();
        menu.setType(Navigator.TYPE_MENU);
        menu.setName("测试菜单" + System.currentTimeMillis());
        menu.setPortalId(0L);
        menu.setParentId(PortalConstants.ROOT_ID);
        menu.setSeqNo(navigatorDao.getNextSeqNo(menu.getParentId()));
        navigatorDao.save(menu);
	}
	
	// for FreeMarker
	protected void initIdentityCard() {
		request = request == null ? new MockHttpServletRequest() : request;
        Context.initRequestContext(request);
        
        IdentityCard card = new IdentityCard("token", OperatorDTO.ADMIN);
        Context.initIdentityInfo(card);
	}
    
    @Test
    public void test0() throws Exception {
        initIdentityCard();
        
        String templateStr = "<#assign Environment = statics[\"com.boubei.tss.framework.sso.Environment\"] />" +
                "<#setting number_format=\"0\">" +
                "${Environment.getUserId()?string.number}";
        
        FreemarkerParser parser = new FreemarkerParser(null);
        parser.parseTemplateTwice(templateStr, new OutputStreamWriter(System.out));
    }

    @Test
    public void test1() throws Exception {
        String templateStr = "<#assign manager = statics[\"com.boubei.tss.portal.engine.StaticManager\"] />" +
                "<#assign data = manager.listFiles(\"" + FileHelper.ioTmpDir() + "\") />" +
                "${data.get(0)} \n <#list data as file> ${file} \n </#list> ";

        FreemarkerParser parser = new FreemarkerParser(null);
        parser.parseTemplateTwice(templateStr, new OutputStreamWriter(System.out));
    }
    
    @Test
    public void test2() throws Exception {
        String div = "<DIV id=span_div_27 style=\"FONT-SIZE: 1px; Z-INDEX: 10; LEFT: 0px; VISIBILITY: hidden;" +
                " POSITION: relative; TOP: 7px; HEIGHT: 1px\"></DIV>";
        
        String templateStr = "" +
             "<#assign manager = statics[\"com.boubei.tss.portal.engine.StaticManager\"] />" +
             "<#assign data = manager.getNavigatorService().getNavigatorXML(" + menu.getId() + ") />" +
             "<#assign doc  = statics[\"freemarker.ext.dom.NodeModel\"].parse(manager.translateValue(data)) />" + 
             "<#assign menu = doc.Menu>" + 
             "<TABLE height=33 cellSpacing=0 cellPadding=0 width=\"90%\" align=center><TBODY><TR> \n" + 
             "<#list menu.MenuItem as item>" + 
             "<TD class=mainTd id=td_${item.@id}><SPAN class=menuSpan id=manegeMenu_${item.@id}>" +
             "<a href='${item.@url}'>${item.@name}</a>" +
             "</SPAN>" + div + "</TD> \n" + 
             "</#list>" +
             "</TR></TBODY></TABLE>";

        FreemarkerParser parser = new FreemarkerParser(null);
        parser.parseTemplateTwice(templateStr, new OutputStreamWriter(System.out));
    }
    
    @Test
    public void test3() throws Exception {
    	initIdentityCard();
    	
        String tempalteStr = "<@common.showMenu menuId=" + menu.getId() + "/>";
        FreemarkerParser parser = new FreemarkerParser(null);
        parser.parseTemplateTwice(tempalteStr, new OutputStreamWriter(System.out));
    }
    
    @Test
    public void test4() throws Exception {
    	FreemarkerParser parser = new FreemarkerParser(null);
        parser.getDataModel().put("article", 
        		FreemarkerParser.translateValue(XMLDocUtil.createDoc("testdata/article.xml").asXML()));
        parser.getDataModel().put("articleList", 
        		FreemarkerParser.translateValue(XMLDocUtil.createDoc("testdata/articleList.xml").asXML()));
        
        String templateStr = "<#assign channel = articleList.ArticleList.channel>" +
                "<#list channel.item as item>" +
                "<a href='/tss/article2.portal?articleId=${item.id}'>* ${item.title}</a><br>\n" +
                "</#list>";
        
        parser.parseTemplateTwice(templateStr, new OutputStreamWriter(System.out));
    }
    
    @Test
    public void test5() throws Exception {
        IRemoteArticleService obj = StaticManager.getArticleService();
        
        obj.getArticleListByChannel(48L, 1, 5, true);
        
        String templateStr = "<@common.getArticleListXML 3, 1, 5/>";
        
        FreemarkerParser parser = new FreemarkerParser(null);
        parser.parseTemplateTwice(templateStr, new OutputStreamWriter(System.out));
    }
    
    @Test
    public void test6() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("channelId", "12");
        map.put("x1", "12".split(","));
        map.put("x2", new String[]{});
        FreemarkerParser parser = new FreemarkerParser(null);
        parser.putParameters(map);
        parser.putParameters(null);
        
        String templateStr = "${channelId}" +
                "<#assign id = channelId/>${id}";
        parser.parseTemplateTwice(templateStr, new OutputStreamWriter(System.out));
    }
    
    @Test
    public void test7() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("channelId", "22");
        FreemarkerParser parser = new FreemarkerParser();
        parser.putParameters(map);
        
        String templateStr = "<#assign Long = statics[\"java.lang.Long\"] />" +
                             "<#assign Test = statics[\"com.boubei.tss.portal.engine.FreeMarkerParserTest\"] />" +
                             "<#macro show channelId>" +
                             "   <#assign num = Test.increase(channelId)/>" +
                             "   ${num}\n" +
                             "</#macro>" +
                             "<@show channelId/>" +
                             "<@show Long.valueOf(channelId)/>" +
                             "";
        
        parser.parseTemplateTwice(templateStr, new OutputStreamWriter(System.out));
        
        StringWriter out = new StringWriter();
        parser.parseTemplateTwice(templateStr, out);
        System.out.println(out.toString());
    }
    
    @Test
    public void test8() throws Exception {
    	String tpl = "<#if xx?contains(\"ice\")>It contains ice! \n</#if>" +
    			"<#assign xxx = [1, 2, 3]>" +
    			" ${xxx?seq_contains(1)?string(\"yes\n\", \"no\n\")}" +
    			"<#if xxx?seq_contains(2)>It contains 2! \n</#if>" +
    			"<#if xxxx?seq_contains(12)>It contains 12! \n</#if>";
    	
    	Map<String, Object> map = new HashMap<String, Object>();
        map.put("xx", "piceous");
        
        List<Long> list = new ArrayList<Long>();
        list.add(11L);
        list.add(12L);
        list.add(13L);
        map.put("xxxx", list);
        FreemarkerParser parser = new FreemarkerParser();
        parser.putParameters(map);
        parser.parseTemplateTwice(tpl, new OutputStreamWriter(System.out));
    }
    
    public static Long increase(Long num){
        return new Long(num.longValue() + 1);
    }
    
    public static Long increase(String num){
        return new Long(num);
    }
}  


