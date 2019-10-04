package com.ipayafrica.unionpay.model;

public class Refund extends Order  {
	private String origQryId;

	/**
	 * @return the origQryId
	 */
	public String getOrigQryId() {
		return origQryId;
	}

	/**
	 * @param origQryId the origQryId to set
	 */
	public void setOrigQryId(String origQryId) {
		this.origQryId = origQryId;
	}

}
