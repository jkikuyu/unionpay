package com.ipayafrica.unionpay.webapp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ipayafrica.unionpay.model.Purchase;
import com.ipayafrica.unionpay.util.AcpService;
import com.ipayafrica.unionpay.util.LogUtil;
import com.ipayafrica.unionpay.util.SDKConfig;
@RestController
public class PurchaseRequest extends BaseAPIController{
	@Autowired
	private Environment env;

	@RequestMapping(value ="/purchase", method=RequestMethod.POST, produces={"application/json"})
	@ResponseBody

	public String getPurchaseRequest(@RequestBody Purchase purchase) {
		Map<String, String> contentData = new HashMap<String, String>();
		String encoding = env.getProperty("encoding");
		putData(contentData);
		String txnType= env.getProperty("purchase.txn.type");
		String txnSubType = env.getProperty("purchase.txn.subtype");

		// Merchant order No
		// Value: 01
		contentData.put("orderId", purchase.getOrderId());   

		//M client order number, consisting of 8-40 alphanumeric characters, no “-” or “_” is allowed, but custom rules are allowed	
		// Date and time when merchant sends transaction
		   //M order delivery time: It must be in format of YYYYMMDDhhmmss. Be sure to use the current time. Otherwise, an error of invalid txnTime will be reported.

		contentData.put("txnTime", purchase.getTxnTime());
		String smsCode = env.getProperty("sms.code");
		contentData.put("txnType", txnType);       

		// Default value is 156.
		String cardNumber = purchase.getCardNumber();
		//M transaction currency (for domestic clients, it is usually 156, which indicates RMB)
		// The unit of transaction amount is cent.
		contentData.put("txnAmt", purchase.getTxnAmount());							   //M transaction amount: in cents, without any decimal point.
	
		//Consumption: The transaction element card number and authentication code depend on the service configuration (by default, an SMS authentication code is required).
		Map<String,String> customerInfoMap = new HashMap<String,String>();
		customerInfoMap.put("smsCode", smsCode);			    	//SMS authentication code: You will not actually receive an SMS in the test environment. Therefore, always fill in 111111 here.
		
		////////////If the client has enabled the right [encrypt sensitive information by the client], you need to encrypt accNo, pin, phoneNo, cvn2, and expired (if these fields will be sent later) for encryption of sensitive information.
		String accNo = AcpService.encryptData(cardNumber, encoding);  //A test card number is used here because it is in test environment. In normal environment, please use a real card number instead.

		contentData.put("accNo", accNo);
		contentData.put("encryptCertId",AcpService.getEncryptCertId());       //certId of the encryption certificate, which is configured under the acpsdk.encryptCert.path property of the acp_sdk.properties file.
		String customerInfoStr = AcpService.getCustomerInfoWithEncrypt(customerInfoMap,cardNumber, encoding);

		contentData.put("customerInfo", customerInfoStr);//M
		
		contentData.put("payTimeout", "");// O
		

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
		String reqMessage = genJsonResult(reqData);
		String rspMessage = genJsonResult(rspData);
		return rspMessage;
		
	}


}