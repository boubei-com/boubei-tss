package com.boubei.tss.modules.sn;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.log.LogDisable;

/**
 * 取号器：前缀YYYYMMDD四位递增数字
 */
@LogDisable
@Entity
@Table(name = "x_serialno")
public class SerialNO extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "serialno_sequence")
	@GenericGenerator(name = "serialno_sequence", strategy = "native")
	private Long id;
	
	@Column(nullable = false, length = 50)
	private String precode;
	
	@Column(nullable = false)
	private Date day;
	
	private int lastNum;

	public Serializable getPK() {
		return this.id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPrecode() {
		return precode;
	}

	public void setPrecode(String precode) {
		this.precode = precode;
	}

	public Date getDay() {
		return day;
	}

	public void setDay(Date day) {
		this.day = day;
	}

	public int getLastNum() {
		return lastNum;
	}

	public void setLastNum(int lastNum) {
		this.lastNum = lastNum;
	}
}
