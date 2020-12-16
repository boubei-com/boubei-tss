/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.ddl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.boubei.tss.EX;
import com.boubei.tss.PX;
import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.JCache;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordEvent;
import com.boubei.tss.dm.record.RecordEventN;
import com.boubei.tss.dm.record.permission.RecordPermission;
import com.boubei.tss.dm.record.permission.RecordResource;
import com.boubei.tss.dm.record.workflow.WFStatus;
import com.boubei.tss.dm.record.workflow.WFUtil;
import com.boubei.tss.dm.report.log.AccessLogRecorder;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.grid.GridTemplet;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.permission.PermissionHelper;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.XMLDocUtil;

public abstract class _Database {
	
	static Logger log = Logger.getLogger(_Database.class);
	
	public final static String _CACHE_KEY(Long recordId) {
		return "_db_record_" + recordId + "_" + Environment.getDomain() + "_" + Environment.getUserCode();
	}
	
	public Long recordId;
	public String recordName;
	public String datasource;
	public String table;
	public String customizeTJ;  // 1=1<#if 1=0>showCUV & ignoreDomain</#if>
	public String wfDefine;
	
	public boolean isWorkflow;
	public boolean logicDel;
	public boolean needFile;
	
	public  boolean needLog;
	private boolean needQLog;
	public  boolean showCreator;
    private boolean ignoreDomain;
    public  boolean makePublic;
	
    protected RecordEvent rcEvent;
	public String remark;
	
	List<Map<String, Object>> fields;
	Map<String, Map<String, Object>> fieldMap = new HashMap<>();
	
	public List<String> fieldCodes;
	public List<String> fieldTypes;
	public List<String> fieldPatterns;
	public List<String> fieldNames;
	public List<String> fieldAligns;
	public List<String> fieldWidths;
	public List<String> fieldRole2s;
	public List<String> fieldValues;
	
	public Map<String, String> cnm = new HashMap<String, String>();      // code -- label
	public Map<String, String> ncm = new HashMap<String, String>();      // label -- code
	public Map<String, String> ctype = new HashMap<String, String>();    // code -- type
	public Map<String, String> crole = new HashMap<String, String>();    // code -- role2
	public Map<String, String> cpattern = new HashMap<String, String>(); // code -- pattern
	public Map<String, String> cval = new HashMap<String, String>();     // code -- defaultValue
	public Map<String, String> cerr = new HashMap<String, String>();     // code -- errorMsg
	public Map<String, String> cuni = new HashMap<String, String>();     // code -- unique
	public Map<String, String> cnull = new HashMap<String, String>();    // code -- nullable
	public Map<String, String> creg = new HashMap<String, String>();     // code -- checkReg
	public Map<String, String> csql = new HashMap<String, String>();     // code -- valSQL
	public Map<String, String> cmatch = new HashMap<String, String>();   // code -- Match partter 严格匹配，不允许模糊查询
	public Map<String, String> csort = new HashMap<String, String>();    // code -- sort asc | desc
	public Map<String, String> coptions = new HashMap<String, String>(); // code -- option 下拉列表（生成Excel导出模板有用）
	
	
	public String toString() {
		return "【" + this.datasource + "." + this.recordName + "】";
	}
 	
	public _Database(Record record) {
		if(record == null) return;
		
		this.recordId = record.getId();
		this.recordName = record.getName();
		this.datasource = record.getDatasource();
		this.table = record.getTable();
		this.fields = parseJson(record.getDefine());
		this.customizeTJ = record.getCustomizeTJ();
		this.needLog  = ParamConstants.TRUE.equals(record.getNeedLog());
		this.needQLog = ParamConstants.TRUE.equals(record.getNeedQLog());
		this.needFile = ParamConstants.TRUE.equals(record.getNeedFile());
		this.logicDel = ParamConstants.TRUE.equals(record.getLogicDel());
		this.showCreator = ParamConstants.TRUE.equals(record.getShowCreator());
		this.ignoreDomain = ParamConstants.TRUE.equals(record.getIgnoreDomain());
		this.makePublic = ParamConstants.TRUE.equals(record.getMakePublic());
		this.remark = record.getRemark();
		this.rcEvent = getRcEvent();
		
		this.initFieldCodes();
		this.wfDefine = record.getWorkflow();
		this.isWorkflow = WFUtil.checkWorkFlow(this.wfDefine);
	}
	
	protected void initFieldCodes() {
		this.fieldCodes = new ArrayList<String>();
		this.fieldTypes = new ArrayList<String>();
		this.fieldPatterns = new ArrayList<String>();
		this.fieldNames = new ArrayList<String>();
		this.fieldAligns = new ArrayList<String>();
		this.fieldWidths = new ArrayList<String>();
		this.fieldRole2s = new ArrayList<String>();
		this.fieldValues = new ArrayList<String>();
		
		ncm.put("创建人", "creator");
		ncm.put("创建时间", "createtime");
		cnm.put("creator", "创建人");
		cnm.put("createtime", "创建时间");
		
		for(Map<String, Object> fDefs : this.fields) {
			String code = (String) fDefs.get("code");
			this.fieldCodes.add(code);
			fieldMap.put(code, fDefs);
			
			String label = (String) fDefs.get("label");
			this.fieldNames.add(label);
			cnm.put(code, label);
			ncm.put(label, code);
			
			String type = (String) fDefs.get("type");
			this.fieldTypes.add(type);
			ctype.put(code, type);
			
			String pattern = (String) fDefs.get("pattern");
			if( _Field.TYPE_NUMBER.equalsIgnoreCase(type) ) {
				pattern = (String) EasyUtils.checkNull(pattern, "##,##0.00");  //  类GridNode会对数据进行格式化，默认3位小数
            }
			this.fieldPatterns.add( pattern ); 
			cpattern.put(code, pattern);
			
			String defaultVal = (String) fDefs.get("defaultValue");
			this.fieldValues.add(defaultVal);
			cval.put(code, defaultVal);
			
			cerr.put(code, (String) fDefs.get("errorMsg"));
			cnull.put(code, (String) fDefs.get("nullable"));
			cuni.put(code, (String) EasyUtils.checkNull(fDefs.get("unique_"), fDefs.get("unique")));
			creg.put(code, (String) fDefs.get("checkReg"));
			csql.put(code, (String) fDefs.get("valSQL"));
			cmatch.put(code, (String) fDefs.get("match"));
			
			Map<?, ?> options = (Map<?, ?>) fDefs.get("options");
			if( options != null ) {
				coptions.put(code, (String) EasyUtils.checkNull(options.get("codes"), options.get("names")));
			}
			
			Object sort = fDefs.get("sort");
			if( !EasyUtils.isNullOrEmpty(sort) ) {
				csort.put(code, code + " " + sort);
			}
			
			String role2 = (String) fDefs.get("role2");
			this.fieldRole2s.add(role2);
			crole.put(code, role2);
			
			this.fieldAligns.add( EasyUtils.obj2String(fDefs.get("calign")) ); // 列对齐方式
			this.fieldWidths.add( EasyUtils.obj2String(fDefs.get("cwidth")) ); // 列宽度
		}
	}
	
