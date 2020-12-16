/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.lucene;
 
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import com.boubei.tss.cms.lucene.executor.IndexExecutorFactory;
import com.boubei.tss.cms.lucene.executor.TitleIndexExecutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.URLUtil;

public class TestQuery {
	
    private static Logger log = Logger.getLogger(TestQuery.class);

    public static void main(String[] args) throws IOException, ParseException {
        test2();
    }
    
    public static void test1() throws IOException, ParseException {
        Hits hits = null;
        String queryString = "三 季度";
        Query query = null;
        
        String path = URLUtil.getResourceFileUrl("application.properties").getPath();
        IndexSearcher searcher = new IndexSearcher(path + "/index/1363");

        Analyzer analyzer = new StandardAnalyzer();
        try {
            QueryParser qp = new QueryParser("title", analyzer);
            query = qp.parse(queryString);
        } catch (ParseException e) {
        }
        
        long startTime = new Date().getTime();
        if (searcher != null) {
            hits = searcher.search(query);
            if (hits.length() > 0) {
                System.out.println("找到:" + hits.length() + " 个结果!");
                for (int i = 0; i < hits.length(); i++) {
                    Document hitDoc = hits.doc(i);
                    System.out.println("内容：" + hitDoc.get("title"));
                }
            }
        }
        long endTime = new Date().getTime();
        log.debug("这花费了" + (endTime - startTime) + " 毫秒搜索!");
    }

    public static void test2() {
        String searchStr = "+ 季度 -杭州";
       // searchStr = "title: 杭州 AND contents:季度";

        try {
            IndexSearcher searcher = new IndexSearcher("E:/cms/demo/jh/index/1363");
            Query query = IndexExecutorFactory.create(TitleIndexExecutor.class.getName()).createIndexQuery(searchStr);
            Hits hits = searcher.search(query, new Sort(new SortField("createTime", SortField.STRING, true))); // 按创建时间排序
            
            int page = 1;
            int pageSize = 12;
            log.info("总记录" + hits.length());
            for (int i = (page - 1) * pageSize; i < hits.length() && i < page * pageSize; i++) {
                org.apache.lucene.document.Document document = hits.doc(i);
                
                Date createTime = document.get("createTime") == null ? null : DateUtil.parse(document.get("createTime"));
                
                log.info(DateUtil.format(createTime) + ":" + document.get("title") + ":" + document.get("author"));
            }
            searcher.close();
        } catch (Exception e) {
            throw new BusinessException("搜索出错!", e);
        } 
    }
}
