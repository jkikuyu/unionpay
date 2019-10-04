package com.ipayafrica.unionpay.webapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;

import com.ipayafrica.unionpay.model.Order;
import com.ipayafrica.unionpay.util.AcpService;
import com.ipayafrica.unionpay.util.LogUtil;
import com.ipayafrica.unionpay.util.SDKConfig;

public class PreauthorizationRequest extends BaseAPIController{
		@Autowired
		private Environment env;
		public static String frontUrl = SDKConfig.getConfig().getFrontUrl();
		public static String backUrl = SDKConfig.getConfig().getBackUrl();   			//At the url of the transaction request, you can read the acpsdk.backTransUrl in the corresponding property file acp_sdk.properties from the configuration file.

		public String getPreauth(@RequestBody Order order) {

			/**
			 * Initialize the parameters related to requesting UnionPay access address as well as obtaining certificate files and certificate paths to the SDKConfig class.
			 * Be sure to load such parameters each time you run them in manner of java main.
			 * In case of Web application development, this method can be written into cache by means of monitoring, and does not need to appear here.
			 */
			//Here, the method for loading a property file has been moved to web/AutoLoadServlet.java.
			//SDKConfig.getConfig().loadPropertiesFromSrc(); //Load the acp_sdk.properties file from classpath
			

			Map<String, String> contentData = new HashMap<String, String>();
			
			/***For an all-channel UnionPay system, all parameters can be left unchanged except the one encoding, which you need to set as required.***/
			String origQryId = env.getProperty("origQryId");
			String encoding = contentData.get("encoding");
			String txnType = env.getProperty("preauth.txn.type");
			String txnSubType =   env.getProperty("purchase.txn.subtype");

			contentData.put("encoding", encoding); 			  //M
			contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //M
			//01: Pre-authorization
			//02: MOTO pre-authorization
			contentData.put("txnType", txnType);               			  //M
			contentData.put("txnSubType", txnSubType);            			  //M
			
			contentData.put("orderId",order.getOrderId());             //M client order number, consisting of 8-40 alphanumeric characters, no “-” or “_” is allowed, but custom rules are allowed		
			contentData.put("txnTime", order.getTxnTime());        //M order delivery time: It must be system time in format of YYYYMMDDhhmmss. Be sure to use the current time. Otherwise, an error of invalid txnTime will be reported.
			contentData.put("txnAmt", order.getTxnAmount());             			      //M transaction amount: in cents, without any decimal point.

			contentData.put("frontUrl", frontUrl);   //C
			

			contentData.put("backUrl", backUrl);//M
			

			contentData.put("payTimeout", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date().getTime() + 15 * 60 * 1000));
		
			
			
			/**Sign the requested parameters, and send http.post requests and receive synchronous response messages.**/
			Map<String, String> reqData = AcpService.sign(contentData,encoding);			//In a message, the values of certId and signature are obtained from the signData method and are assigned with values automatically. Therefore, you just need to ensure that the certificate is correctly configured.
			String requestBackUrl = SDKConfig.getConfig().getBackRequestUrl();   			//At the url of the transaction request, you can read the acpsdk.backTransUrl in the corresponding property file acp_sdk.properties from the configuration file.
			Map<String, String> rspData = AcpService.post(reqData,requestBackUrl,encoding); //Send request messages and receive synchronous responses (the default connection timeout is 30s and the timeout for reading the returned result is 30s); Here, after calling signData, do not make any modification to the value of any key in submitFromData before calling submitUrl. Any such modification may cause failure to signature authentication.
			
			/**To proceed the response codes, you need to compile a program based on your business logics. The logics for response code processing below are for reference only.------------->**/
			//For response code specifications, refer to Part 5 “Appendix” in Specifications for Platform Access Interfaces- by selecting Help Center > Download > Product Interface Specifications at open.unionpay.com.
			StringBuffer parseStr = new StringBuffer("");
			if(!rspData.isEmpty()){
				if(AcpService.validate(rspData, encoding)){
					LogUtil.writeLog("Signature authentication succeeds");
					String respCode = rspData.get("respCode") ;
					if(("00").equals(respCode)){
						//Transaction accepted (this does not mean that the transaction is successful). When the transaction is in this state, you can choose to update the order state after receiving the background notification, or actively initiate a transaction query to determine the transaction state.
						//TODO
						//If you have configured encryption for sensitive information, you can use the method below to decrypt the card number to obtain the plain text card number.
//							String accNo1 = rspData.get("accNo");
//							String accNo2 = AcpService.decryptData(accNo1, "UTF-8");  //To decrypt a card number, you need to use the private key certificate acpsdk.signCert.path signed by the client.
//							LogUtil.writeLog("Decrypted card number:"+accNo2);
//							parseStr.append("Decrypted card number:"+accNo2);
					}else if(("03").equals(respCode)||
							 ("04").equals(respCode)||
							 ("05").equals(respCode)){
						//Also, you can initiate a transaction state query later to determine the transaction state.
						//TODO
					}else{
						//Other response codes are failure. Please find the cause.
						//TODO
					}
				}else{
					LogUtil.writeErrorLog("Signature authentication fails");
					//TODO Find the reason why the signature authentication fails
				}
			}else{
				//The returned http state code is incorrect.
				LogUtil.writeErrorLog("No returned message is obtained or the returned http state code is not 200.");
			}
			String rspMessage = genJsonResult(rspData);
			return rspMessage;
		}

	}