	/**
	 * 获取自定义字段，多层级自定义查询过滤，优先级 user > org > domain > domain is null
	 * 	1、tb不为空，ignoreTblisNull = true， 只取指定表的自定义字段
	 * 	2、tb不为空，ignoreTblisNull = false， 取指定表 及 公共 的自定义字段
	 * 	3、tbl为空 且 ignoreTblisNull = false， 前台调用，取共同部分（tbl is null）的字段自定义，eg：批次属性
	 */
	public static Map<?, Map<?, Object>> queryRecordFields(String tbl, Long userOrg, boolean ignoreTblisNull) {
		String domain = Environment.getDomain();
		String userCode = Environment.getUserCode();
		String sql = "select code,label, IFNULL(type, 'string') type, nullable,unique_,readonly,role1,role2,cwidth,align,options,defaultValue,checkReg from dm_record_field ";
		
		List<Map<String, Object>> _fields4 = SQLExcutor.queryL(sql + " where ? in (tbl,udf1) and domain = ? and user is null and user_org is null", tbl, domain);
		List<Map<String, Object>> _fields5 = SQLExcutor.queryL(sql + " where ? in (tbl,udf1) and domain = ? and user is null and user_org = ?", tbl, domain, userOrg);
		List<Map<String, Object>> _fields6 = SQLExcutor.queryL(sql + " where ? in (tbl,udf1) and domain = ? and user = ?", tbl, domain, userCode);
		
		if( ignoreTblisNull ) { // 只取指定表的自定义字段
			return EasyUtils.list2Map("code", _fields4, _fields5, _fields6);
		}
		else { // tbl is null，字段作用于多个表（eg：批次属性）
			List<Map<String, Object>> _fields1 = SQLExcutor.queryL(sql + " where tbl is null and domain = ? and user is null and user_org is null", domain);
			List<Map<String, Object>> _fields2 = SQLExcutor.queryL(sql + " where tbl is null and domain = ? and user is null and user_org = ?", domain, userOrg);
			List<Map<String, Object>> _fields3 = SQLExcutor.queryL(sql + " where tbl is null and domain = ? and user = ?", domain, userCode);
			
			return EasyUtils.list2Map("code", _fields1, _fields2, _fields3, _fields4, _fields5, _fields6);
		}
	}
	
	protected List<Map<String, Object>> parseJson(String define) {
		if(EasyUtils.isNullOrEmpty(define)) {
			return new ArrayList<Map<String, Object>>();
		}
		
		// 获取自定义字段
		Map<?, Map<?, Object>> map = queryRecordFields(this.table, Environment.getUserOrg(), false);
		
		define = define.replaceAll("'", "\"");
		List<String> remainCodes = Arrays.asList("creator", "createTime", "updator", "updateTime", "version", "id");  // domain
		List<String> labels = new ArrayList<String>();
		List<String> codes = new ArrayList<String>();
		try {  
   			List<Map<String, Object>> list = EasyUtils.json2List(define);  
   			Pattern pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$"); // code：字母、数字、下划线
   			for(int i = 0; i < list.size(); i++) {
   	        	Map<String, Object> fDefs = list.get(i);
   	        	int index = i + 1;
   	        	
   				String code = (String) fDefs.get("code");
   				boolean codeVaild = !EasyUtils.isNullOrEmpty(code) && pattern.matcher(code).matches();
   				code = (codeVaild ? code : _Field.COLUMN + index).toLowerCase().trim();
   				fDefs.put("code", code);
   				
   				// 覆盖自定义字段）
   				if( map.containsKey(code) )	{
   					Map<?, Object> attrs = map.get(code);
   					for(Object key : attrs.keySet()) {
   						Object val = attrs.get(key);
						if( val == null ) continue;
						
						if( "options".equals(key) ) { // 空格分隔的选项列表
							Map<String, String> mVal = new HashMap<>();
							mVal.put("codes", val.toString().replaceAll(" ", "|"));
							val = mVal;
						}
						key = key.toString().replaceFirst("defaultvalue", "defaultValue").replaceFirst("checkreg", "checkReg");
						fDefs.put(key.toString(), val);
   					}
   				}
   				
   				String label = (String) fDefs.get("label");
   				if( labels.contains(label) ) {
   					throw new BusinessException( EX.parse(EX.DM_26, label) );
   				}
   				if( remainCodes.contains(code) ) {
   					throw new BusinessException( EX.parse(EX.DM_34, code) );
   				}
   				if( codes.contains(code) ) {
   					throw new BusinessException( EX.parse(EX.DM_25, code) );
   				}
   				
				labels.add( label );
   				codes.add( code );
   			}
   			return list;
   	    } 
   		catch (Exception e) {  
			throw new BusinessException( EX.parse(EX.DM_15, recordName, e.getMessage()), true );
   	    } 
	}

	protected Map<String, String> getDBFiledTypes(int length) {
		Map<String, String> m = new HashMap<String, String>();
		m.put(_Field.TYPE_NUMBER, "float");
		m.put(_Field.TYPE_INT, "int");
		m.put(_Field.TYPE_DATETIME, "datetime");
		m.put(_Field.TYPE_DATE, "date");
		m.put(_Field.TYPE_STRING, "varchar(" + length + ")");
		m.put(_Field.TYPE_FILE, "varchar(255)");
		
		return m;
	}
	
	protected String getFiledDef(Map<String, Object> fDef, boolean ignoreNullable) {
		
		Map<String, String> dbFieldTypes = getDBFiledTypes( _Field.getVarcharLength(fDef) );
		dbFieldTypes.put(_Field.TYPE_HIDDEN, "varchar(255)");
		
		String def = "" + fDef.get("code") + " ";
		String tssFieldType = (String) fDef.get("type"); // string/number/int/date/datetime/hidden
		tssFieldType = (String) EasyUtils.checkNull(tssFieldType, _Field.TYPE_STRING);
		
		String fieldType = dbFieldTypes.get( tssFieldType );
		def += " " +fieldType+ " "; 
		
		if("false".equals(fDef.get("nullable")) && !ignoreNullable) {
			def += " NOT NULL "; 
		}
		
		return def; // eg: f1 varchar(100) not null
	}
	
	protected String[] getSQLs(String field) {
		String[] names = createNames(field);
		String sql0 = "alter table " +this.table+ " add constraint " +names[0]+ " UNIQUE (" +field+ ", domain)";
		String sql1 = "alter table " +this.table+ " drop constraint " +names[0];
		String sql2 = "create index " +names[1]+ " on " +this.table+ " (" +field+ ")";
		String sql3 = "drop index " +names[1];
		String sql4 = "alter table " +this.table+ " modify " + field;
		return new String[] { sql0, sql1, sql2, sql3, sql4 };
	}
	
