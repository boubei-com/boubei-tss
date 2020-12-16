/* ==================================================================   
 * Created [2006-6-19] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018  
 * ================================================================== 
*/
package com.boubei.tss.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.script.ScriptEngineManager;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.boubei.tss.PX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.param.ParamConfig;

import freemarker.template.Configuration;
import freemarker.template.Template;

/** 
 * <p> EasyUtils.java </p> 
 * 
 * 一些常用的工具方法
 */
public class EasyUtils {
    
    static Logger log = Logger.getLogger(EasyUtils.class);
    
    /**
     * 判断是否为数字（正负整数）
     */
    public static boolean isDigit(String s) {
    	return EasyUtils.isNullOrEmpty(s) ? false : s.matches("^(-|\\+)?[0-9]*$");  // ^-?\\d+$
    }
    
    public static boolean isTimestamp(Long val) {
		return val != null && val > 1510000000000L;
    }
    
    public static final Long str2Long(String s) {
    	if( isDigit(s) ) {
    		return Long.valueOf(s);
    	}
    	return null;
    }
    
    /**
     * 将对象转换成Double。
     * 用于SQL取出的数据类型字段处理，因为double 单元测试环境下取出的是BigDecimal，jobss下取出的是Double。
     * 统一转为String再转回Double。
     * 
     * @param value
     * @return
     */
    public static final Double obj2Double(Object value) {
        try{
        	return Double.valueOf( toNumberString(value) ); // value = .5 
        } 
        catch (Exception e) {
            throw new RuntimeException("【" +value+ "】不是有效数字");
        }
    }
    
    public static final Long obj2Long(Object value) {
        try{
        	return Long.valueOf( toNumberString(value) );
        } 
        catch (Exception e) {
        	throw new RuntimeException("【" +value+ "】不是有效数字");
        }
    }
    
    public static final Integer obj2Int(Object value) {
        Long lv = obj2Long(value);
        if(lv > 2147483647) {
        	throw new RuntimeException("【" +value+ "】过大，不是int");
        }
		return lv.intValue();
    }
    
    private static final String toNumberString(Object value) {
    	String _val = obj2String(value).trim();
    	if( _val.length() == 0) {
    		_val = "0";
    	}
    	return _val;
    }
    
    public static final String obj2String(Object value) {
        if( isNullOrEmpty( value ) ) { // "null"、"undefined"均为空字符串处理
        	return "";
        }
        return value.toString();
    }
    
    public static String obj2Json(Object obj) {
    	ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {  
			return e.getMessage();
  	    }  
    }
    
    @SuppressWarnings("unchecked")
	public static Map<String, String> json2Map(String json) throws Exception {
    	return new ObjectMapper().readValue(json, Map.class);
    }
    
    @SuppressWarnings("unchecked")
	public static Map<String, Object> json2Map2(String json) throws Exception {
    	return new ObjectMapper().readValue(json, Map.class);
    }
    
    @SuppressWarnings("unchecked")
	public static List<Map<String, Object>> json2List(String json) throws Exception {
    	return new ObjectMapper().readValue(json, List.class);
    }
    
    public static boolean isNullOrEmpty(String str) {
        return str == null || "".equals(str.trim()) || "null".equalsIgnoreCase(str.trim()) || "undefined".equalsIgnoreCase(str.trim()) || "NaN".equals(str.trim());
    }
    
    public static boolean isNullOrEmpty(Object obj) {
        return obj == null || isNullOrEmpty(obj.toString());
    }
    
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    public static Object checkNull(Object...objs) {
    	for(Object obj: objs) {
    		if( !isNullOrEmpty(obj) ) {
    			return obj;
    		}
    	}
    	return null;
    }
    
    public static Object checkNullI(Object...objs) {
    	Object ret = null;
    	for(Object obj: objs) {
    		if( !isNullOrEmpty(obj) ) {
    			return obj;
    		}
    		ret = obj;
    	}
    	return ret;
    }
    
    public static Object checkTrue(boolean condition, Object result1, Object result2) {
    	return condition ? result1 : result2;
    }
    
