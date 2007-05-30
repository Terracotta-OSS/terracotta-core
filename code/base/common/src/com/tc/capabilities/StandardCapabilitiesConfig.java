/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.capabilities;

public class StandardCapabilitiesConfig implements CapabilitiesConfig{
  private boolean networkEnabledHA;

  public void setNetworkEnabledHA(boolean val) {
    networkEnabledHA = val;
  }

  public boolean getNetworkEnabledHA() {
    return networkEnabledHA;
  }
}
