/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.domain;

import com.tctest.domain.Person;


public class Account {

  private Long   id;
  private Person person;

  public Account() {
    // 
  }
  
  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

  public Long getId() {
    return id;
  }

  private void setId(Long id) {
    this.id = id;
  }

  public String toString() {
    return super.toString();
  }
}