    public static boolean contains(List<?> list, String items) {
    	if(EasyUtils.isNullOrEmpty(items)) return true;
    	
		String[] _arr = items.split(",");
		for (String item : _arr) {
			item = item.trim();
			if( item.length() == 0 ) continue;
			
			for( Object obj : list ) {
				if( obj.toString().equals(item) ) {
					return true;
				}
			}
		}
		return false;
	}
    
    public static List<String> toList(String s) {
    	return toList(s, ",");
    }
    public static List<String> toList(String s, String seperator) {
    	ArrayList<String> list = new ArrayList<String>();
    	if( isNullOrEmpty(s) ) {
			return list;
		}
    	
    	String[] vals = (" " +s+ " ").split(seperator);
    	for(String val : vals) {
    		list.add( val.trim() );
    	}
    	return list;
    }

    /**
     * 将list转换以”,“号隔开的一组字符串。
     * 通常用于转换id列表。
     * 
     * @param list
     * @return
     */
    public static String list2Str(Collection<?> list){
        return list2Str(list, ",");
    }
    
    public static String list2Str(Collection<?> list, String seperator){
        if( isNullOrEmpty(list) ) return "";
        
        // 先复制一份， 防止ConcurrentModificationException
        list = new ArrayList<>(list);
        
        StringBuffer sb = new StringBuffer();
        int index = 0;
        for(Object obj : list) {
        	if(obj == null) continue; // 剔除掉null
        	
            if(index++ > 0) {
                sb.append(seperator);
            }
            sb.append( obj );
        }
        return sb.toString();
    }
    
    /**
     * 将数组中的 指定列拼成字符串
     */
    public static String list2Str(Collection<Object[]> list, int index){
    	List<Object> temp = new ArrayList<Object>();
    	for(Object[] objs : list) {
    		temp.add( objs[index] );
    	}
        return list2Str(temp, ",");
    }
    
    public static String attr2Str(Collection<Map<String, Object>> list, String key){
        return list2Str( attr2List(list, key), ",");
    }
    
    public static List<Object> attr2List(Collection<Map<String, Object>> list, String key){
    	List<Object> temp = new ArrayList<Object>();
    	for(Map<String, Object> m : list) {
    		temp.add( m.get(key) );
    	}
        return temp;
    }
    
    @SuppressWarnings("unchecked")
	public static Map<?, Map<?, Object>> list2Map(String key, Collection<?>..._list) {
    	Map<Object, Map<?, Object>> map = new HashMap<>();
    	
    	for(Collection<?> list : _list) {
    		for(Object o : list) {
    			Map<?, Object> m = (Map<?, Object>) o;
    			map.put( m.get(key), m );
        	}
    	}
    	
        return map;
    }

    public static List<Object> objAttr2List(Collection<?> list, String key) {
    	List<Object> result = new ArrayList<>();
    	for (Object o : list) {
    		result.add(BeanUtil.getPropertyValue(o, key));
		}
		return result;
    }
    
    public static String objAttr2Str(Collection<?> list, String key) {
    	return  EasyUtils.list2Str(EasyUtils.objAttr2List(list, key));
    }
    
    /**
     * 生成下拉所需要的value和 text
     * 用法如： EasyUtils.list2Combo(list, "year", "name", "|")
     * 
     * @param list
     * @param valueName 实体value属性名称
     * @param textName  实体text 属性名称
     * @param seperator  分隔符 "|"/","等
     * @return
     */
    public static String[] list2Combo(Collection<?> list, String valueName, String textName, String seperator){
        StringBuffer value = new StringBuffer();
        StringBuffer text = new StringBuffer();
        for(Object bean : list){
            if(value.length() > 0){
                value.append(seperator);
                text.append(seperator);
            }
            value.append(BeanUtil.getPropertyValue(bean, valueName));
            text.append (BeanUtil.getPropertyValue(bean, textName));
        }
        return new String[]{value.toString(), text.toString()};
    }
    
    public static Map<String, String> sortMapByKey(Map<String, String> map) {
        Map<String, String> sortMap = new TreeMap<String, String>(new Comparator<String>(){
        	public int compare(String str1, String str2) {
                return str1.compareTo(str2);
            }
        });

        sortMap.putAll(map);
        return sortMap;
    }

