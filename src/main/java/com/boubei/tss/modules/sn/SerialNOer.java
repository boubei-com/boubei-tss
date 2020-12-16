package com.boubei.tss.modules.sn;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.EasyUtils;

/**
 *	取号器，支持指定前缀及一次获取多个连号
 *	配合数据表字段使用，在字段定义默认值填入：XX-yyMMddxxxx，则在界面新增或导入数据时自动调用本取号器
 	
 	$.getJSON("/tss/sn/O/1", {}, function(sns) { console.log(sns[0]); });
 	
 */
@Controller
@RequestMapping("/sn")
public class SerialNOer {

	@RequestMapping(value = "/{snTL}/{count}")
	@ResponseBody
	public List<String> create(@PathVariable("snTL") String snTL, @PathVariable("count") int count, Integer size) {
		int length = EasyUtils.obj2Int(size);
		return getSNCreator(snTL, false, length).create(count);
	}
	
	public static List<String> create(String sntemplate, int count) {
		return getSNCreator(sntemplate, false, 0).create(count);
	}

	public static String createOne(String sntemplate) {
		return create(sntemplate, 1).get(0);
	}
	
	public static List<String> create(String domain, String sntemplate, int count, int length) {
		return getSNCreator(domain, sntemplate, length).create(count);
	}
	
	// 无限定前缀
	public static String get() {
		return get( _Field.SNO_yyMMddxxxx );
	}
	public static String get(String preCode) {
		return get(preCode, false);
	}
	public static String get(String preCode, boolean isGlobal) {
		return getSNCreator(preCode, isGlobal, 0).create();  // preCode eg: Dxxxx
	}
	
	
	/**
	 * 生成固定位数(size)的编码
	 * 
	 * 测试取号器锁机制是否有效：
	 * var a = [], i = 0; 
	 * while(i++ < 1000) {  $.get('/tss/fix/sn/Sxxxx/1', {size:5, isGlobal:true}, function(r) { !a.contains(r[0]) && a.push(r[0]);  }); }
	 * 
	 */
	@RequestMapping(value = "/fix/{precode}/{count}")
	@ResponseBody
	public  List<String> getFixSN( @PathVariable("precode") String preCode, @PathVariable("count") int count, 
			int size, boolean isGlobal ) {
		return getFixSN(size, preCode, count, isGlobal);
	}
	
	// 生成固定位数的编码（单个）
	public static String getFixSN(int length, String preCode, boolean isGlobal) {
		return getFixSN(length, preCode, 1, isGlobal).get(0);
	}
		
	// 生成固定位数的编码（批量）
	public static List<String> getFixSN(int length, String preCode, int count, boolean isGlobal) {
		preCode = EasyUtils.checkNull(preCode, "1").toString();
		if( !preCode.endsWith(_Field.SNO_xxxx) ) {
			preCode += _Field.SNO_xxxx;
		}
		return getSNCreator(preCode, isGlobal, length).create(count);
	}
	
	// 对取号器进行缓存，可用作对象锁
	static SNCreator getSNCreator(String snTemplate, boolean isGlobal, int length) {
		String domain = isGlobal ? UMConstants.DEFAULT_DOMAIN : Environment.getDomain();
		return getSNCreator(domain, snTemplate, length);
	}
	synchronized static SNCreator getSNCreator(String domain, String snTemplate, int length) {
		domain = (String) EasyUtils.checkNull(domain, Environment.getDomain());
		
		Pool cache = CacheHelper.getLongCache();
		String key = domain + "-" + snTemplate + "_" + length;
		Cacheable item = cache.getObject(key);
		if( item == null ) {
			item = cache.putObject( key, new SNCreator(domain, snTemplate, length) );
			
			// 顺带清理掉：15分钟内只用过一次的取号器
			try {
				CacheHelper.flushCache(cache, "xxxx", 2, System.currentTimeMillis(), 15);
			} catch(Exception e) { }
		}
		
		return (SNCreator) item.getValue();
	}
}
