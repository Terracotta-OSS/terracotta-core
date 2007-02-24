package com.tctest.domain;

public class Account {

	private int id;

	private String number;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String toString() {
		return "account.id: " + id + ", account.number: " + number;
	}
}
