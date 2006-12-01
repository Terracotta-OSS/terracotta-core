/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.InitializingBean;

import com.tc.aspectwerkz.proxy.Uuid;

public class SimpleInitializingSingleton implements InitializingBean, ISimpleInitializingSingleton {
  public static final String ME = "me";
  private transient long id = System.identityHashCode(this) + Uuid.newUuid();

  public static ISimpleInitializingSingleton afterPropertiesSetThis;
  private String name;

  public void afterPropertiesSet() throws Exception {
    afterPropertiesSetThis = this;
    this.name = ME;
  }

  public String getName() {
    return name;
  }
  
  public long getId() {
    return id;
  }
  
  public long getInnerId() {
    return afterPropertiesSetThis.getId();
  }
  
  public boolean isTheSameInstance() {
    return afterPropertiesSetThis == this;
  }
}
