package com.ipayafrica.unionpay.model;

public class Order {
	String orderId;
	String txnTime;
	String txnAmount;
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
	
	
}
