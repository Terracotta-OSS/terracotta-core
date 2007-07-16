/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.domain;

import java.util.HashSet;
import java.util.Set;

public class Person {

  private Long   id;
  private int    age;
  private String firstname;
  private String lastname;
  private Set    events         = new HashSet();
  private Set    emailAddresses = new HashSet();
  private Set    phoneNumbers   = new HashSet();

  public Person() {
    // 
  }
  
  protected Set getEvents() {
    return events;
  }

  protected void setEvents(Set events) {
    this.events = events;
  }

  public void addToEvent(Event event) {
    this.getEvents().add(event);
    event.getParticipants().add(this);
  }

  public void removeFromEvent(Event event) {
    this.getEvents().remove(event);
    event.getParticipants().remove(this);
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public Long getId() {
    return id;
  }

  private void setId(Long id) {
    this.id = id;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public Set getEmailAddresses() {
    return emailAddresses;
  }

  public void setEmailAddresses(Set emailAddresses) {
    this.emailAddresses = emailAddresses;
  }

  public Set getPhoneNumbers() {
    return phoneNumbers;
  }
  
  public void setPhoneNumbers(Set phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }
  
  public String toString() {
    return getFirstname() + " " + getLastname();
  }
}
