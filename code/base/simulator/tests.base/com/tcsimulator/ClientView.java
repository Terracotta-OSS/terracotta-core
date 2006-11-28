/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
