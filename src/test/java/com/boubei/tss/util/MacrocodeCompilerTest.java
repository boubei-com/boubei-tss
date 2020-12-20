/* ==================================================================   
 * Created [2015/2016/2017] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.junit.Assert;

public class MacrocodeCompilerTest {
	
	@Test
    public void testRunLoop() {
		String script = "${X0}";
		Map<String, Object> macrocodes = new HashMap<String, Object>();
		for(int i = 0; i < 10; i++) {
			macrocodes.put("X" + i, "${X" + (i+1) + "}");
		}
		macrocodes.put("X10", "This Loop End!");
		
		String result = MacrocodeCompiler.runLoop(script, macrocodes);
		Assert.assertEquals("This Loop End!", result);
		
		Assert.assertEquals("${xx}", MacrocodeCompiler.createMacroCode("xx") );
		Assert.assertEquals("#{xx}", MacrocodeCompiler.createVariable("xx") );
		Assert.assertEquals("${xx}", MacrocodeCompiler.run("${xx}", null) );
	}
    
	@Test
    public void testRun() {
        String code = "<table id=\"${portlet.id}\" description=\"修饰器\">"
            + "             <tr>"
            + "                 <td>#{title}</td>"
            + "             </tr>"
            + "             <tr>    "
            + "                 <td>${js.content}</td>"
            + "             </tr>"
            + "         </table>${ignore}";

        Map<String, Object> macro = new HashMap<String, Object>();
        macro.put("${portlet.id}", "1001");
        macro.put("#{title}", new Object() {
                                public String toString() {
                                    return "HTML5可以做五件事情， 超出你的想象";
                                }
                            });
        macro.put("${d}", "|test");
        macro.put("${js.content}", "作为下一代的网页语言，HTML5 拥有很多让人期待已久的新特性，它可以说是近十年来 Web 标准最巨大的飞跃。");
        
//        System.out.println(MacrocodeCompiler.run(code, macro));
        
        String result = "<table id=\"1001\" description=\"修饰器\">" +
        		"             <tr>" +
        		"                 <td>HTML5可以做五件事情， 超出你的想象</td>" +
        		"             </tr>" +
        		"             <tr>" +
        		"                     <td>作为下一代的网页语言，HTML5 拥有很多让人期待已久的新特性，它可以说是近十年来 Web 标准最巨大的飞跃。</td>" +
        		"             </tr>" +
        		"         </table>";
        assertEquals( result, MacrocodeCompiler.run(code, macro, false) );
        assertEquals( result + "${ignore}", MacrocodeCompiler.run(code, macro, true) );
        
        assertEquals( "test|test", MacrocodeCompiler.run("test${d}", macro));
    }
	
	@Test
	public void testVsFreemarker() {
		String s = "{'gprice':'小果|79.0|44367.0|${GG小果79.0},中果|89.0|44368.0|${GG中果89.0},大果|99.0|44369.0|${GG大果99.0}','guige1_name':'规格','guige1':'小果|中果|大果'}";
		Map<String, Object> data = new HashMap<>();
		data.put("GG中果79.0", 44368);
		data.put("GG小果89.0", 44367);
		data.put("GG大果99.0", 44369);
		
		System.out.println( EasyUtils.fmParse(s, data) );
		System.out.println( MacrocodeCompiler.run(s, data) );
	}

}
