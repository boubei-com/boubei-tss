/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence.pagequery;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MacrocodeCompiler;

/**
 * 分页条件查询基类
 */
public abstract class PaginationQuery {

    static final String[] COMMON_IGNORE_PROPERTIES = new String[] { "page", "conditionMacrocodes", "class", "ignoreProperties", "order" };
	
    protected EntityManager em;
    
    protected String ql;  // SQL 或  HQL 语句，里面带宏，传入时不带order by
    
    protected MacrocodeQueryCondition condition;
 
    public PaginationQuery(EntityManager em, String ql, MacrocodeQueryCondition condition) {
        if ( em == null ) throw new BusinessException("em is null");
        if ( condition == null ) throw new BusinessException("condition is null");
        if ( EasyUtils.isNullOrEmpty(ql) ) throw new BusinessException("query ql is null");
        
        this.em = em;
        this.ql = ql;
        this.condition = condition;
    }

    /**
     * <p>
     * 执行分页查询，获取分页结果及相关信息
     * </p>
     * @return
     */
    public PageInfo getResultList() {
        Set<String> ignores = condition.getIgnoreProperties();
        ignores.addAll( Arrays.asList(COMMON_IGNORE_PROPERTIES) );
        Map<String, Object> properties = BeanUtil.getProperties(condition, ignores);
        
        // 过滤空参数、无效参数对应的宏代码 
        Map<String, Object> macrocodes = condition.getConditionMacrocodes();
        Map<String, Object> _macrocodes = new HashMap<String, Object>();
        for (  String key : macrocodes.keySet() ) {
            Object regex = macrocodes.get(key);
            
            if (key.startsWith("${") && key.endsWith("}")) {
                String name = key.substring(2, key.length() - 1);
                Object value = properties.get(name);
                
                // 查询条件字段值为空，则移除该条件
                if ( !isValueNullOrEmpty(value) ) { 
                	_macrocodes.put(key, regex);
                }
            } else { // 如果条件表达式是确定的，则用 #{xxx}，eg: #{domain} = "and o.createId in (1,2,3)"
            	String _key = (String) EasyUtils.checkTrue( key.indexOf("{") > 0, key, MacrocodeCompiler.createMacroCode(key) );
            	String _key$ = key.replace("#", "$");
            	if( !_macrocodes.containsKey(_key$) ) {
            		_macrocodes.put(_key, regex);
            		_macrocodes.put(_key$, regex);
            	}
            }
        }
        
        // 添加HQL中order by 语句
        appendOrderBy();
        
        // 解析带了宏定义的QL语句
        String queryQl = MacrocodeCompiler.run(ql, _macrocodes, false);
 
        PageInfo page = condition.getPage();
        page.setItems(null); // 清空上次的查询结果，一个condition多次被用来查询的情况
        page.setTotalRows(getTotalRows(queryQl, properties));
        
        // 获取当前页数据记录集
        Query query = createQuery(queryQl.replace("order by 1", ""));
        query.setFirstResult(page.getFirstResult());
        query.setMaxResults(page.getPageSize());
        
        // 为查询语句设置相应的参数
        setProperties4Query(query, properties);
        page.setItems( query.getResultList() );

        return page;
    }
    
    /* 添加HQL中order by 语句 */
    private void appendOrderBy(){
    	String orderBy = "";
        for ( String field : condition.getOrderByFields() ) {
        	if ( orderBy.length() > 0 && !EasyUtils.isNullOrEmpty(field) ) {
                orderBy += ", ";
            }
            orderBy += field;
        }
        
        if ( orderBy.length() > 0 ) {
            ql += " order by " + orderBy;
    	}
    }
    
    /**
     * <p>
     * 为查询语句设置相应的参数
     * </p>
     * @param query 查询
     * @param properties 参数集
     */
    protected void setProperties4Query(Query query, Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if ( !isValueNullOrEmpty(value) ) {
            	query.setParameter( entry.getKey(), value );
            }
        }
    }

    /**
     * <p>
     * 获取总记录数
     * </p>
     * @param sumQl 统计QL语句
     * @param properties 参数集
     * @return Integer 总记录数
     */
    protected Integer getTotalRows(String ql, Map<String, Object> properties)  {
    	// 生成对应的总记录数统计QL语句（不能含有子查询）
    	int fromIndex = ql.toLowerCase().indexOf("from ");
    	int toIndex = ql.toLowerCase().lastIndexOf(" order by ");
    	if(toIndex <= 0) {
    	    toIndex = ql.length();
    	}
    	String sumHql = "select count(*) " + ql.substring(fromIndex, toIndex);
    	
        Query query = this.createQuery(sumHql); 
        setProperties4Query(query, properties);
        return EasyUtils.obj2Int(query.getSingleResult());
    }

    /**
     * <p>
     * 通过QL语句创建对应的Query对象
     * </p>
     * @param ql 查询HQL/SQL语句
     * @return Query对象
     */
    protected abstract Query createQuery(String ql);
    
    boolean isValueNullOrEmpty(Object value) {
        if ( value == null ) {
            return true;
        }
        
        if( value instanceof String &&  EasyUtils.isNullOrEmpty(value) ) {
            return true;
        }
        
        if( value instanceof Collection<?> && ((Collection<?>)value).isEmpty()) {
            return true;
        }
        
        return false;
    }
}
