/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.factory.BeanNameAware;

public class BeanNameAwareBean implements BeanNameAware{

  private String name;

  public void setBeanName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
  

}
