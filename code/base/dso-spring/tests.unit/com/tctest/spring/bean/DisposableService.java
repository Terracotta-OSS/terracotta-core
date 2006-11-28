/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class DisposableService implements InitializingBean, DisposableBean {

  private transient String name = "disposable";

  private transient String foo;

  public transient static DisposableService afterPropertiesSetThis;
  public transient static DisposableService destroyThis;

  public String getName() {
    return name;
  }

  public String getFoo() {
    return foo;
  }
  
  public void afterPropertiesSet() throws Exception {
    this.foo = "bar";
    afterPropertiesSetThis = this;
    
  }

  public void destroy() throws Exception {
    this.name = null;
    destroyThis = this;
    if (foo == null) throw new RuntimeException("foo is null");
  }

}
