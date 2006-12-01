/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;

public class MasterBean implements IMasterBean, InitializingBean {

  private List sharedSingletons = new ArrayList();
  private ISingleton singleton;
  private List values = new ArrayList();
  
  public void afterPropertiesSet() {
    sharedSingletons.add(singleton);
  }
  
  public ISingleton getSingleton() {
    return singleton;
  }
  
  public void setSingleton(ISingleton singleton) {
    this.singleton = singleton;
  }
  
  public void addValue(String value) {
    synchronized (this) {
      this.values.add(value);
    }
  }

  public List getValues() {
    synchronized(this) {
      return values;
    }
  }
  
  /**
   * Verify 
   * 1. If the 2nd Node uses the shared singleton instead of create another copy 
   * 2. If the a roundtrip of the shared object retains the semantics for ==
   */
  public boolean isTheSameSingletonReferenceUsed() {
    // assume 2 nodes; otherwise let them throw exception
    return sharedSingletons.get(0) == sharedSingletons.get(1);
  }
}

