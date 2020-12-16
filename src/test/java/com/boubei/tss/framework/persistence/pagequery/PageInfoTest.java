/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence.pagequery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
 
public class PageInfoTest {

    private PageInfo page;
 
    @Before
    public void setUp() {
        page = new PageInfo();
        page.setPageSize(20);
        page.setTotalRows(0);
    }
 
    @Test
    public void testGetPageSize() {
        assertEquals("默认值", 20, page.getPageSize());
        page.setPageSize(12);
        assertTrue(12 ==  page.getPageSize());
    }

    @Test
    public void testGetTotalPages() {
    	assertTrue("totalRows = 0", page.getTotalPages() == 0);
        page.setTotalRows(22);
        assertTrue("totalRows % pageSize > 0", page.getTotalPages() == 2);
        page.setTotalRows(40);
        assertTrue("totalRows % PageSize = 0", page.getTotalPages() == 2);
        
        Assert.assertEquals(10, PageInfo.calTotalPages(1000, 100));
        Assert.assertEquals(11, PageInfo.calTotalPages(1001, 100));
    }

    @Test
    public void testGetPageNum() {
    	assertTrue("totalRows = 0", page.getPageNum() == 1);
        page.setTotalRows(12);
        assertTrue("totalRows > 0, pageNum = null", page.getPageNum() == 1);
        page.setPageNum(0);
        assertTrue("totalRows > 0, pageNum = 0", page.getPageNum() == 1);
        page.setPageNum(2);
        assertTrue("totalRows > 0, pageNum > totalPages", page.getPageNum() == 2);
        page.setPageNum(2);
        page.setTotalRows(42);
        assertTrue("totalRows > 0, pageNum < totalPages", page.getPageNum() == 2);
    }

    @Test
    public void testGetFirstPageNum() {
    	assertTrue("totalRows = 0", page.getFirstPageNum() == 1);
        page.setTotalRows(12);
        assertTrue("totalRows > 0, totalPages = 1", page.getFirstPageNum() == 1);
        page.setTotalRows(22);
        page.setPageNum(1);
        assertTrue("totalRows > 0, totalPages > 1, pageNum = 1", page.getFirstPageNum() == 1);
        page.setPageNum(2);
        assertTrue("totalRows > 0, totalPages > 1, pageNum > 1", page.getFirstPageNum() == 1);
    }

    @Test
    public void testGetNextPageNum() {
    	assertTrue("totalRows = 0", page.getNextPageNum() == 0);
        page.setTotalRows(12);
        assertTrue("totalRows > 0, pageNum = totalPages", page.getNextPageNum() == 0);
        page.setTotalRows(22);
        assertTrue("totalRows > 0, pageNum < totalPages", page.getNextPageNum() == 2);
        page.setPageNum(3);
        assertTrue("totalRows > 0, pageNum > totalPages", page.getNextPageNum() == 0);
    }

    @Test
    public void testGetLastPageNum() {
    	assertTrue("totalRows = 0", page.getLastPageNum() == 0);
        page.setTotalRows(12);
        assertTrue("totalRows > 0, pageNum = totalPages", page.getLastPageNum() == 1);
        page.setTotalRows(22);
        assertTrue("totalRows > 0, pageNum < totalPages", page.getLastPageNum() == 2);
        page.setPageNum(3);
        assertTrue("totalRows > 0, pageNum > totalPages", page.getLastPageNum() == 2);
    }

    @Test
    public void testGetPrePageNum() {
    	assertTrue("totalRows = 0", page.getPrePageNum() == 0);
        page.setTotalRows(22);
        assertTrue("totalRows > 0, pageNum = 1", page.getPrePageNum() == 0);
        page.setTotalRows(42);
        page.setPageNum(3);
        assertTrue("totalRows > 0, pageNum > 1", page.getPrePageNum() == 2);
        page.setPageNum(0);
        assertTrue("totalRows > 0, pageNum < 1", page.getPrePageNum() == 0);
    }

    @Test
    public void testGetPageRows() {
    	assertTrue("totalRows = 0", page.getPageRows() == 0);
        page.setTotalRows(42);
        page.setPageNum(2);
        assertEquals("totalRows > 0, pageNum < totalPages", 20, page.getPageRows());
        page.setPageNum(3);
        assertTrue("totalRows > 0, pageNum = totalPages", page.getPageRows() == 2);
    }

    @Test
    public void testToString() {
        String xml = "<pagelist totalpages=\"0\" totalrecords=\"0\" "
                + "currentpage=\"1\" pagesize=\"20\" firstpage=\"1\" prepage=\"0\" "
                + "nextpage=\"0\" lastpage=\"0\" pagerecords=\"0\"/>";
        assertEquals("totalRows = 0", xml, page.toString());
        
        assertTrue(0 == page.toJson().get("total"));
        assertEquals(0, page.toEasyUIDataGrid().get("total"));
    }

    @Test
    public void testGetItems() {
        List<?> items = new ArrayList<Object>();
        page.setItems(items);
        assertEquals(items, page.getItems());
    }
}
