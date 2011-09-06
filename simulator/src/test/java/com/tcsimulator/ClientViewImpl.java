/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;


import java.util.List;

public class ClientViewImpl implements ClientView {

  private final ClientSpec spec;

  public ClientViewImpl(ClientSpec cSpec) {
    spec = cSpec;
  }

  public String getHostName() {
    return this.spec.getHostName();
  }

  public String getTestHome() {
    return this.spec.getTestHome();
  }

  public int getVMCount() {
    return this.spec.getVMCount();
  }

  public int getExecutionCount() {
    return this.spec.getExecutionCount();
  }

  public List getJvmOpts() {
    return this.spec.getJvmOpts();
  }

  public ClientView copy() {
    return new ClientViewImpl(this.spec.copy());
  }

}
