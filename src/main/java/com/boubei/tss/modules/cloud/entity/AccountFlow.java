package com.boubei.tss.modules.cloud.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.modules.cloud.pay.AbstractProduct;
import com.boubei.tss.modules.sn.SerialNOer;

/**
 * 域账户流水
 */
@Entity
@Table(name = "cloud_account_flow")
@SequenceGenerator(name = "account_flow_seq", sequenceName = "account_flow_seq", initialValue = 1, allocationSize = 10)
public class AccountFlow implements IEntity {

	public final static String TYPE0 = "系统使用费";
	public final static String TYPE1 = "充值";
	public final static String TYPE2 = "代理费";
	public final static String TYPE3 = "积分充值";
	public final static String TYPE4 = "积分支出";
	public final static String TYPE5 = "积分冻结";

	public static final String PAYMENT0 = "系统赠送";
	public static final String PAYMENT1 = "系统扣款";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "account_flow_seq")
	private Long id;

	private Long account_id;

	private String sn; // 流水号

	private String type;    // 费用类型：充值/短信费/查单费/系统使用费/报表定制费/接口对接费

	private String payment; // 支付方式：系统扣款、系统赠送、微信支付、支付宝支付、对公转账、对私转账

	private String order_no; // 关联的订单号

	private Double money; 	 // 本次金额
	private Double balance;  // 余额（后）
	private Double balance_freeze; // 冻结余额（后）

	private Date   pay_time;  // 入账时间
	private String pay_man;  // 操作人
	private String remark;  // 备注
	
	private Long fk; // 外键

	public AccountFlow() {
	}

	public AccountFlow(Account account, AbstractProduct product, String type, String remark) {
		this(account, type, remark);
		this.setOrder_no(product.co.getOrder_no());
		this.setPay_man(product.payer);
		this.setPayment(product.payType);
	}

	public AccountFlow(Account account, String type, String remark) {
		this.setAccount_id(account.getId());
		this.setPay_time(new Date());
		this.setSn(SerialNOer.get("AF", true));
		this.setType(type);
		this.setRemark(remark);
		this.setBalance(account.getBalance());
		this.setBalance_freeze(account.getBalance_freeze());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSn() {
		return sn;
	}

	public void setSn(String sn) {
		this.sn = sn;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPayment() {
		return payment;
	}

	public void setPayment(String payment) {
		this.payment = payment;
	}

	public Double getMoney() {
		return money;
	}

	public void setMoney(Double money) {
		this.money = money;
	}

	public Double getBalance() {
		return balance;
	}

	public void setBalance(Double balance) {
		this.balance = balance;
	}

	public Double getBalance_freeze() {
		return balance_freeze;
	}

	public void setBalance_freeze(Double balance_freeze) {
		this.balance_freeze = balance_freeze;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Serializable getPK() {
		return this.getId();
	}

	public Date getPay_time() {
		return pay_time;
	}

	public void setPay_time(Date pay_time) {
		this.pay_time = pay_time;
	}

	public String getPay_man() {
		return pay_man;
	}

	public void setPay_man(String pay_man) {
		this.pay_man = pay_man;
	}

	public Long getAccount_id() {
		return account_id;
	}

	public void setAccount_id(Long account_id) {
		this.account_id = account_id;
	}

	public String getOrder_no() {
		return order_no;
	}

	public void setOrder_no(String order_no) {
		this.order_no = order_no;
	}

	public Long getFk() {
		return fk;
	}

	public void setFk(Long fk) {
		this.fk = fk;
	}
}
