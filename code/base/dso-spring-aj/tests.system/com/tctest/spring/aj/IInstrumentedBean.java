/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.aj;

public interface IInstrumentedBean {

  public String getProperty1();
  public String getProperty2();

  public void setValue(String value);
  public Object getValue();

  public void setTransientValue(String transientValue);
  public Object getTransientValue();


}
