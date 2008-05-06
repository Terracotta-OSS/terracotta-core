/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import java.util.List;

public interface ClientSpec {

  /**
   * The name of the host to run the client(s) on
   */
  public String getHostName();

  /**
   * The location of the test sandbox
   */
  public String getTestHome();

  /**
   * How many JVMs to start on this host.
   */
  public int getVMCount();

  /**
   * How many instances of the test per JVM.
   */
  public int getExecutionCount();

  public List getJvmOpts();

  public ClientSpec copy();
}
