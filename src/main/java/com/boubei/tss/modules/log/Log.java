/* ==================================================================   
 * Created [2016-06-22] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.modules.log;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hibernate.annotations.GenericGenerator;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.display.grid.GridAttributesMap;
import com.boubei.tss.framework.web.display.grid.IGridNode;
import com.boubei.tss.framework.web.display.xform.IXForm;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

/** 
 * 日志表
 */
@Entity
@Table(name = "component_log")
@JsonIgnoreProperties(value={"pk", "attributes4XForm", "attributes", "operatorBrowser"})
public class Log implements IEntity, IXForm, IGridNode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "log_sequence")
    @GenericGenerator(name = "log_sequence", strategy = "native")
    private Long    id;
    
    @Column(nullable = false)  
    private String operateTable;  // 操作的对象
    private String operationCode; // 操作Code
    private Date   operateTime;   // 操作时间
    
    private Long   operatorId;    // 操作者ID
    private String operatorName;  // 操作者Name
    private String operatorIP;    // 操作者IP
    private String operatorBrowser;  // 操作者浏览器类型
    
    @Column(length = 4000)  
    private String  content;      // 操作内容（LogOutputTask里限定长度为2000）
    
    private Integer methodExcuteTime; // 方法执行时间（单位: 微秒）
    private String udf1;
    
    public Log() { }

    public Log(String operationCode, Object entity) {
    	if(operationCode != null && operationCode.length() > 100) {
    		operationCode = operationCode.substring(0, 100);
    	}
    	entity = entity == null ? "" : entity;

    	this.setOperatorId( Environment.getUserId() );
		this.setOperatorName( Environment.getUserName() );
        this.setOperatorIP( Environment.getClientIp() +" "+ Environment.getDomainCN() );
        this.setOperationCode( operationCode );
        this.setOperateTable ( entity.getClass().getName() );
        this.setOperateTime  ( new Date() );
        
        String content;
	    if(entity instanceof String || entity instanceof Number) {
	    	content = String.valueOf(entity);
	   	} else {
	   		content = ToStringBuilder.reflectionToString(entity, ToStringStyle.SHORT_PREFIX_STYLE);
	   	}
        this.setContent( content);
        this.setOperatorBrowser( Environment.getOrigin() );
    }
    
    public Log(String opTable, String opCode, Object entity) {
    	this(opCode, entity);
    	this.setOperateTable(opTable);
    }
    
    public void formatContent() {
    	String content = this.getContent();
    	int index1 = content.indexOf("begin: {"),
    		index2 = content.indexOf("after: {");
    	
    	if(index1 >= 0 && index2 > 0) {
    		String compareResult = "";
    		String begin = content.substring(index1 + 8, index2 - 4);
    		String after = content.substring(index2 + 8, content.length() - 1);
    		List<String> ignoreFields = Arrays.asList("creator", "createTime", "updator", "updateTime", "version");
    		
    		String[] s1 = begin.split(",");
    		String[] s2 = after.split(",");
    		Map<String, String> m1 = new HashMap<String, String>();
    		for(String s : s1) {
    			String[] pair = s.trim().split("=");
    			if(pair.length == 2) {
    				m1.put(pair[0], pair[1]);
    			}
    		}
    		for(String s : s2) {
    			String[] pair = s.trim().split("=");
    			if(pair.length == 2) {
    				String key = pair[0];
					if(  ignoreFields.contains(key) ) continue;
    				
    				String newV = pair[1];
    				String oldV = m1.get(key);
    				if( !newV.equals(oldV) ) {
    					compareResult += key + ": " + oldV + " => " + newV + ", ";
    				}
    			}
    		}
    		
    		this.setContent( compareResult );
    	}
    }
    
    public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
 
    public String getContent() {
        return content;
    }
 
    public Long getId() {
        return id;
    }
 
    public Date getOperateTime() {
        return operateTime;
    }
 
    public String getOperationCode() {
        return operationCode;
    }
 
    public Long getOperatorId() {
        return operatorId;
    }
 
    public String getOperatorIP() {
        return operatorIP;
    }
 
    public String getOperatorName() {
        return operatorName;
    }
 
    public String getOperateTable() {
        return operateTable;
    }
 
    public void setContent(String content) {
        this.content = content;
        formatContent();
    }
 
    public void setId(Long id) {
        this.id = id;
    }
 
    public void setOperateTime(Date operateTime) {
        this.operateTime = operateTime;
    }
 
    public void setOperationCode(String operationCode) {
        this.operationCode = operationCode;
    }
 
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
 
    public void setOperatorIP(String operatorIP) {
        this.operatorIP = operatorIP;
    }
 
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }
 
    public void setOperateTable(String table) {
        this.operateTable = table;
    }
    
    public Map<String, Object> getAttributes4XForm() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", this.id);
        map.put("_content", this.content);
        map.put("_operateTime", DateUtil.formatCare2Second(this.operateTime));
        map.put("_operatorName", this.operatorName);
        map.put("_browser", this.getOperatorBrowser());
        
        return map;
    }
    
    public GridAttributesMap getAttributes(GridAttributesMap map) {
        map.put("id", this.id);
        map.put("operateTable", this.operateTable);
        map.put("operateTime", this.operateTime);
        map.put("operationCode", this.operationCode);
        map.put("operatorIP", this.operatorIP);
        map.put("operatorName", this.operatorName);
        map.put("methodExcuteTime", this.methodExcuteTime);
        
        String content = EasyUtils.obj2Json(this.getContent()).replaceAll("\"", "");
        map.put("content", content.substring(0, Math.min(content.length(), 128)));
        map.put("origin", this.getOperatorBrowser() );
        map.put("udf1", this.getUdf1());
        
        return map;
    }

    public Integer getMethodExcuteTime() {
        return methodExcuteTime;
    }

    public void setMethodExcuteTime(Integer methodExcuteTime) {
        this.methodExcuteTime = methodExcuteTime;
    }
    
	public Serializable getPK() {
		return this.id;
	}

	public String getOperatorBrowser() {
		return operatorBrowser;
	}

	public void setOperatorBrowser(String operatorBrowser) {
		this.operatorBrowser = operatorBrowser;
	}

	public String getUdf1() {
		return udf1;
	}

	public void setUdf1(String udf1) {
		this.udf1 = udf1;
	}
}

