/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class PersistentObject {

	private int id;

	private String phoneNumber;

	private String message;

	private PersistentSubobject status = new PersistentSubobject(PersistentSubobject.NOT_SENT);

	public PersistentObject() {
	}

	public PersistentObject(int id, String phoneNumber, String message) {
		this.id = id;
		this.phoneNumber = phoneNumber;
		this.message = message;
	}

	public PersistentObject(String phoneNumber, String message) {
		this.phoneNumber = phoneNumber;
		this.message = message;
	}

	public int getMessageId() {
		return id;
	}

	public String getMessage() {
		return message;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void noteMessageStatus(PersistentSubobject messageStatus) {
		this.status = messageStatus;
	}

	public void noteQueued() {
		status.noteQueued();
	}

	public boolean isSentOrDelivered() {
		return status.isSentOrDelivered();
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public String getStatusCode() {
		return status.getStatusCode();
	}

}