	// 防止索引长度过长
	public String[] createNames(String field) {
		String name = this.table + "_" +field;
		if(name.length() > 20) {
			name = name.substring(name.length() - 20);
		}
		return new String[] { "uni_"+name, "idx_"+name };
	}
	
	public abstract String toPageQuery(String sql, int page, int pagesize);
	
	/**
	 * 如果相同表名已经存在，直接使用既有表
	 */
	public abstract void createTable();
	
	/**
	 * 如果表已经存在，重复创建索引将会报错，此处直接try catch掉
	 */
	public void createUniqueAndIndex() {
   		for(Map<String, Object> fDefs : fields) {
   			String code = (String) fDefs.get("code");
   			String unique = (String) fDefs.get("unique");
   			String isparam = (String) fDefs.get("isparam");
   			
   			String[] ddlSQLs = getSQLs(code);
			if( "true".equals(unique) ) {
				try {
					SQLExcutor.excute(ddlSQLs[0], datasource);
				} catch(Exception e) { }
			}
			if( "true".equals(isparam) ) {
				try {
					SQLExcutor.excute(ddlSQLs[2], datasource);
				} catch(Exception e) { }
			}
   		}	
   		
   		// 为domain、creator、createTime字段分别创建索引
   		try { 
   			SQLExcutor.excute( getSQLs("domain") [2], datasource);
   			SQLExcutor.excute( getSQLs("creator") [2], datasource);
   			SQLExcutor.excute( getSQLs("createtime") [2], datasource);
   		} 
   		catch(Exception e) { }
   		
   		// 总是在新建表之后执行
   		CacheHelper.getNoDeadCache().destroyByKey(DMConstants.RECORD_TABLE_LIST);
	}
	
	public void dropTable(String table, String datasource) {
		SQLExcutor.excute("drop table " + table, datasource);
	}
	
	/**
	 * 修改表结构注意事项：
	 * 1、字段Code修改后，会新增一列出来，而不是直接修改原有的列名。旧的列如果有数据会保留，没有数据则被删除。如需把旧列的数据显示在新列里，需要自行在DB端操作。
	 * 2、Oracle表中存在数据时，无法修改字段类型
	 */
	public void alterTable(Record _new) {
		int existedCount = 0;
		try {
			this.customizeTJ = null;
			existedCount = this.select(1, 1, null).count;
		} 
		catch(Exception e) { }
		
		String newDS = _new.getDatasource();
		String table = _new.getTable();
		this.customizeTJ = _new.getCustomizeTJ();
		this.needLog  = ParamConstants.TRUE.equals(_new.getNeedLog());
		this.needFile = ParamConstants.TRUE.equals(_new.getNeedFile());
		this.needQLog = ParamConstants.TRUE.equals(_new.getNeedQLog());
		this.logicDel = ParamConstants.TRUE.equals(_new.getLogicDel());
		this.showCreator = ParamConstants.TRUE.equals(_new.getShowCreator());
		this.ignoreDomain = ParamConstants.TRUE.equals(_new.getIgnoreDomain());
		this.makePublic = ParamConstants.TRUE.equals(_new.getMakePublic());
		
		this.wfDefine = _new.getWorkflow(); // 更新流程配置
		
		// 比较新旧字段定义的异同（新增的和删除的，暂时只关心新增的）
		List<Map<String, Object>> newFields = parseJson(_new.getDefine());
				
		if(!newDS.equals(this.datasource) || !table.equals(this.table)) {
			this.datasource = newDS;
			this.table = table;
			
			this.fields = newFields;
			initFieldCodes();
			
			createTable();
			createUniqueAndIndex();
			return;
		}
		
		// 新增加的字段
		for(Map<String, Object> fDefs1 : newFields) { // new
			String code = (String) fDefs1.get("code");
			fDefs1.put("code", code);
			
			boolean exsited = false;
        	for(Map<String, Object> fDefs0 : this.fields ) { // old
        		if(code.equals(fDefs0.get("code"))) {
        			exsited = true; 
        			
        			String[] ddlSQLs = getSQLs(code);
        			
        			// 进一步判断字段类型及长度，及是否可空等有无发生变化。注：如果表已有数据，null-->not null可能出错 （注：只是修改type为hidden则忽略）
        			String fieldDef0 = getFiledDef(fDefs0, false);
        			String fieldDef1 = getFiledDef(fDefs1, false);
        			if( !fieldDef1.equalsIgnoreCase(fieldDef0) 
        					&& !_Field.TYPE_HIDDEN.equals(fDefs1.get("type")) && !_Field.TYPE_HIDDEN.equals(fDefs0.get("type")) ) {
        				
        				try {
        					SQLExcutor.excute( ddlSQLs[4] + " " + fieldDef1.replace(code, ""), newDS);
            			} catch(Exception e1) {
            				try {
            					SQLExcutor.excute( "alter table " +this.table+ " alter " + code + " " + fieldDef1.replace(code, ""), newDS); // for unit Test
            				} catch(Exception e2) {
            					throw e1; // 注：抛出的是e1
            				}
            			}
        			} 
        			
        			// 检查唯一性约束是否有变
        			String unique1 = (String) fDefs1.get("unique");
        			String unique0 = (String) fDefs0.get("unique");
      				
        			if( "true".equals(unique1) && !"true".equals(unique0) ) {
        				// 现在要求唯一，以前没有要求，则加唯一性约束; 如果表里已经有重复的值，会导致创建唯一性约束失败
        				SQLExcutor.excute(ddlSQLs[0], newDS);
        			} 
        			else if( "true".equals(unique0) && !"true".equals(unique1) ) {
        				// 以前要求唯一，现在没有要求，则删除唯一性约束; 可能原表并没有唯一性约束，catch掉异常
        				try {
    	        			SQLExcutor.excute(ddlSQLs[1], newDS);
            			} catch(Exception e) { }
        			}
        			
        			// 按照是否为查询条件，创建索引
        			String isparam1 = (String) fDefs1.get("isparam");
        			String isparam0 = (String) fDefs0.get("isparam");
        			if( "true".equals(isparam1) && !"true".equals(isparam0) ) {
        				try { 
        					SQLExcutor.excute(ddlSQLs[2], newDS); 
        				} catch(Exception e) { }
        			} 
        			else if( "true".equals(isparam0) && !"true".equals(isparam1) ) {
        				try {
    	        			SQLExcutor.excute(ddlSQLs[3], newDS);
            			} catch(Exception e) { }
        			}
        			
        			break;
        		}
        	}
        	
        	if( !exsited ) {
        		// 先查询是否已有数据，不允许在一个有数据的表里增加非空列
				String fieldDef = getFiledDef(fDefs1, existedCount > 0);
				try {
					SQLExcutor.excute("alter table " + this.table + " add " + fieldDef, newDS); 
				} catch(Exception e) { }
        	}
		}
		
		// 被删除的字段（原来有的，在新的定义里没有了）
		for(Map<String, Object> fDefs2 : this.fields ) {
			Object oldCode = fDefs2.get("code");
			boolean exsited = false;
    		for(Map<String, Object> fDefs1 : newFields) {
    			String code = (String) fDefs1.get("code");
 
				if(code.equals(oldCode)) {
        			exsited = true; 
        		}
    		}
    		if( !exsited ) {
    			try {
    				// 先查询该字段是否有值，没有值才删除该列；有值需要去除非空约束
    				String checkSQL = "select count(*) num from " +this.table+ " where " +oldCode+ " is not null";
    				SQLExcutor ex = new SQLExcutor();
    				ex.excuteQuery(checkSQL, newDS);
    				int count = EasyUtils.obj2Int( ex.getFirstRow("num") );
    				if(count == 0) {
    					SQLExcutor.excute("alter table " +this.table+ " drop column " + oldCode, newDS);
    				} else {
    					SQLExcutor.excute("alter table " +this.table+ " modify " +oldCode+ " null", newDS); // 去除非空约束
    				}
    			} catch(Exception e) { }
        	}
    	}
		
		this.fields = newFields;
		initFieldCodes();
	}
	
