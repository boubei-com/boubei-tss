/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

//package com.boubei.tss.cms.lucene.analyzer;
//
//import java.io.IOException;
//
//import jeasy.analysis.MMAnalyzer;
//
//import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.TokenStream;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.IndexWriter;
//import org.apache.lucene.index.TermPositionVector;
//import org.apache.lucene.queryParser.QueryParser;
//import org.apache.lucene.search.Hits;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.highlight.Highlighter;
//import org.apache.lucene.search.highlight.QueryScorer;
//import org.apache.lucene.search.highlight.TokenSources;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.RAMDirectory;
//
//import com.boubei.tss.cms.lucene.AnalyzerFactory;
//
///**
// * <p>
// * MMAnalyzerTest.java
// * </p>
// * 分词效率： 第一次分词需要1－2秒（读取词典），之后速度基本与Lucene自带分词持平。内存消耗： 30M+
// * MMAnalyzer 单介：
// *  1、支持英文、数字、中文（简体）混合分词
// *  2、常用的数量和人名的匹配
// *  3、超过22万词的词库整理
// *  4、实现正向最大匹配算法
// *  5、词典的动态扩展
// *  
// * 常用API： 
// *  1、MMAnalyzer analyzer = new MMAnalyzer(); //采用正向最大匹配的中文分词算法，相当于分词粒度等于0
// *  2、MMAnalyzer analyzer = new MMAnalyzer(2); //参数为分词粒度：当字数等于或超过该参数，且能成词，该词就被切分出来
// *  3、MMAnalyzer.addDictionary(reader);  //增加一个新词典，采用每行一个词的读取方式
// *  4、MMAnalyzer.addWord(newWord);      //增加一个新词
// *  
// * 分词思路： 读取一个字，然后联想，直到联想到不能为止。如果当前可以构成词，便返回一个Token。
// *          如果当前不能构成词语，便回溯到最近的可以构成词语的节点，返回。
// *          最差的情况就是返回第一个单字。
// *          然后从返回结果的下一个字重新开始联想。
// */
//public class TestMMAnalyzer {
//    public static void main(String[] args) {
//        testSegment2();
//    }
//
//    static String text = "据路透社报道，印度尼西亚社会事务部一官员星期二(29日)表示，"
//        + "日惹市附近当地时间27日晨5时53分 发生 的里氏6.2级地震已经造成至少5427人死亡，"
//        + "20000余人受伤，近20万人无家可归。三季度。金普俊。我是额飞挺阿什顿";   // 检索内容
//    
//    public static void testSegment() {
//        //MMAnalyzer analyzer = new MMAnalyzer(2);
//        //System.out.println(MMAnalyzer.contains("科学"));
//        
//        MMAnalyzer analyzer = (MMAnalyzer) AnalyzerFactory.createAnalyzer("额飞挺");
//        try {
//            System.out.println(analyzer.segment(text, " | "));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void testSegment2() {
//        String fieldName = "text";
//
//        // 采用正向最大匹配的中文分词算法
//        Analyzer analyzer = new MMAnalyzer();
//
//        Directory directory = new RAMDirectory();
//        // Directory directory = FSDirectory.getDirectory("/tmp/testindex", true);
//
//        try {
//            IndexWriter iwriter = new IndexWriter(directory, analyzer, true);
//            iwriter.setMaxFieldLength(25000);
//            Document doc = new Document();
//            doc.add(new Field(fieldName, text, Field.Store.YES, Field.Index.TOKENIZED));
//            iwriter.addDocument(doc);
//            iwriter.close();
//
//            IndexSearcher isearcher = new IndexSearcher(directory);
//            QueryParser parser = new QueryParser(fieldName, analyzer);
//            Query query = parser.parse("印度尼西亚 6.2级地震"); // 检索词
//            Hits hits = isearcher.search(query);
//            System.out.println("命中：" + hits.length());
//
//            for (int i = 0; i < hits.length(); i++) {
//                Document hitDoc = hits.doc(i);
//                System.out.println("内容：" + hitDoc.get(fieldName));
//            }
//
//            isearcher.close();
//            directory.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    
//    /**
//     * 测试收索结果对关键字进行高亮显示。
//     */
//    public static void testSegment3() {
//        String fieldName = "text";
//
////      采用正向最大匹配的中文分词算法
//        Analyzer analyzer = new MMAnalyzer();
//
//        Directory directory = new RAMDirectory();
////        Directory directory = FSDirectory.getDirectory("/tmp/testindex", true);
//
//        try {
//            IndexWriter iwriter = new IndexWriter(directory, analyzer, true);
//            iwriter.setMaxFieldLength(25000);
//            Document doc = new Document();
//            doc.add(new Field(fieldName, text, Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
//            iwriter.addDocument(doc);
//            iwriter.close();
//
//            IndexSearcher isearcher = new IndexSearcher(directory);
//            QueryParser parser = new QueryParser(fieldName, analyzer);
//            Query query = parser.parse("印度尼西亚 6.2级地震");// 检索词
//            Hits hits = isearcher.search(query);
//            System.out.println("命中：" + hits.length());
//
//            Highlighter highlighter = new Highlighter(new QueryScorer(query));
//            for (int i = 0; i < hits.length(); i++) {
//                text = hits.doc(i).get(fieldName);
//                TermPositionVector tpv = (TermPositionVector) IndexReader.open(directory).getTermFreqVector(hits.id(i), fieldName);
//                TokenStream tokenStream = TokenSources.getTokenStream(tpv);
//                String result = highlighter.getBestFragments(tokenStream, text, 3, "...");
//                System.out.println("内容：" + result);
//            }
//
//            isearcher.close();
//            directory.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
