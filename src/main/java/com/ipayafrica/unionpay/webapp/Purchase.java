package com.ipayafrica.unionpay.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ipayafrica.unionpay.util.AcpService;
import com.ipayafrica.unionpay.util.LogUtil;
import com.ipayafrica.unionpay.util.SDKConfig;
@RestController
public class Purchase extends BaseAPIController{
	@RequestMapping(value ="/purchase", method=RequestMethod.POST, produces={"application/json"})
	@ResponseBody

	public String getPurchaseRequest(@RequestBody Order order) {
	
		String merId = req.getParameter("merId");
		merId = "000000070000017";
		String orderId = req.getParameter("orderId");
		String txnTime = req.getParameter("txnTime");
		String txnAmt = req.getParameter("txnAmt");
		Map<String, String> contentData = new HashMap<String, String>();

		/***For an all-channel UnionPay system, all parameters can be left unchanged except the one encoding, which you need to set as required.***/
		// Constant value: 5.1.0
		contentData.put("version", DemoBase.version);                  //M
		// Default value: UTF-8
		contentData.put("encoding", DemoBase.encoding);           //M
		// Value: 01 (RSA)
		contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //M
		// Value: 01
		contentData.put("txnType", "01");                              //M
		// 01: Purchase, to differentiate the front-end purchase or back-end purchase through transaction request URL
		// 02: MOTO
		//05: Purchase with authentication (Applied to Product type 000301)
		contentData.put("txnSubType", "01");                           //Transaction sub-type 01-purchase
		//000301: Merchant-hosted
		//000000: ExpressPay
		//000902: Token payment
		//001001: MOTO
		contentData.put("bizType", "000000");         //M      	
		// 0: Merchant direct access
		// 1: Acquirer Access
		// 2: Platform merchant access
		contentData.put("accessType", "0");// M
		// 07: Internet
		// 08: Mobile
		contentData.put("channelType", "07");// M

		// Merchant ID
		contentData.put("merId", merId);                   			  //M
		// Merchant order No
		contentData.put("orderId", orderId);             			   //M client order number, consisting of 8-40 alphanumeric characters, no “-” or “_” is allowed, but custom rules are allowed	
		// Date and time when merchant sends transaction
		contentData.put("txnTime", txnTime);         				   //M order delivery time: It must be in format of YYYYMMDDhhmmss. Be sure to use the current time. Otherwise, an error of invalid txnTime will be reported.
		// Default value is 156.
		contentData.put("currencyCode", "156");						   //M transaction currency (for domestic clients, it is usually 156, which indicates RMB)
		// The unit of transaction amount is cent.
		contentData.put("txnAmt", txnAmt);							   //M transaction amount: in cents, without any decimal point.
	
		//Consumption: The transaction element card number and authentication code depend on the service configuration (by default, an SMS authentication code is required).
		Map<String,String> customerInfoMap = new HashMap<String,String>();
		customerInfoMap.put("smsCode", "111111");			    	//SMS authentication code: You will not actually receive an SMS in the test environment. Therefore, always fill in 111111 here.
		
		////////////If the client has enabled the right [encrypt sensitive information by the client], you need to encrypt accNo, pin, phoneNo, cvn2, and expired (if these fields will be sent later) for encryption of sensitive information.
		String accNo = AcpService.encryptData("6216261000000000018", DemoBase.encoding);  //A test card number is used here because it is in test environment. In normal environment, please use a real card number instead.

		contentData.put("accNo", accNo);
		contentData.put("encryptCertId",AcpService.getEncryptCertId());       //certId of the encryption certificate, which is configured under the acpsdk.encryptCert.path property of the acp_sdk.properties file.
		String customerInfoStr = AcpService.getCustomerInfoWithEncrypt(customerInfoMap,"6216261000000000018",DemoBase.encoding);

		contentData.put("customerInfo", customerInfoStr);//M
		
		contentData.put("payTimeout", "");// O
		
		contentData.put("backUrl", DemoBase.backUrl);

		/**Sign the requested parameters, and send http.post requests and receive synchronous response messages.**/
		Map<String, String> reqData = AcpService.sign(contentData,DemoBase.encoding);			//In a message, the values of certId and signature are obtained from the signData method and are assigned with values automatically. Therefore, you just need to ensure that the certificate is correctly configured.
		String requestBackUrl = SDKConfig.getConfig().getBackRequestUrl();   			//At the url of the transaction request, you can read the acpsdk.backTransUrl in the corresponding property file acp_sdk.properties from the configuration file.
		Map<String, String> rspData = AcpService.post(reqData,requestBackUrl,DemoBase.encoding); //Send request messages and receive synchronous responses (the default connection timeout is 30s and the timeout for reading the returned result is 30s); Here, after calling signData, do not make any modification to the value of any key in submitFromData before calling submitUrl. Any such modification may cause failure to signature authentication.
		
		/**To proceed the response codes, you need to compile a program based on your business logics. The logics for response code processing below are for reference only.------------->**/
		//For response code specifications, refer to Part 5 “Appendix” in Specifications for Platform Access Interfaces- by selecting Help Center > Download > Product Interface Specifications at open.unionpay.com.
		StringBuffer parseStr = new StringBuffer("");
		if(!rspData.isEmpty()){
			if(AcpService.validate(rspData, DemoBase.encoding)){
				LogUtil.writeLog("Signature authentication succeeds");
				String respCode = rspData.get("respCode") ;
				if(("00").equals(respCode)){
					//Transaction accepted (this does not mean that the transaction is successful). When the transaction is in this state, you can choose to update the order state after receiving the background notification, or actively initiate a transaction query to determine the transaction state.
					//TODO
					//If you have configured encryption for sensitive information, you can use the method below to decrypt the card number to obtain the plain text card number.
					String accNo1 = rspData.get("accNo");
					String accNo2 = AcpService.decryptData(accNo1, "UTF-8");  //To decrypt a card number, you need to use the private key certificate acpsdk.signCert.path signed by the client.
					LogUtil.writeLog("Decrypted card number:"+accNo2);
//					parseStr.append("Decrypted card number:"+accNo2);
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
		String reqMessage = DemoBase.genHtmlResult(reqData);
		String rspMessage = DemoBase.genHtmlResult(rspData);
		resp.getWriter().write("Request message:<br/>"+reqMessage+"<br/>" + "Response message:</br>"+rspMessage+parseStr);
		
	}


}