	// 用于单行数据新增
	public void insert(Map<String, String> valuesMap) {
		Map<Integer, Object> paramsMap = buildInsertParams(valuesMap);
		SQLExcutor.excute(createInsertSQL(), paramsMap, this.datasource);
		
		rcEvent.afterInsert(null, valuesMap, this);
	}
	
	public Long insertRID(Map<String, String> valuesMap) {
		Map<Integer, Object> paramsMap = buildInsertParams(valuesMap);
		Object[] params = new Object[ paramsMap.size() ];
		for(int i = 0; i < params.length; i++) {
			params[i] = paramsMap.get( i+1 );
		}
		
		Long rID = SQLExcutor.excuteInsert(createInsertSQL(), params, this.datasource);
		
		rcEvent.afterInsert(rID, valuesMap, this);
		return rID;
	}

	protected Map<Integer, Object> buildInsertParams(Map<String, String> valuesMap) {
		
		rcEvent.beforeInsert(valuesMap);
		
		Map<Integer, Object> paramsMap = new LinkedHashMap<Integer, Object>();
		int index = 0;
		
		// 定时器跑ETL往录入表写数据时，域信息已经指定
		String pointedDomain = valuesMap.get("domain");
		
		for(String field : this.fieldCodes) {
			Object value = DMUtil.preTreatValue(valuesMap.get(field), fieldTypes.get(index));
			
			// 检查值为空的字段，是否配置自动取号规则
			String defaultVal = this.fieldValues.get(index);
			if( EasyUtils.isNullOrEmpty(value) &&  _Field.isAutoSN(defaultVal) ) {
				String domain = (String) EasyUtils.checkNull(pointedDomain, Environment.getDomainOrign()); // ETL时，输入数据指定好了域
				value = SerialNOer.create(domain, defaultVal, 1, 0).get(0);
			}
			
			paramsMap.put(++index, value);
		}
		
		String creator= Environment.getNotnullUserCode();
		String domain = (String) EasyUtils.checkNull( pointedDomain, Environment.getDomainOrign() );
		paramsMap.put(++index, domain); 
		paramsMap.put(++index, new Timestamp(new Date().getTime())); 
		paramsMap.put(++index, creator);
		paramsMap.put(++index, 0);
		return paramsMap;
	}
	
	// 用于数据清洗或批量导入（含自动取号）
	public void insertBatch(Collection<Map<String, String>> valuesMaps) {
		if(valuesMaps == null || valuesMaps.isEmpty()) return;
		
		List<Map<Integer, Object>> paramsList = new ArrayList<Map<Integer,Object>>();
		for(Map<String, String> valuesMap : valuesMaps) {
			Map<Integer, Object> paramsMap = buildInsertParams(valuesMap);
			paramsList.add(paramsMap);
		}
		
		SQLExcutor.excuteBatch(createInsertSQL(), paramsList , this.datasource);
		
		RecordEventN.log("batch", "create", " add some rows: " + valuesMaps, this);
	}
	
	protected String createInsertSQL() {
		String valueTags = "", fieldTags = "";
		for(String field : this.fieldCodes) {
			valueTags += "?,";
			fieldTags += field + ",";
		}
		String insertSQL = "insert into " + this.table + "(" + fieldTags + "domain,createtime,creator,version) " +
				" values (" + valueTags + " ?, ?, ?, ?)";
		return insertSQL;
	}
	
	public void rollback(Long id, Map<String, Object> old) {
		Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
		String tags = "";
		for(String field: old.keySet()) {
			paramsMap.put(paramsMap.size(), old.get(field));
			if( tags.length() > 0 ) {
				tags += ",";
			}
			tags += field + "=?";
		}
		paramsMap.put(paramsMap.size(), id);
		
		String updateSQL = "update " + this.table + " set " + tags + " where id=?";
		SQLExcutor.excute(updateSQL, paramsMap, this.datasource);
	}

	public void update(Long id, Map<String, String> valuesMap) {
		Map<String, Object> old = rcEvent.beforeUpdate(id, valuesMap, this);
		
		// 如果_version值不为空，则用其实现乐观锁控制
		String _version = (String) EasyUtils.checkNull(valuesMap.remove("_version"), valuesMap.remove("version"));
		if( !EasyUtils.isNullOrEmpty(_version) ) {
			int version1 = EasyUtils.obj2Int(_version);
			int version0 = EasyUtils.obj2Int(old.get("version"));
			if( version1 < version0 ) {
				throw new BusinessException(EX.DM_17);
			}
		}
		
		Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
		String tags = "";
		for(int index = 0; index < this.fieldCodes.size(); index++) {
			String field = this.fieldCodes.get(index);
			if( !valuesMap.containsKey(field) ) continue;
			
			Object value = valuesMap.get(field);
			value = DMUtil.preTreatValue((String)value, fieldTypes.get(index));
			
			paramsMap.put(paramsMap.size(), value);
			tags += field + "=?, ";
		}
		paramsMap.put(paramsMap.size(), new Timestamp(new Date().getTime()));
		paramsMap.put(paramsMap.size(), Environment.getUserCode());
		paramsMap.put(paramsMap.size(), id);
		
		String updateSQL = "update " + this.table + " set " + tags + "updatetime=?, updator=?, version=version+1 where id=?";
		SQLExcutor.excute(updateSQL, paramsMap, this.datasource);
		
		rcEvent.afterUpdate(id, old, this);
	}
	
