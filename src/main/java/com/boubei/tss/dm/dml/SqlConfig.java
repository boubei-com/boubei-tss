/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.dml;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.URLUtil;
import com.boubei.tss.util.XMLDocUtil;

public class SqlConfig {

	static final Logger log = Logger.getLogger(SqlConfig.class);
	
	public static final String SQL_CONFIG_INIT = "SQL_CONFIG_INIT";

	/**
	 * 不能用缓存池来缓存，有隐患，当缓存池满了以后，单条SQL缓存可能会被清除掉，get该SQL会取不到。
	 */
	static Map<String, Script> cache = new HashMap<>();

	static Map<String, Object> sqlNestFmParams = new HashMap<String, Object>(); // SQL嵌套解析用

	public static String getScript(String sqlCode) {
		return getScript(sqlCode, "script");
	}
	
	public static String getScript(String sqlCode, String sqlPath) {
		return _getScript(sqlCode, sqlPath).sql;
	}

	public static Script _getScript(String sqlCode, String sqlPath) {
		sqlPath = (String) EasyUtils.checkNull(sqlPath, "script");
		String cacheKey = SQL_CONFIG_INIT + "_" + sqlPath.toUpperCase();

		Pool pool = CacheHelper.getNoDeadCache(); // 清空NoDeadCache，可触发所有Script重新加载
		if (!pool.contains(cacheKey)) {
			File scriptDir = new File(URLUtil.getResourceFileUrl(sqlPath).getPath());
			List<File> sqlFiles = FileHelper.listFilesByTypeDeeply("xml", scriptDir);

			for (File sqlFile : sqlFiles) {
				Document doc = XMLDocUtil.createDocByAbsolutePath(sqlFile.getPath());
				Element root = doc.getRootElement();
				String rootRole = root.attributeValue("role");

				List<Element> sqlNodes = XMLDocUtil.selectNodes(doc, "//sql");
				for (Element sqlNode : sqlNodes) {
					Script obj = new Script();
					obj.code = sqlNode.attributeValue("code").trim();
					obj.role = sqlNode.attributeValue("role");
					obj.ds   = sqlNode.attributeValue("datasource");
					obj.dataProcess = sqlNode.attributeValue("bIDataProcess");
					obj.defaultFilter = sqlNode.attributeValue("defaultFilter");
					obj.recordLog = "on".equalsIgnoreCase( sqlNode.attributeValue("log") ); // 默认不记日志，log="on"才记
					obj.sql = sqlNode.getText().trim();
				
					obj.name = (String) EasyUtils.checkNull(sqlNode.attributeValue("name"), obj.code);
					obj.role = EasyUtils.obj2String(rootRole) + "," + EasyUtils.obj2String(obj.role);

					sqlNestFmParams.put("${" + obj.code + "}", obj.sql);
					cache.put(obj.code, obj);
				}
			}
			pool.putObject(cacheKey, true);
		}

		Script script = cache.get(sqlCode);
		if (script == null) {
			throw new BusinessException("没有找到编码为【" + sqlCode + "】的SQL");
		}

		// 根据当前登录人的角色判断其是否有权限访问sqlCode对应的SQL
		List<String> ownRoles = Environment.getOwnRoleNames();
		String[] permitRoles = EasyUtils.obj2String( script.role ).split(",");

		boolean flag = true;
		for (String permitRole : permitRoles) {
			if (EasyUtils.isNullOrEmpty(permitRole))
				continue;

			flag = false;
			if (ownRoles.contains(permitRole)) {
				flag = true;
				break;
			}
		}
		if (!flag) {
			throw new BusinessException("你对数据服务【" + sqlCode + "】没有访问权限");
		}

		script.sql = MacrocodeCompiler.run(script.sql, sqlNestFmParams, true); // 自动解析script里的宏嵌套

		return script;
	}
	
	public static class Script {
		public String code;
		public String sql;
		public String role;
		public String name;
		public String ds;
		public String dataProcess;
		public String defaultFilter;
		public boolean recordLog;
		
		public String toString() {
			return sql;
		}
	}
}
