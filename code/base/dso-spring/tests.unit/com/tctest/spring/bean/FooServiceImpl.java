/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public class FooServiceImpl implements FooService {

  public FooServiceImpl() {
  }
  
  public String serviceMethod() {
    return "rawValue";

  }

}
