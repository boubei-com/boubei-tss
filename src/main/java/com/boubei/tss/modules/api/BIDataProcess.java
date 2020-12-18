package com.boubei.tss.modules.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.RecordField;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.cloud.entity.DomainConfig;
import com.boubei.tss.util.EasyUtils;

public class BIDataProcess {
	protected Logger log = Logger.getLogger(this.getClass());

	public void handle(SQLExcutor ex, Map<String, String> requestMap, String tag) {
		if ("query".equals(tag)) {
			handleQuery(ex, requestMap);
		} else {
			handleExport(ex, requestMap);
		}
	}

	protected void handleQuery(SQLExcutor ex, Map<String, String> requestMap) {

	}

	protected void handleExport(SQLExcutor ex, Map<String, String> requestMap) {

	}

	public String beforeHandleSql(String sql, Map<String, String> requestMap) {
		/* 1、如果传过来的selectFields是数字，则从表中取出真正的selectFields替换 */
		sql = handleSelectFields(sql, requestMap);

		/* 2、如果sql中包含特定字符 如 <#if tms_order_s_udf??> 代表这个参数需要用tms_order表的udf集代替 且自动过滤权限 
		 * 	  eg：s.udf1 自定义1, s.udf3 自定义3
		 */
		addUdf(sql, requestMap);

		return sql;
	}

	/*
	 * selectFields = name 名称,code 编码 强制修改requestMap
	 */
	public String handleSql(String sql, Map<String, String> requestMap) {
		String selectFields = requestMap.get("selectFields");
		if (EasyUtils.isNullOrEmpty(selectFields)) {
			return sql;
		}

		String[] selectFields_ = selectFields.split("( |,)+");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < selectFields_.length; i++) {
			String s = selectFields_[i];
			if (i % 2 == 0) {
				sb.append("s." + s);
			} else {
				sb.append(" " + s + ",");
			}
		}

		sql = "select " + sb.substring(0, sb.length() - 1) + " from (" + sql + ") s";
		return sql;
	}

	protected String handleSelectFields(String sql, Map<String, String> requestMap) {
		String selectFields = requestMap.get("selectFields");
		if ( !EasyUtils.isDigit(selectFields) )
			return sql;

		Long dcID = EasyUtils.obj2Long(selectFields);
		DomainConfig domainConfig = (DomainConfig) Global.getCommonService().getEntity(DomainConfig.class, dcID);
		if (domainConfig == null) {
			requestMap.remove("selectFields");
			return sql;
		}

		String _list = domainConfig.getContent();
		try {
			List<Map<String, Object>> list = EasyUtils.json2List(_list);

			StringBuffer sb = new StringBuffer();
			for (Map<String, Object> map : list) {
				String field = (String) map.get("field");
				String title = (String) map.get("title");
				String export = (String) map.get("export");
				if ("ck".equals(field) || !"√".equals(export))
					continue;
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(field + " " + title);
			}

			requestMap.put("selectFields", sb.toString());
		} catch (Exception e) {
			throw new BusinessException(e.getMessage());
		}

		return sql;
	}

	protected static void addUdf(String sql, Map<String, String> requestMap) {
		String pattern = "<#if ([a-z]|_)*_udf\\?\\?>";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(sql);

		List<String> list = new ArrayList<>();
		if (m.find()) {
			list.add(m.group());
		}

		for (String matched : list) {
			String key = matched.replace("<#if ", "").replace("??>", "");
			List<String> matchedList = new ArrayList<>(Arrays.asList(key.split("_")));
			matchedList.remove(matchedList.size() - 1);
			String preCode = matchedList.remove(matchedList.size() - 1);
			String table = EasyUtils.list2Str(matchedList, "_");

			// log.debug(key + " " + preCode + "  " + table);
			requestMap.put(key, getUdfDefine(Environment.getDomain(), table, Environment.getOwnRoles(), preCode));
		}
	}
	
	public static List<RecordField> getRecordFields(String domain, String table, List<Long> roles) {
		String hql = " from RecordField where tbl = ?1 and domain = ?2 order by code";
		@SuppressWarnings("unchecked")
		List<RecordField> recordFields = (List<RecordField>) Global.getCommonService().getList(hql, table, domain);
		for (RecordField recordField : recordFields) {
			if ( !"hidden".equals(recordField.getType()) ) {
				if ( recordField.containsRole2(roles) ) {
					recordField.role = "编辑";
				} else if ( recordField.containsRole1(roles) ) {
					recordField.role = "查看";
				}
			}  
		}
		return recordFields;
	}

	protected static String getUdfDefine(String domain, String table, List<Long> roles, String preCode) {
		Pool pool = CacheHelper.getShortCache();
		String ckey = "BiDataProcessUdf" + domain + table + roles + preCode;
		Cacheable item = pool.getObject(ckey);
		if (item == null) {
			List<RecordField> list = getRecordFields(domain, table, roles);
			String sql = "";
			for (RecordField recordField : list) {
				if (recordField.role != null) {
					sql += ", " + preCode + "." + recordField.getCode() + " " + recordField.getLabel();
				}
			}
			item = pool.putObject(ckey, sql);
		}

		return (String) item.getValue();
	}

}