	public void updateBatch(String ids, String field, String value, boolean addVersion) {
		String version = addVersion ? ", version=version+1" : "";
		String updateSQL = "update " + this.table + " set " + field + "=?, updatetime=?, updator=? " +version+ " where id in (" + ids + ")";
		
		Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
		int index = 0, fieldIndex = this.fieldCodes.indexOf(field);
		if( fieldIndex == -1) {
			throw new BusinessException(EX.parse(EX.DM_32, field));
		}
		
		paramsMap.put(++index, DMUtil.preTreatValue( value, fieldTypes.get(fieldIndex) ));
		paramsMap.put(++index, new Timestamp(new Date().getTime()));
		paramsMap.put(++index, Environment.getUserCode());
		
		SQLExcutor.excute(updateSQL, paramsMap, this.datasource);
		
		String[] idArray = ids.split(",");
		for(String _id : idArray) {
			RecordEventN.log(_id, "update", field + "=" + value, this);
		}
		RecordEventN.log(ids, "update batch", field + "=" + value, this);
	}

	public Map<String, Object> get(Long id) {
		if(id == null) return new HashMap<String, Object>();
		
		String fields = EasyUtils.list2Str( getVisiableFields(false) );

		String sql = "select " + fields + ",domain,creator,version,createtime from " + this.table + " where id=?";
		List<Map<String, Object>> list = SQLExcutor.query(this.datasource, sql, id);
		if( EasyUtils.isNullOrEmpty(list) ) {
			return null;
		}
		return list.get(0);
	}
	
	// 判断是逻辑删除还是物理删除（系统级、单个表级）。流程表默认不再启用回收站，需自行启用
	public boolean isLogicDelete() {
		return "true".equals(ParamManager.getValue(PX.LOGIC_DEL, "false")) || this.logicDel;
	}

	// 物理删除
	public void delete(Long id) {
		if(id == null) return;
		
		Map<String, Object> old = rcEvent.beforeDelete(id, this);
		
		String updateSQL = "delete from " + this.table + " where id=" + id;
		SQLExcutor.excute(updateSQL, this.datasource);
		
        rcEvent.afterDelete(id, old, this);
	}
	
	public static final String deletedTag = "@--";
	// 逻辑删除
	public void logicDelete(Long id) {
		if(id == null) return;
		
		Map<String, Object> old = rcEvent.beforeDelete(id, this);
		
		String domain = EasyUtils.obj2String(old.get("domain")) + deletedTag;
		
		String userCode = Environment.getUserCode();
		String updateSQL = "update " + this.table + " set domain = '" +domain+ "', updator = '" +userCode+ "' where id=" + id;
		SQLExcutor.excute(updateSQL, this.datasource);
		
		if( this.isWorkflow ) {
			updateSQL = "update dm_workflow_status set currentStatus = '" +WFStatus.REMOVED+ "' where tableId = " +this.recordId+ " and itemId=" + id;
			SQLExcutor.excute(updateSQL, this.datasource);
		}
		
        rcEvent.afterLogicDelete(id, old, this);
	}
	
	// 还原数据
	public void restore(Long id) {
		if(id == null) return;
		
		Map<String, Object> old = get(id);
		String domain = EasyUtils.obj2String(old.get("domain")).replaceAll(deletedTag, "");
		
		String updateSQL = "update " + this.table + " set domain = '" +domain+ "' where id=" + id;
		SQLExcutor.excute(updateSQL, this.datasource);
		
		if( this.isWorkflow ) {
			updateSQL = "update dm_workflow_status set currentStatus = '" +WFStatus.NEW+ "' where tableId = " +this.recordId+ " and itemId=" + id;
			SQLExcutor.excute(updateSQL, this.datasource);
		}
		
		// 记录还原日志
        RecordEventN.log(id, "restore", Environment.getUserCode() + " restored one row：" + old, this);
	}
	
	/**
	 * 获取用户可见的字段列表
	 */
	public List<String> getVisiableFields(boolean needName, List<String> fieldCodes) {
		List<String> result = new ArrayList<String>();
        for(String fieldCode : fieldCodes) {
        	if( !this.fieldCodes.contains(fieldCode) ) { // 字段列不是录入表的，则无需判断权限
        		result.add(fieldCode);
        		continue;
        	}
        	
            String fieldRole2 = crole.get(fieldCode);
            String type = ctype.get(fieldCode);
            if( PermissionHelper.checkRole(fieldRole2) && !_Field.TYPE_HIDDEN.equals(type) ) {
            	result.add( needName ? cnm.get(fieldCode) : fieldCode);
            }
        }
        return result;
	}
	
	public List<String> getVisiableFields(boolean needName) {
		return getVisiableFields(needName, this.fieldCodes);
	}
	
