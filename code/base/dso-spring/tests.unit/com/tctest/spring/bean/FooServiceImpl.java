/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class FooServiceImpl implements FooService {

  public FooServiceImpl() {
    //
  }
  
  public String serviceMethod() {
    return "rawValue";

  }

}
