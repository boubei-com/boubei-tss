/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.sso.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.web.servlet.Servlet3MultiRequest;
import com.boubei.tss.util.XMLDocUtil;


/**
 * <p>
 * servlet请求合并测试对象。
 * 先将合并的总请求发到Servlet3MultiRequest，然后Servlet3MultiRequest解析总的请求，
 * 将一个个子请求分离出来分别执行（这里子请求一律重新请求到SimpleRequestServlet），
 * 再将每个子请求的返回结果合并为一个总的结果（响应）。
 * </p>
 */
public class MultiRequestServletTest {

	private Server server;
	 
    @Before
    public void setUp() throws Exception {
        
        server = new Server(8083); // 设置监听端口为8083
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(Servlet3MultiRequest.class, "/multi");
        context.addServlet(SimpleRequestServlet.class, "/simple/*");
        context.setContextPath("/tss");
        
        server.setHandler(context);
        
        server.start();
    }
    
    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    /**
     * Test method for {@link com.boubei.tss.core.web.servlet.Servlet3MultiRequest#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     */
    @Test
    public void testDoPost() throws ServletException, IOException {
        HttpClient client = new HttpClient();
        PostMethod httppost = new PostMethod("http://localhost:8083/tss/multi");
        //设置请求内容
        String body = "<Requests>" +
                "<Request url=\"/simple/1\"><Param><Name><![CDATA[token]]></Name><Value><![CDATA[12345]]></Value></Param></Request>" + 
                "<Request url=\"/simple/2\"><Param><Name><![CDATA[token]]></Name><Value><![CDATA[12w45]]></Value></Param></Request>" + 
                "<Request url=\"/simple/3\"></Request>" + 
                "<Request url=\"/simple/4\"><Param><Name><![CDATA[token]]></Name><Value><![CDATA[12s45]]></Value></Param></Request>" +
                "</Requests>";
        httppost.setRequestEntity(new StringRequestEntity(body, null, null));

        try {
            //请求
            client.executeMethod(httppost);
            int statusCode = httppost.getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                try {
                    Document doc = new SAXReader().read(httppost.getResponseBodyAsStream());
                    System.out.println(doc.asXML());
                    
                    List<Element> nodes = XMLDocUtil.selectNodes(doc, "/Responses/Response/url");
                    assertEquals(4, nodes.size());
                    
                    int i = 1; 
                    for(Node node :nodes) {
                        assertEquals("/simple/" + i++, node.getText());
                    }
                    
                } catch (DocumentException e) {
                    throw new BusinessException("解析合并请求的xml数据流失败", e);
                }
            } else {
                fail("请求连接失败");
            }
        } finally {
            httppost.releaseConnection();
        }
    }
    
    @Test
    public void testDoPostError() throws ServletException, IOException {
        HttpClient client = new HttpClient();
        PostMethod httppost = new PostMethod("http://localhost:8083/tss/multi");

        String body = "<Requests>" +
                "<Request url=\"/simple/1\"><Param><Name><![CDATA[token]]></Name><Value><![CDATA[12345]]></Value></Param></Request>" + 
                "<Request url=\"/simple/2\"><Param><Name><![CDATA[token]]></Name><Value><![CDATA[12w45]]></Value></Param></Request>" + 
                "</XXX>";
        httppost.setRequestEntity(new StringRequestEntity(body, null, null));

        try {
            //请求
            client.executeMethod(httppost);
            httppost.getStatusCode();
            System.out.println( httppost.getResponseBodyAsString() );
            
        } finally {
            httppost.releaseConnection();
        }
    }
}