	/**
	 * 查询数据，同时进行权限控制。 <br>
	 * 1、如果用户有浏览、维护数据权限，允许其查询其它人创建的记录；否则只能查询本人创建的记录 <br>
	 * 2、加上 customizeTJ 里强制性的过滤条件 <br>
	 * 
	 * 以下参数可自由定义：
	 * 1、fields               查询结果字段
	 * 2、params + strictQuery 查询参数及匹配方式
	 * 3、groupby              汇总维度字段
	 * 4、sortField + sortType 排序结果， 如果没有指定，按字段定义里排序字段进行排序
	 * 
	 * 支持 or 查询：
	 * eg: code|name|desc='关键字'
	 * 
	 * @param page
	 * @param pagesize
	 * @param params
	 * @return
	 */
	public SQLExcutor select(int page, int pagesize, Map<String, String> params) {
		return select(page, pagesize, params, false);
	}
	public SQLExcutor select(int page, int pagesize, Map<String, String> params, boolean isApprover) {
		long start = System.currentTimeMillis();
		Map<Integer, Object> paramsMap = new HashMap<Integer, Object>();
		paramsMap.put(1, Environment.getNotnullUserCode());
		
		if(params == null) {
			params = new HashMap<String, String>();
		}
		rcEvent.beforeSelect(params, this);
		
		String strictQuery = params.remove(_Field.STRICT_QUERY); // 是否精确查询
		String _fields = params.remove("fields"); // eg: /tss/xdata/price_list?fields=name,fee as value
		
		List<String> visiableFields = getVisiableFields(false);
		String defaultFields = EasyUtils.list2Str( visiableFields )+",domain,createtime,creator,updatetime,updator,version,id";
		String fields = (String) EasyUtils.checkNull(_fields, defaultFields);
		boolean noPointed = EasyUtils.isNullOrEmpty(_fields);
		
		String _customizeTJ = (String) EasyUtils.checkNull(this.customizeTJ, " 1=1 ");
		
		// 对fields进行SQL注入检查
		fields = DMUtil.checkSQLInject( fields );
		
		// 对fields 进行FM解析，其可能是个带子查询的语句，定义在自定义SQL里（避开SQL注入检查）
		if( fields.startsWith("macro_") ) {
			List<Map<String, Object>> list = SQLExcutor.queryL("select script from dm_sql_def where code=?", fields);
			if( list.size() > 0 ) {
				fields = (String) list.get(0).get("script"); 
			}
		}
		
		// 增加权限控制，针对有編輯权限的允許查看他人录入数据, '000' <> ? <==> 忽略创建人这个查询条件
		boolean visible = Environment.isAdmin() || Environment.isRobot() || isApprover, anonymousVisiable = false;
		try {
			PermissionHelper ph = PermissionHelper.getInstance();
			List<String> permissions = ph.getOperationsByResource(recordId, RecordPermission.class.getName(), RecordResource.class); // 用户对资源节点的权限
			visible = visible || permissions.contains(Record.OPERATION_VDATA) || permissions.contains(Record.OPERATION_EDATA);
			
			List<?> permissions_a = ph.getOperationsByResourceAndRole(RecordPermission.class.getName(), recordId, UMConstants.ANONYMOUS_ROLE_ID);
			anonymousVisiable = permissions_a.contains(Record.OPERATION_VDATA) || permissions_a.contains(Record.OPERATION_EDATA); // 匿名用户可浏览
		} 
		catch(Exception e) { }
		
		// 审批表: 提交模式下，只能查询本人提交的流程; 如果其指定了其它“创建人”作为查询条件，将什么都查不到
		if( this.isWorkflow && !isApprover && !Environment.isAdmin()) {
			params.put("creator", Environment.getUserCode());
		}
		
		// 设置查询条件
		String condition;
		if( visible && !params.containsKey("creator") ) {
			condition = " '000' <> ? ";
		} else {
			condition = " creator = ? ";
		}
		
		boolean pointedDomain = false, pointedID = false;
		for(String _key : params.keySet()) {
			String _valueStr = params.get(_key);
			String key = _key.toLowerCase();  // code默认都是小写
			
			String $key = MacrocodeCompiler.createMacroCode(key);  // 被自定义过滤条件引用到的参数不再单独过滤
			if( EasyUtils.isNullOrEmpty(_valueStr) || _customizeTJ.indexOf($key) >= 0) continue;
			
			if( "creator,updator,wf_next_processor".indexOf(key) >= 0 && !params.containsKey("id") ) {
				// 支持按姓名、账号、电话、email等查询 录入表
				String sql = "select loginName v from um_user where ? in (loginName,userName,telephone,email)";
				_valueStr = (String) EasyUtils.checkNull(SQLExcutor.queryVL(sql, "v", _valueStr), _valueStr);
			}
			
			// 对paramValue进行检测，防止SQL注入
			_valueStr = DMUtil.fmParse(_valueStr);
			String valueStr = DMUtil.checkSQLInject(_valueStr).replaceAll("，", ",");
			
			if( "creator".equals(key) && visible) {
				paramsMap.put(1, valueStr);  // 替换登录账号，允许查询其它人创建的数据; 
				continue;
			}
			
			if("updator".equals(key)) {
				condition += " and updator = ? ";
				paramsMap.put(paramsMap.size() + 1, valueStr);
				continue;
			}
			
			if("createtime".equals(key) || "updatetime".equals(key)) {
				String[] vals = DMUtil.preTreatScopeValue(valueStr); // 是否查询条件为：从和到	
				if(vals.length == 1) {
					vals = new String[] { vals[0],  DateUtil.formatCare2Second( DateUtil.addDays(DateUtil.today(), 1) )};
				}
				condition += " and " + key + " >= ?  and " + key + " <= ? ";
				paramsMap.put(paramsMap.size() + 1, DMUtil.preTreatValue(vals[0], _Field.TYPE_DATE));
				paramsMap.put(paramsMap.size() + 1, DMUtil.preTreatValue(vals[1], _Field.TYPE_DATE));
				continue;
			}
			
			// Admin或匿名可浏览的表 可以查询指定域下的数据, 用户也可以查看本域下已逻辑删除的数据（BB@--）
			String _deletedTag = Environment.getDomainOrign() + deletedTag;
			if( "domain".equals(key) && (Environment.isAdmin() || anonymousVisiable || valueStr.equals(_deletedTag)) ) { 
				condition += " and domain = ? ";
				paramsMap.put(paramsMap.size() + 1, valueStr);
				pointedDomain = true;
				continue;
			}
			
			if( "id".equals(key) || "ids".equals(key) ) {
				if( valueStr.indexOf(",") >= 0 ) {  
					condition += " and id in (" + EasyUtils.filterEmptyItem(valueStr) + ") ";
				} else {
					condition += " and id = ? ";
					valueStr = valueStr.replace("_copy", ""); // 复制记录
					paramsMap.put(paramsMap.size() + 1, EasyUtils.obj2Long(valueStr));
				}
				pointedID = true;
				continue;
			}

			if( isWorkflow ) {
				String sql_wf = "select itemId from dm_workflow_status where tableId = " +this.recordId;
				Object domain_wf = EasyUtils.checkTrue(Environment.isAdmin(), "", " and domain = '" +Environment.getDomain()+ "'");
				
				if( "wf_status".equals(key) ) { // 按流程状态查询
					condition += " and id";
					if( WFStatus.DRAFT.equals(valueStr) ) {
						condition += " not in (" +sql_wf;
					} else {
						condition += " in (" +sql_wf+ " and currentStatus='" +valueStr+ "' ";
					}
					condition += domain_wf + ")" ;
					continue;
				}
				
				if( "wf_next_processor".equals(key) ) {
					condition += " and id in (" +sql_wf+ " and nextProcessor='" +valueStr+ "' " + domain_wf + ")" ;
					continue;
				}
				
				if("wf_process_time".equals(key)) {
					String[] vals = DMUtil.preTreatScopeValue(valueStr); // 是否查询条件为：从和到	
					if(vals.length == 1) {
						vals = new String[] { vals[0],  DateUtil.formatCare2Second( DateUtil.addDays(DateUtil.today(), 1) )};
					}
					
					condition += " and id in (" +sql_wf + " and lastProcessTime >= ? and lastProcessTime <= ?" + domain_wf + ")" ;
					paramsMap.put(paramsMap.size() + 1, DMUtil.preTreatValue(vals[0], _Field.TYPE_DATE));
					paramsMap.put(paramsMap.size() + 1, DMUtil.preTreatValue(vals[1], _Field.TYPE_DATE));
					continue;
				}
			}
			
			// eg: othercondition = "and curdate() between c3 and c4";
			if("othercondition".equals(key)) {
				condition += " " + _valueStr + " ";
				continue;
			}
			
			// 录入表字段过滤
			int fieldIndex = this.fieldCodes.indexOf(key);
			if(fieldIndex >= 0) {
				String paramType = this.fieldTypes.get(fieldIndex);
				paramType = EasyUtils.checkNull(paramType,  _Field.TYPE_STRING).toString().toLowerCase();
				
				String[] vals = DMUtil.preTreatScopeValue(valueStr); // 是否查询条件为：从和到				
				if(vals.length == 1) {
				
					boolean isStringType = ( paramType == null || _Field.TYPE_STRING.equals(paramType) );
					
					// 如果是一个逗号分隔的字符串，使用in查询
					if( valueStr.indexOf(",") > 0 && (isStringType || _Field.TYPE_INT.equals(paramType)) ) {  
						condition += " and " + key + " in (" + ("\'" + valueStr.replaceAll(",", "\',\'") + "\'") + ") ";
					}
					else {
						String val_ = EasyUtils.obj2String(vals[0]).trim().toLowerCase();
						if( val_.startsWith("is ") && val_.endsWith(" null") ) {
							condition += " and " + key + " " + val_;  // is null | is not null
						}
						else { 
							Object val = DMUtil.preTreatValue(vals[0], paramType);
							
							/* 字符串支持模糊查询（_Recorder过来的前台查询默认用模糊查询，_Database自己发起的则严格查询）*/
							if( isStringType && "false".equals(strictQuery) && !"true".equals(cmatch.get("strictQuery")) ) { 
								condition += " and " + key + " like ? ";
								val = "%"+val+"%";
							} else {
								condition += " and " + key + " = ? ";
							}
							paramsMap.put(paramsMap.size() + 1, val);
						}
					}
				}
				else if(vals.length == 2) {
					condition += " and " + key + " >= ?  and " + key + " <= ? ";
					paramsMap.put(paramsMap.size() + 1, DMUtil.preTreatValue(vals[0], paramType));
					paramsMap.put(paramsMap.size() + 1, DMUtil.preTreatValue(vals[1], paramType));
				}
			}
			else { // or 或查询 eg: code|name = 'xx'
				String[] orFields = EasyUtils.split(key, "|");
				if( orFields.length > 1 ) {
					condition += " and ( 1=0 ";
					for(String field : orFields) {
						if( !this.fieldCodes.contains(field) ) continue;
						
						if( "false".equals(strictQuery) ) { 
							condition += " or " + field + " like ? ";
							valueStr = "%"+valueStr+"%";
						} else {
							condition += " or " + field + " = ? ";
						}
						paramsMap.put(paramsMap.size() + 1, valueStr);
					}
					condition += " ) ";
				}
			}
		}
		
		/* 
		 * 如果用户的域不为空，则只筛选出该域下用户创建的记录
		 * 只有单机部署的BI允许无域；SAAS部署必须每个组都要有域，每个人必属于某个域。Admin不属于任何域。
		 * 注：部分全局基础表需要忽略域限制：比如行政区划等，customizeTJ: <#if 1=0>ignoreDomain</#if>
		 */
		boolean _ignoreDomain = this.ignoreDomain || _customizeTJ.indexOf("ignoreDomain") > 0;
		if( _ignoreDomain || pointedDomain || (anonymousVisiable && (Environment.isAnonymous() || pointedID)) ) {  
			// 匿名用户可浏览，则无需过滤域
		}
		else {
			_customizeTJ += DMConstants.DOMAIN_CONDITION;
		}
		
		// _customizeTJ 可以依据 params 里的参数信息灵活解析成各种查询条件组合
		_customizeTJ = (String) EasyUtils.checkNull( DMUtil.fmParse(_customizeTJ, params), "1=1" );
		if( _customizeTJ.toLowerCase().trim().startsWith("and")) {
			_customizeTJ = "1=1 " + _customizeTJ;
		}
		condition += " and ( ( " + DMUtil.fmParse(_customizeTJ + " )  or -1 = ${_userId!-10000} ") + " ) "; // Admin对所有数据可见
		
		// 设置排序方式
		String _sortField = params.get("sortField");
		
		List<String> sortFieldList = new ArrayList<String>();
		if( !EasyUtils.isNullOrEmpty(_sortField) ) {
			String[] sortFields = _sortField.split(","); // 支持按多个字段排序
			String[] sortTypes  = EasyUtils.obj2String( params.get("sortType") ).split(",");
			String[] sortTypes_ = new String[sortFields.length];
			for(int i = 0; i < sortTypes.length; i++) {
				sortTypes_[i] = sortTypes[i];
			}
			
			int index = 0;
			for(String sortField : sortFields) {
				// 判断字段是否存在，无需再检查SQL注入
				String sortField_= sortField.toLowerCase().replaceAll(" desc", "").replaceAll(" asc", "").trim();
				String sortType_ = EasyUtils.obj2String( sortTypes_[index] );
				if( this.fieldCodes.contains(sortField_) || "id,createtime,creator,updatetime,updator".indexOf(sortField_) >= 0 ) { 
					if("onlynull".equals(sortType_)) {
						condition += " and " + sortField + " is null ";
					}
					else if("notnull".equals(sortType_)) {
						condition += " and " + sortField + " is not null ";
					}
					else if( noPointed || _fields.indexOf(sortField_) >= 0 ) {
						sortFieldList.add( sortField + " " + sortType_ );
					}
				}
				index ++;
			}
		}
		if( noPointed ) {
			sortFieldList.addAll( csort.values() ); // 如果查询时没有指定排序字段，则按字段定义里排序字段（eg: 'sort':'asc'）进行排序
			sortFieldList.add( "id desc");          // 始终加上ID排序，保证查询结果排序方式唯一
		}
		
		String orderby = "";
		if(sortFieldList.size() > 0) {
			orderby = " order by " + EasyUtils.list2Str(sortFieldList);
		}
		
		// 是否有group by
		String groupby = EasyUtils.obj2String( params.remove("groupby") );
		if( !EasyUtils.isNullOrEmpty(groupby) ) {
			groupby = " group by " + groupby + " ";
		}
		
		String selectSQL = "select " + fields 
					+ " from  " + this.table
					+ " where " + condition 
					+ groupby
					+ orderby;
		
		SQLExcutor ex = new SQLExcutor();
		ex.excuteQuery(selectSQL, paramsMap, page, pagesize, this.datasource);
		
		// 对结果集里的字段进行权限过滤，过渡掉没有查看权限的字段
		if( !noPointed ) {
			for(Map<String, Object> item : ex.result) {
	    		for(String field : ex.selectFields) {
	    			if( this.fieldCodes.contains(field) && !visiableFields.contains(field) ) {
	    				item.remove(field);
	    			}
	    		}
	        }
		}
		
		if(this.needQLog) {
			String tag = "pointed:" + !noPointed + ",wf:" + isApprover + ",export:"+params.get("export");
			AccessLogRecorder.outputAccessLog("record-" + recordId, recordName, tag, params, start);
		}
		
		rcEvent.afterSelect(ex, this);
		return ex;
	}

