/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
