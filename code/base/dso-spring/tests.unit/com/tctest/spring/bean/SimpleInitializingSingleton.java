/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.InitializingBean;

public class SimpleInitializingSingleton implements InitializingBean {

  public static SimpleInitializingSingleton afterPropertiesSetThis;
  private String name;

  public void afterPropertiesSet() throws Exception {
    afterPropertiesSetThis = this;
    this.name = "me";
  }

  public String getName() {
    return name;
  }

}
