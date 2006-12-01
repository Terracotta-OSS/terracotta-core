/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring.beans.orm.domain;

import java.util.Set;

public class Customer {

  private int customerId;
  private String firstName;
  private String lastName;
  private Set addresses;
  private Set permissions;

  public String toString() {
    StringBuffer result = new StringBuffer(250);
    result.append("Customer { customerId=");
    result.append(customerId);
    result.append(", firstName=");
    result.append(firstName);
    result.append(", lastName=");
    result.append(lastName);
    result.append(", addresses=");
    result.append(addresses);
    result.append(", permissions=");
    result.append(permissions);
    result.append(" }");
    return result.toString();
  }

  public Set getAddresses() {
    return addresses;
  }

  public void setAddresses(Set addresses) {
    this.addresses = addresses;
  }

  public int getCustomerId() {
    return customerId;
  }

  public void setCustomerId(int customerId) {
    this.customerId = customerId;
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

  public Set getPermissions() {
    return permissions;
  }

  public void setPermissions(Set permissions) {
    this.permissions = permissions;
  }
}