    /**
     * 将字符串按其中子字符串分解成字符串数组。
     * @param s
     * @param s1
     * @return
     */
    public static String[] split(String s, String s1) {
        if(s == null) return null;
        
        StringTokenizer stringtokenizer = new StringTokenizer(s, s1);
        int i = stringtokenizer.countTokens();
        String as[] = new String[i];
        for(int j = 0; j < i; j++) {
            as[j] = stringtokenizer.nextToken();
        }

        return as;
    }
    
    /**
     * 对重复表头进行处理，自动加2
     */
    public static void fixRepeat(List<String> l) {
    	if(l == null) return;
    	
    	// 先去除空格
    	int index = 0;
    	for(String s : l) {
    		l.set(index++, s.trim());
    	}
    	
    	Map<String, Integer> m = new HashMap<>();
		index = 0;
		for(String s : l) {
			Integer count = m.get(s);
			if( count == null ) {
				count = 1;
			} else {
				count ++;
				String s2 = s + "_" + count;
				l.set(index, s2);
				m.put(s2, 1);
			}
			m.put(s, count);
			index++;
		}
    }
    
    /**
     * 过滤掉逗号分隔的字符串里空的元素（eg: 12,,,33,44,,66 --> 12,33,44,66） 并去重
     */
    public static String filterEmptyItem(String s) {
    	Set<String> list = new LinkedHashSet<String>();
    	String[] arr = s.split(",");
    	for(String item : arr) {
    		if( !isNullOrEmpty(item) ) {
    			list.add( item.trim() );
    		}
    	}
    	
    	return list2Str(list);
    }
    
    public static boolean fmParseError(String s) {
    	return s != null && s.startsWith(FM_PARSE_ERROR);
    }
    
    static String FM_PARSE_ERROR = "FM-parse-error:";
    
    /**
     * log4j里加 log4j.logger.freemarker=fatal 过滤掉FM自身的error Log
     */
    public static String fmParse( String str, Map<String, ?> data, Writer out, Configuration cfg ) {
    	try {
    		cfg.setNumberFormat("#"); // 防止数字类格式化，eg: 1000 变 1,000
    		cfg.setDefaultEncoding("UTF-8");
    		
	        Template tl = new Template("t.ftl", new StringReader(str), cfg);
	        tl.process(data, out);
	        str = out.toString();
	        out.flush();
	        
	        return str;
        } 
        catch (Exception e) {
			String errorMsg = FM_PARSE_ERROR + e.getMessage();
	    	log.error(errorMsg + ", template = " + str.substring(0, Math.min(800, str.length())) + ", data = " + data);
	    	
	    	return errorMsg;
	    }
    }
    
    /**
		Map<String, ?> data = _Util.json2Map("{\"64G\":4}");
		EasyUtils.fmParse("${64G}", data); ===> 报错，key需要非数字开头
     */
    public static String fmParse( String template, Map<String, ?> data ) {
    	Writer out = new StringWriter();
    	return fmParse(template, data, out, new Configuration(Configuration.VERSION_2_3_28));
    }
    
    
    /**
	 * 调用freeMarker解析后，用js的eval方法计算结果
	 * 
	 * @param script
	 * @param params
	 * @return
	 */
	public static Double eval(String script, Map<String, ?> params) {
		if (script.indexOf("${") > -1 || script.indexOf("<#if") > -1) {
			script = fmParse(script, params);
		}
		if (script.indexOf("${") > -1 || script.indexOf("<#if") > -1) {
			script = fmParse(script, params);
		}
		
		Object value = 0;
		try {
			value = new ScriptEngineManager().getEngineByName("JavaScript").eval(script);
		} catch (Exception e) {
			throw new BusinessException( script + "," + params + " eval error: " + e.getMessage(), e);
		}
		return obj2Double(value);
	}

	/**
	 * 是否是服务器环境, 约定：本地或dev环境envirment必须是带local或dev字样的（test需要当prod对待，保证测试覆盖）
	 * 
	 * @return
	 */
    public static boolean isProd() {
    	String envirment = ParamConfig.getAttribute(PX.ENVIRONMENT);
    	return envirment != null && envirment.indexOf("local") == -1 && envirment.indexOf("dev") == -1;
    }
}