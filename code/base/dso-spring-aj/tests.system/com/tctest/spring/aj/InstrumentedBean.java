/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aj;

import org.springframework.beans.factory.InitializingBean;


public class InstrumentedBean implements InitializingBean, IInstrumentedBean {

  private ConfigurableBean configurableBean;

  private String value;

  private transient String transientValue = "aaa";

  
  public void afterPropertiesSet() throws Exception {
    this.configurableBean = new ConfigurableBean();
  }
  
  public String getProperty1() {
    synchronized(this) {
      return this.configurableBean.getProperty1();
    }
  }

  public String getProperty2() {
    synchronized(this) {
      return this.configurableBean.getProperty2();
    }
  }
  
  public void setValue(String value) {
    synchronized(this) {
      this.value = value;
    }
  }

  public Object getValue() {
    synchronized(this) {
      return value;
    }
  }

  public Object getTransientValue() {
    return transientValue;
  }
  
  public void setTransientValue(String transientValue) {
    this.transientValue = transientValue;
  }

}

