package com.ipayafrica.unionpay.model;

public class Purchase extends Order {
	String cardNumber;
	public String getCardNumber() {
		return cardNumber;
	}
	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

}
