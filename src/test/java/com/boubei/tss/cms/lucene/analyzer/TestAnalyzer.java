/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.cms.lucene.analyzer;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;


/**
 * WhitespaceAnalyzer不会把指定的单词转换为小写形式，它以空格作为边界，将空格间的内容切分为最小的语汇单元。
 * SimpleAnalyzer会保留那些所谓的停用词，但是它会把指定域中的所有单词转换为小写形式，并以非字母字符作为单个语汇单元的边界。
 * StopAnalyzer和StandardAnalyzer都删除了单词“the”。

 * 一个好的分词分析器要有一个庞大优良的词库以及设计优秀的数据结构。
 * lucene自带的几个分词程序中：
 * ChineseAnalyzer是按字分的,与StandardAnalyzer对中文的分词没有大的区别.
 * CJKAnalyzer是按两字切分的, 比较武断,并且会产生垃圾Token,影响索引大小。
 * 
 * 问题：
 * 
 *  另外，一般来说，中文分词常会有以下问题：
     1、分词的缺失: 就似乎同义词. 为了减少用户搜索的次数, 增加搜索效果. 如果用户搜 "北京 饭店" 能不能把"首都 饭店"也列出来呢. 这个分词器无能为力. 
     就似乎如果搜索 锐器, 系统会自动把匕首,尖刀等词汇一并加入搜索结果. 
     所以这个问题 ,就只能是在分词之前，再加一层:同义词返回模块。关键是词库的建立。
     
     2、优先级：例如:我还清晰地记得我们坐在江边聊天的情景.
     分出来是： 我 还清 晰 地 记得 我们 坐在 江边 聊天 的 情景。
     结果： 清晰 被拆开了.
     这个是基于词库的分词算法固有的问题.没有很好的解决方法。
     有这样的统计结果，单纯使用正向最大匹配的错误率为1/169，单纯使用逆向最大匹配的错误率为1/245.
     有一种解决方案是正向匹配结果后再逆向匹配一次,然后比较结果,消除歧义.最好加入词汇概率统计功能.有歧义的用概率决定.
     
     3、最大匹配的问题：比如 三季度 
     词库里同时有 三季度 和 季度 这两个词，分词时按最大正向匹配 则 三季度 被分成一个完整的词，按 季度 去检索反而检索不出来了。
     解决办法：缩短分词粒度，当字数等于或超过该粒度参数，且能成词，该词就被切分出来。
     
 */
public class TestAnalyzer {
    
    // 检索内容
    static String text = "2008年前三季度，美国次贷危机升级，全球金融持续动荡，世界经济增长全面放缓，" +
            "全球经济增长动力减弱，世界主要经济体与新兴市场正面临巨大的外部冲击。"; 
    
    static Analyzer analyzer0 = new ChineseAnalyzer();
    static Analyzer analyzer1 = new WhitespaceAnalyzer();
    static Analyzer analyzer2 = new SimpleAnalyzer();
    static Analyzer analyzer3 = new StandardAnalyzer();
    static Analyzer analyzer4 = new StopAnalyzer();
//    static Analyzer analyzer5 = new MMAnalyzer(2);
//    static Analyzer analyzer6 = new MMAnalyzer();
    static Analyzer analyzer7 = new CJKAnalyzer(); 
    static Analyzer analyzer8 = new org.apache.lucene.analysis.cjk.CJKAnalyzer(); 
    
    public static void main(String[] args) throws IOException {
        displayTokens(analyzer8, text);
    }

    public static void displayTokens(Analyzer analyzer, String text) throws IOException {
        TokenStream stream = analyzer.tokenStream("contents", new StringReader(text));
        while (true) {
            Token token = stream.next();
            if (token == null) break;
            
            System.out.print("[" + token.termText() + "] ");
        }
    }
}
