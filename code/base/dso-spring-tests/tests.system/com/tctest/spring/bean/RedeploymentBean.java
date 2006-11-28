/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;


public interface RedeploymentBean {

  void setValue(int value);
  
  int getValue();
  
  boolean hasResource(String resource);
  
}