	public Document getGridTemplate() {
		StringBuffer sb = new StringBuffer();
        sb.append("<grid><declare sequence=\"true\" header=\"checkbox\">").append("\n");
        
        int index = 0;
        boolean hasFileField = false;
        for(String fieldName : fieldNames) {
            String fieldCode = fieldCodes.get(index);
            String fieldAlign = (String) EasyUtils.checkNull(fieldAligns.get(index), "center");
            String fieldWidth = fieldWidths.get(index);
            String fieldRole2 = fieldRole2s.get(index);
            String fieldType  = fieldTypes.get(index);
            String fieldPattern = fieldPatterns.get(index);
            
            if( _Field.TYPE_DATETIME.equalsIgnoreCase(fieldType) ) { // GridNode里转换异常（date类型要求值也为date）
            	fieldType = _Field.TYPE_DATE;  
            } 
            if( _Field.TYPE_FILE.equalsIgnoreCase(fieldType) ) {
            	hasFileField = true;
            }
            fieldWidth = GridTemplet.transColWidth(fieldWidth);
            
            boolean isHidden = "hidden".equals( fieldTypes.get(index) );
            if( PermissionHelper.checkRole(fieldRole2) && !isHidden ) {
            	// 如果是下拉列表，把下拉列表放到 values 和 texts 里
            	Map<String, Object> fDefs = this.fieldMap.get(fieldCode);
            	String _options = "";
            	@SuppressWarnings("unchecked")
				Map<String, String> options = (Map<String, String>) fDefs.get("options");
            	if( options != null && options.containsKey("codes") ) {
            		String values = options.get("codes");
            		String texts = options.get("names");
            		_options = " values=\"" + values + "\" texts=\"" + EasyUtils.checkNull(texts, values) + "\" ";
            		fieldType = _Field.TYPE_STRING; 
            	}
            	
            	sb.append("<column name=\"" + fieldCode + "\" mode=\"" + fieldType + "\" pattern=\"" + fieldPattern 
            			+ "\" caption=\"" + fieldName + "\" align=\"" + fieldAlign + "\" " + fieldWidth + _options + " />").append("\n");
            }
            index++;
        }
        
        if( this.isWorkflow ) {
        	sb.append("<column name=\"wfstatus\" mode=\"string\" caption=\"流程状态\" sortable=\"true\" width=\"35px\"/>").append("\n");
        	sb.append("<column name=\"wfapplier\" mode=\"string\" caption=\"发起人\" sortable=\"true\" width=\"30px\"/>").append("\n");
        	sb.append("<column name=\"wfapplyTime\" mode=\"date\" format=\"yyyy-mm-dd\" caption=\"发起时间\" width=\"40px\"/>").append("\n");
        	sb.append("<column name=\"wfto\" mode=\"string\" caption=\"审批人列表\" width=\"40px\"/>").append("\n");
        	sb.append("<column name=\"nextProcessor\" mode=\"string\" caption=\"当前审批人\" width=\"40px\"/>").append("\n");
        	sb.append("<column name=\"wfcc\" mode=\"string\" caption=\"抄送\" width=\"30px\"/>").append("\n");
        }
        if(this.needFile && !hasFileField) { // 如果已有专门的附件展示字段，则无需再展示默认的“附件”列
        	sb.append("<column name=\"fileNum\" mode=\"string\" caption=\"附件\" width=\"30px\"/>").append("\n");
        }
        
        // 判断是否显示创建人和创建时间, customizeTJ: 1=1<#if 1=0>showCUV< /#if>
        if( showCreator || (this.customizeTJ+"").indexOf("showCUV") >= 0 ) {
        	sb.append("<column name=\"createtime\"  caption=\"创建时间\" sortable=\"true\" width=\"60px\"/>").append("\n");
            sb.append("<column name=\"creatorName\"  caption=\"创建人\" sortable=\"true\" width=\"40px\"/>").append("\n");
            sb.append("<column name=\"creator\"  display=\"none\"/>").append("\n");
        } else {
        	sb.append("<column name=\"createtime\" display=\"none\"/>").append("\n");
        	sb.append("<column name=\"creator\" display=\"none\"/>").append("\n");
        }
        sb.append("<column name=\"updatetime\" display=\"none\"/>").append("\n");
        sb.append("<column name=\"updator\" display=\"none\"/>").append("\n");
        sb.append("<column name=\"version\" display=\"none\"/>").append("\n");
        
        // ID列默认隐藏
        sb.append("<column name=\"id\" display=\"none\"/>").append("\n");
        String domainShow = Environment.isAdmin() && !this.ignoreDomain ? "" : "display=\"none\"";
		sb.append("<column name=\"domain\" caption=\"域\" " +domainShow+ " width=\"30px\"/>").append("\n");
        
        sb.append("</declare>\n<data></data></grid>");
        
    	return XMLDocUtil.dataXml2Doc(sb.toString());
	}
	
