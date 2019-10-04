package com.ipayafrica.unionpay.webapp;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class BaseAPIController {
	@Autowired
	private Environment env;

	public BaseAPIController() {
		
	}
	protected void putData(Map<String, String> contentData) {
		String merId = env.getProperty("merchant.id");
		String version = env.getProperty("version");
		String encoding = env.getProperty("encoding");
		String signMethod = env.getProperty("sign.method");
		String bizType = env.getProperty("biz.type");
		String accessType = env.getProperty("access.type");
		String channelType = env.getProperty("channel.type");
		String backUrl =  env.getProperty("back.url");
		String currencyCode = env.getProperty("currency.code");

		/***For an all-channel UnionPay system, all parameters can be left unchanged except the one encoding, which you need to set as required.***/
		contentData.put("version", version);  
		contentData.put("encoding", encoding);  
	
		// Value: 01 (RSA)
		contentData.put("signMethod",signMethod); //M
                       //M
		// 01: Purchase, to differentiate the front-end purchase or back-end purchase through transaction request URL
		// 02: MOTO
		//05: Purchase with authentication (Applied to Product type 000301)
		//000301: Merchant-hosted
		//000000: ExpressPay
		//000902: Token payment
		//001001: MOTO
		contentData.put("bizType", bizType);         //M      	
		// 0: Merchant direct access
		// 1: Acquirer Access
		// 2: Platform merchant access
		contentData.put("accessType", accessType);// M
		// 07: Internet
		// 08: Mobile
		contentData.put("channelType", channelType);// M

		// Merchant ID
		contentData.put("merId", merId);                   			  //M
		contentData.put("backUrl", backUrl);
		contentData.put("currencyCode", currencyCode);			

     
	}
	public String genJsonResult(Map<String, String> data) {
		return null;
		
	}

}
