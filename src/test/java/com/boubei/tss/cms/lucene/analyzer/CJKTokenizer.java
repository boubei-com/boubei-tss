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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.TreeMap;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
 
public class CJKTokenizer extends Tokenizer {

    // 这个TreeMap用来缓存词库
    private static TreeMap<String, String> simWords = null;

    private int bufferIndex = 0;

    private int dataLen = 0;

    private final char[] ioBuffer = new char[256];

    private String tokenType = "word";

    public CJKTokenizer(Reader input) {
        this.input = input;
    }

    // 这里是lucene分词器实现的最关键的地方
    public Token next() throws IOException {
        loadWords();

        StringBuffer currentWord = new StringBuffer();
        while (true) {
            char c;
            Character.UnicodeBlock ub;

            if (bufferIndex >= dataLen) {
                dataLen = input.read(ioBuffer);
                bufferIndex = 0;
            }

            if (dataLen == -1) {
                if (currentWord.length() == 0) { return null; } 
                else { break; }
            } else {
                c = ioBuffer[bufferIndex++];
                ub = Character.UnicodeBlock.of(c);
            }
            // 通过这个条件不难看出这里只处理了CJK_UNIFIED_IDEOGRAPHS，
            // 因此会丢掉其它的字符，如它会丢掉LATIN字符和数字
            // 这也是该lucene分词器的一个限制，您可以在此基础之上完善它，也很欢迎把您完善的结果反馈给我
            if (Character.isLetter(c) && ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                tokenType = "double";
                if (currentWord.length() == 0) {
                    currentWord.append(c);
                } else {
                    // 这里实现了正向最大匹配法
                    String temp = (currentWord.toString() + c).intern();
                    if (simWords.containsKey(temp)) {
                        currentWord.append(c);
                    } else {
                        bufferIndex--;
                        break;
                    }
                }
            }
        }
        Token token = new Token(currentWord.toString(), bufferIndex - currentWord.length(), bufferIndex, tokenType);
        currentWord.setLength(0);
        return token;
    }

    // 装载词库，您必须明白它的逻辑和之所以这样做的目的，这样您才能理解正向最大匹配法是如何实现的
    public void loadWords() {
        if (simWords != null)
            return;
        
        simWords = new TreeMap<String, String>();
        try {
            InputStream words = new FileInputStream("d:/Temp/simchinese.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(words, "UTF-8"));
            String word = null;

            while ((word = in.readLine()) != null) {
                // #使得我们可以在词库中进行必要的注释
                if ((word.indexOf("#") == -1) && (word.length() < 5) && (word.length() > 0)) {
                    simWords.put(word.intern(), "1");
                    if (word.length() == 3) {
                        if (!simWords.containsKey(word.substring(0, 2).intern())) {
                            simWords.put(word.substring(0, 2).intern(), "2");
                        }
                    }
                    if (word.length() == 4) {
                        if (!simWords.containsKey(word.substring(0, 2).intern())) {
                            simWords.put(word.substring(0, 2).intern(), "2");
                        }
                        if (!simWords.containsKey(word.substring(0, 3).intern())) {
                            simWords.put(word.substring(0, 3).intern(), "2");
                        }
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
