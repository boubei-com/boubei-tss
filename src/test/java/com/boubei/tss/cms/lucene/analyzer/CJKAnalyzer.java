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

import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
 
public class CJKAnalyzer extends Analyzer { // 继承了Analyzer抽象超类，这是lucene的要求
    public final static String[] STOP_WORDS = {};
    
    private Set<?> stopTable;    

    public CJKAnalyzer() {
        stopTable = StopFilter.makeStopSet(STOP_WORDS);
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new StopFilter(new CJKTokenizer(reader), stopTable);
    }    
 }

