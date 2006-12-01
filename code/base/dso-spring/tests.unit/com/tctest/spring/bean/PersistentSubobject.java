/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class PersistentSubobject {

	public static final String NOT_SENT = "NotSent";

	public static final String QUEUED = "Queued";

	private static final String PENDING = "Pending";

	public static final String SENT = "Sent";

	public static final String FAILED = "Failed";

	public static final String DELIVERED = "Delivered";

	private int messageId;

	private String statusCode;

	private String smscMessageId;

	PersistentSubobject() {
	}

	public PersistentSubobject(String code) {
		statusCode = code;
	}

	public PersistentSubobject(int messageId, String statusCode, String smscMessageId) {
		this.messageId = messageId;
		this.statusCode = statusCode;
		this.smscMessageId = smscMessageId;
	}

	public int getMessageId() {
		return messageId;
	}

	public void noteQueued() {
		this.statusCode = QUEUED;
	}

	public boolean isSentOrDelivered() {
		return statusCode.equals(SENT) || statusCode.equals(DELIVERED);
	}

	public String getSMSCMessageId() {
		return smscMessageId;
	}

	public String getStatusCode() {
		return statusCode;
	}

}
