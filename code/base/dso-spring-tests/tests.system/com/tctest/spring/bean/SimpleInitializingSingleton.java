/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.InitializingBean;

import java.util.UUID;

public class SimpleInitializingSingleton implements InitializingBean, ISimpleInitializingSingleton {
  public static final String                 ME = "me";
  private transient UUID                     id = UUID.randomUUID();

  public static ISimpleInitializingSingleton afterPropertiesSetThis;
  private String                             name;

  public void afterPropertiesSet() throws Exception {
    afterPropertiesSetThis = this;
    this.name = ME;
  }

  public String getName() {
    return name;
  }

  public UUID getId() {
    return id;
  }

  public UUID getInnerId() {
    return afterPropertiesSetThis.getId();
  }

  public boolean isTheSameInstance() {
    return afterPropertiesSetThis == this;
  }
}
