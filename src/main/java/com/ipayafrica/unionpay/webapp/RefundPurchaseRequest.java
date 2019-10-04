package com.ipayafrica.unionpay.webapp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;

import com.ipayafrica.unionpay.model.Refund;
import com.ipayafrica.unionpay.util.AcpService;
import com.ipayafrica.unionpay.util.LogUtil;
import com.ipayafrica.unionpay.util.SDKConfig;

public class RefundPurchaseRequest extends BaseAPIController{
	@Autowired
	private Environment env;

	protected String getCancelPurchase(@RequestBody Refund refund){
		
		Map<String, String> contentData = new HashMap<String, String>();

		putData(contentData);
		String origQryId = env.getProperty("origQryId");
		String encoding = contentData.get("encoding");
		String txnSubType =   env.getProperty("cancel.txn.subtype");
		String txnType = env.getProperty("refund.txn.type");
		/***For an all-channel UnionPay system, all parameters can be left unchanged except the one encoding, which you need to set as required.***/
		contentData.put("txnSubType", txnSubType);// M

		
		/***To debug a transaction so that it runs properly, you must modify the fields below.***/
		contentData.put("origQryId", origQryId);      //M****: queryId returned by the original purchase transaction, which can be obtained from the background notification interface or transaction state query interface for purchase transaction.
		
		/**All request parameters have been set successfully. Now, let's sign the requested parameters, and send http.post requests and receive synchronous response messages.------------->**/

		Map<String, String> reqData  = AcpService.sign(contentData,encoding);			//In a message, the values of certId and signature are obtained from the signData method and are assigned with values automatically. Therefore, you just need to ensure that the certificate is correctly configured.
		String url = SDKConfig.getConfig().getBackRequestUrl();									 	//At the url of the transaction request, you can read the acpsdk.backTransUrl in the corresponding property file acp_sdk.properties from the configuration file.
		Map<String, String> rspData = AcpService.post(reqData, url,encoding);	//Here, after calling signData, do not make any modification to the value of any key in submitFromData before calling submitUrl. Any such modification may cause failure to signature authentication.
		
		/**To proceed the response codes, you need to compile a program based on your business logics. The logics for response code processing below are for reference only.------------->**/
		
		//For response code specifications, refer to Part 5 “Appendix” in Specifications for Platform Access Interfaces- by selecting Help Center > Download > Product Interface Specifications at open.unionpay.com.
		if(!rspData.isEmpty()){
			if(AcpService.validate(rspData,encoding)){
				LogUtil.writeLog("Signature authentication succeeds");
				String respCode = rspData.get("respCode") ;
				if(("00").equals(respCode)){
					//Transaction accepted (this does not mean that the transaction is successful). When the transaction is in this state, you can choose to update the order state after receiving the background notification, or actively initiate a transaction query to determine the transaction state.
					//TODO
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
