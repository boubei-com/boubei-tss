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
 
import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import com.boubei.tss.util.FileHelper;

/**
 * 测试对txt附件进行索引。
 */
public class TestIndexTXTFile {
    
    public static void main(String[] args) throws Exception {
        
        /* 指明要索引文件夹的位置 */
        File fileDir = new File("d:/temp/lucence/");

        /* 这里放索引文件的位置 */
        File indexDir = new File("d:/temp/lucence/index");
        
        Analyzer luceneAnalyzer = new StandardAnalyzer();
        IndexWriter indexWriter = new IndexWriter(indexDir, luceneAnalyzer, true);
        File[] textFiles = fileDir.listFiles();
        long startTime = System.currentTimeMillis();

        // 增加document到索引去
        int index = 0;
        for (File file : textFiles)  {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                System.out.println("File " + file.getCanonicalPath() + "正在被索引....");
                String temp =  FileHelper.readFile(file, "GBK");
 
                Document document = new Document();
                Field FieldPath = new Field("path", file.getPath(), Field.Store.YES, Field.Index.NO);
                Field FieldBody = new Field("body", temp, Field.Store.YES, Field.Index.TOKENIZED);
                Field tt = new Field("tt", temp + index++, Field.Store.YES,  Field.Index.TOKENIZED);
                
                document.add(FieldPath);
                document.add(FieldBody);
                document.add(tt);
                indexWriter.addDocument(document);
            }
        }
        // optimize()方法是对索引进行优化
        indexWriter.optimize();
        indexWriter.close();

        // 测试一下索引的时间
        long endTime = System.currentTimeMillis();
        System.out.println("这花费了" + (endTime - startTime) + " 毫秒来把文档增加到索引里面去!" + fileDir.getPath());
    }
}
