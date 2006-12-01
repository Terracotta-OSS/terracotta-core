/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
