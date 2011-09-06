/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.distrunner;

import java.util.ArrayList;
import java.util.List;

public class NullServerSpec implements ServerSpec {

  private static final int UNDEFINED = -1;

  public boolean isNull() {
    return true;
  }

  public String getHostName() {
    return "";
  }

  public String getTestHome() {
    return "";
  }

  public String toString() {
    return "null";
  }

  public List getJvmOpts() {
    return new ArrayList();
  }

  public int getCache() {
    return UNDEFINED;
  }

  public int getJmxPort() {
    return UNDEFINED;
  }

  public int getDsoPort() {
    return UNDEFINED;
  }

  public ServerSpec copy() {
    return this;
  }

  public int getType() {
    return UNDEFINED;
  }

}
