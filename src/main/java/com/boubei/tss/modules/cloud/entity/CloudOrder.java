/* ==================================================================
 * Created [2019-01-01] by Jon.King
 * ==================================================================
 * TSS
 * ==================================================================
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2019-2025
 * ==================================================================
 */
package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.boubei.tss.dm.record.ARecordTable;

@Entity
@Table(name = "cloud_module_order")
@SequenceGenerator(name = "module_order_seq", sequenceName = "module_order_seq", initialValue = 1, allocationSize = 10)
public class CloudOrder extends ARecordTable {

    public final static String NEW = "待付款";
    public final static String CANCELED = "已取消";
    public final static String PAYED = "已付款";
    public final static String PART_PAYED = "部分付款";
    public final static String RETURN = "已退款";
    public final static String PART_RETURN = "已退款";

    public final static String PAYTYPE_1 = "余额";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "module_order_seq")
    private Long id;

    private String product;

    @Column(nullable = false)
    private String type;  // 订单类型：自定义实现类

    private Long invite_user_id; // 邀请人
    private Long module_id;
    private Integer account_num;
    private Integer month_num;

    private String order_no; // 订单号 编码规则=时间戳-id
    private Date order_date = new Date();


    private String status = NEW; // 待付款、取消、已付款

    private Double price;
    private Double money_cal; // 应付金额

    private Double rebate; // 折扣
    private Double derate; // 减免

    private String appid; // (小程序)应用ID
    private String mchid; // (微信)商户号

    @Lob
    private String params; // 更多参数
    private String remark; // 备注

    private Date pay_date;
    private String pay_type;
    private String pay_clazz;
    private Double money_real; // 实付金额

    @Transient
    public Boolean singleSubAuthorize = false;

    @Transient
    public Boolean checkSMS = true;

    public Serializable getPK() {
        return this.getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getModule_id() {
        return module_id;
    }

    public void setModule_id(Long module_id) {
        this.module_id = module_id;
    }

    public Integer getAccount_num() {
        return account_num;
    }

    public void setAccount_num(Integer account_num) {
        this.account_num = account_num;
    }

    public Integer getMonth_num() {
        return month_num;
    }

    public void setMonth_num(Integer month_num) {
        this.month_num = month_num;
    }

    public String getOrder_no() {
        return order_no;
    }

    public void setOrder_no(String order_no) {
        this.order_no = order_no;
    }

    public Date getOrder_date() {
        return order_date;
    }

    public void setOrder_date(Date order_date) {
        this.order_date = order_date;
    }

    public Date getPay_date() {
        return pay_date;
    }

    public void setPay_date(Date pay_date) {
        this.pay_date = pay_date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getMoney_cal() {
        return money_cal;
    }

    public void setMoney_cal(Double money_cal) {
        this.money_cal = money_cal;
    }

    public Double getMoney_real() {
        return money_real;
    }

    public void setMoney_real(Double money_real) {
        this.money_real = money_real;
    }

    public Double getRebate() {
        return rebate;
    }

    public void setRebate(Double rebate) {
        this.rebate = rebate;
    }

    public Double getDerate() {
        return derate;
    }

    public void setDerate(Double derate) {
        this.derate = derate;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Long getInvite_user_id() {
        return invite_user_id;
    }

    public void setInvite_user_id(Long invite_user_id) {
        this.invite_user_id = invite_user_id;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getPay_type() {
        return pay_type;
    }

    public void setPay_type(String pay_type) {
        this.pay_type = pay_type;
    }

    public String getMchid() {
        return mchid;
    }

    public void setMchid(String mchid) {
        this.mchid = mchid;
    }

	public String getPay_clazz() {
		return pay_clazz;
	}

	public void setPay_clazz(String pay_clazz) {
		this.pay_clazz = pay_clazz;
	}


}
