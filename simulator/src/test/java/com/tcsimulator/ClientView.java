/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import java.util.List;

public interface ClientView {

  public String getHostName();

  public String getTestHome();

  public int getVMCount();

  public int getExecutionCount();

  public List getJvmOpts();

  public ClientView copy();

}
