/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring.beans.orm.domain;

public class CustomerAddress {

  private int customerAddressId;
  private int customer;
  private String line1;
  private String line2;
  private String city;
  private String postCode;

  public String toString() {
    StringBuffer result = new StringBuffer(100);
    result.append("CustomerAddress { customerAddressId=");
    result.append(customerAddressId);
    result.append(", line1=");
    result.append(line1);
    result.append(", line2=");
    result.append(line2);
    result.append(", city=");
    result.append(city);
    result.append(", postCode=");
    result.append(postCode);
    result.append(" }");
    return result.toString();
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public int getCustomerAddressId() {
    return customerAddressId;
  }

  public void setCustomerAddressId(int customerAddressId) {
    this.customerAddressId = customerAddressId;
  }

  public String getLine1() {
    return line1;
  }

  public void setLine1(String line1) {
    this.line1 = line1;
  }

  public String getLine2() {
    return line2;
  }

  public void setLine2(String line2) {
    this.line2 = line2;
  }

  public String getPostCode() {
    return postCode;
  }

  public void setPostCode(String postCode) {
    this.postCode = postCode;
  }

  public int getCustomer() {
    return customer;
  }

  public void setCustomer(int customer) {
    this.customer = customer;
  }
}
