/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class BarServiceHelper {

  private FooService service;

  public BarServiceHelper(FooService service) {
    this.service = service;
  }
  
  public String serviceMethod() {
    return "barAnd" + service.serviceMethod();
  }
  
}