	public List<Map<String, Object>> getFields() {
		return this.fields;
	}
	
	private static Map<String, String> dsMappingType = new HashMap<String, String>();
	
	public static String getDBType(String datasource) {
		String result = dsMappingType.get(datasource);
		if(result != null) return result;
		
		Pool connpool = JCache.getInstance().getPool(datasource);
		if(connpool == null) {
			throw new BusinessException( EX.parse(EX.DM_02, datasource) );
		}
        Cacheable connItem = connpool.checkOut(0);
        Connection conn = (Connection) connItem.getValue();
        
		try {
			String driveName = conn.getMetaData().getDriverName();
			log.debug(" database diverName: 【 " + driveName + "】。");
			
			for(String type : DB_TYPE) {
				if (driveName.indexOf(type) >= 0) {
					dsMappingType.put(datasource, result = type);
		        }
			}
		} catch (SQLException e) {
			
		} finally {
            connpool.checkIn(connItem); // 返回连接到连接池
        }
        
		return result;
	}
	
	static String[] DB_TYPE = new String[] {"H2", "MySQL", "Oracle", "PostgreSQL", "SQL Server"};
	
	public static _Database getDB(Record record) {
		String type = getDBType(record.getDatasource());
		return getDB(type, record);
	}
	
	public static _Database getDB(String type, Record record) {
		if( type.startsWith( DB_TYPE[1] ) ) {
			return new _MySQL(record);
		}
		else if( type.startsWith( DB_TYPE[2] ) ) {
			return new _Oracle(record);
		}
		else if( type.startsWith( DB_TYPE[3] ) ) {
			return new _PostgreSQL(record);
		} 
		else if( type.indexOf( DB_TYPE[4] ) >= 0 ) {
			return new _SQLServer(record);
		} 
		else {
			return new _H2(record);
		}
	}
	
	public RecordEvent getRcEvent() {
		RecordEvent ev = null;
		try {
			String rcEventClazz = DMUtil.getExtendAttr(this.remark, "rcEventClass");
			if( rcEventClazz != null ) {
				ev = (RecordEvent) BeanUtil.newInstanceByName(rcEventClazz);
			}
		} catch(Exception e) {
			log.error("init " + this.recordName + " event error : ", e);
		}
		
		return (RecordEvent) EasyUtils.checkNull(ev, new RecordEventN());
	}
}
