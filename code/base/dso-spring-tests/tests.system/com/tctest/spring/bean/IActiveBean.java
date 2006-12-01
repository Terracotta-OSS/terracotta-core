/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public interface IActiveBean {

  void start();
  void stop();
  
  void setValue(String value);
  String getValue();
  
  boolean isActive();
}
