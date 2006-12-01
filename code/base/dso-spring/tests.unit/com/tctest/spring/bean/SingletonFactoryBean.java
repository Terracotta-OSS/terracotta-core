/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.FactoryBean;

public class SingletonFactoryBean implements FactoryBean {

  private Object singleton;

  public Object getObject() throws Exception {
    return singleton;
  }

  public Class getObjectType() {
    return Singleton.class;
  }

  public boolean isSingleton() {
    return true;
  }
  
  public void setObject(Object singleton) {
    this.singleton = singleton;
  }

}

