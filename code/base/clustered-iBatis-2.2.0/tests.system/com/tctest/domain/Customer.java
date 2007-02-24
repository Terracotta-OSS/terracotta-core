package com.tctest.domain;


public class Customer {

	private int id;

	private String firstName;

	private String lastName;

	private String emailAddress;

	private Account account;
  
  public Customer() {
    super();
  }

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public String toString() {
		return "customer.lastName: " + lastName + ", customer.firstName: "
				+ firstName + ", customer.email: " + emailAddress;
	}
}
