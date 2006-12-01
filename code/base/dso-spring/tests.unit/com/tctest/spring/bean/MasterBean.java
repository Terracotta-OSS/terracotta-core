/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.ArrayList;
import java.util.List;


public class MasterBean {

  private Singleton singleton;
  private List values = new ArrayList();
  
  public Singleton getSingleton() {
    return singleton;
  }
  
  public void setSingleton(Singleton singleton) {
    this.singleton = singleton;
  }
  
  public void addValue(String value) {
    synchronized (this) {
      this.values.add(value);
    }
  }

  public List getValues() {
    return values;
  }
  
}

