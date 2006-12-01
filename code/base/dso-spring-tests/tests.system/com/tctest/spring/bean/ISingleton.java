/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.spring.bean;

public interface ISingleton {

  public int getCounter();

  public void incrementCounter();

  public String getTransientValue();

  public void setTransientValue(String transientValue);

  public boolean toggleBoolean();

}
