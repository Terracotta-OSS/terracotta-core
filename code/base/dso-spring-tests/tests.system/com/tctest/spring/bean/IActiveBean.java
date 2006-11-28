/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public interface IActiveBean {

  void start();
  void stop();
  
  void setValue(String value);
  String getValue();
  
  boolean isActive();
}
