/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aj;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class ConfigurableBean implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private String property1;
  private String property2;
  
  public ConfigurableBean() {
  }
  
  public String getProperty1() {
    return this.property1;
  }
  
  public String getProperty2() {
    return this.property2;
  }
  
  public void setProperty1(String property1) {
    this.property1 = property1;
  }
  
  public void setProperty2(String property2) {
    this.property2 = property2;
  }
  
}
