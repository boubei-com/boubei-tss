/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.sso.Environment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p> Temp.java </p>
 * 临时表，用以批量查询等操作，可取代in 查询。
 */
@Entity
@Table(name = "TBL_TEMP_")
@SequenceGenerator(name = "temp_sequence", sequenceName = "temp_sequence", initialValue = 1, allocationSize = 10)
@JsonIgnoreProperties(value={"pk", "PK"})
public class Temp implements IEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "temp_sequence")
	private Long pK;
    
	private Long id; 
	private Long thread; // 当前线程ID，用以多线程场景
	
	private String udf1;
	private String udf2;
	private String udf3;
	private String udf4;
	private String udf5;
	private String udf6;
	private String udf7;
	private String udf8;
	
	public Temp() {
		this.setThread(Environment.threadID());
	}
	
	public Temp(Long thread) {
		this.setThread(thread);
	}
	
	public Temp(Long thread, Long id) {
		this(thread);
		this.setId(id);
	}
    
    public Long getId() {
        return id;
    }
 
    public void setId(Long id) {
        this.id = id;
    }

    public String getUdf1() {
        return udf1;
    }

    public void setUdf1(String udf1) {
        this.udf1 = udf1;
    }

    public String getUdf2() {
        return udf2;
    }

    public void setUdf2(String udf2) {
        this.udf2 = udf2;
    }

    public String getUdf3() {
        return udf3;
    }

    public void setUdf3(String udf3) {
        this.udf3 = udf3;
    }
	
	public boolean equals(Object obj) {
		return this.toString().equals(obj.toString());
	}

	public int hashCode() {
		return this.toString().hashCode();
	}

	public String toString() {
		return "{id:" + this.getId() + ", thread:" + this.thread + 
				", udf1:" + this.udf1 + ", udf2:" + this.udf2 + ", udf3:" + this.udf3+ "}";
	}

	public Long getThread() {
		return thread;
	}

	public void setThread(Long thread) {
		this.thread = thread;
	}
	
	public Serializable getPK() {
		return this.pK;
	}

	public void setPK(Long pk) {
		this.pK = pk;
	}

	public String getUdf4() {
		return udf4;
	}

	public void setUdf4(String udf4) {
		this.udf4 = udf4;
	}

	public String getUdf5() {
		return udf5;
	}

	public void setUdf5(String udf5) {
		this.udf5 = udf5;
	}

	public String getUdf6() {
		return udf6;
	}

	public void setUdf6(String udf6) {
		this.udf6 = udf6;
	}

	public String getUdf7() {
		return udf7;
	}

	public void setUdf7(String udf7) {
		this.udf7 = udf7;
	}

	public String getUdf8() {
		return udf8;
	}

	public void setUdf8(String udf8) {
		this.udf8 = udf8;
	}
}

	