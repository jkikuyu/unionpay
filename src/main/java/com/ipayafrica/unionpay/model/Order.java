package com.ipayafrica.unionpay.model;

import java.util.Calendar;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

@Entity
@Table(name = "Order")
@Inheritance(strategy=InheritanceType.JOINED)
public class Order {
    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "ORDERID")

	private String orderId;
    @Column(name = "TRANSACTTIME", nullable=false)
    @Temporal(TemporalType.TIMESTAMP)

    private Calendar transactTime;
    
    @Column(name = "AMOUNT", nullable=false, precision=10, scale=2)
    
    private Double amount;
    
    @Transient
    private String txnTime;
    
    @Transient
	private String txnAmount;
	
	public String getOrderId() {
		return orderId;
	}

	public String getTxnTime() {
		return txnTime;
	}
	public String getTxnAmount() {
		return txnAmount;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public void setTxnTime(String txnTime) {
		this.txnTime = txnTime;
	}
	public void setTxnAmount(String txnAmount) {
		this.txnAmount = txnAmount;
	}


	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public Calendar getTransactTime() {
		return transactTime;
	}

	public void setTransactTime(Calendar transactTime) {
		this.transactTime = transactTime;
	}
	
	
}
