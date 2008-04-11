/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l1propertiesfroml2;

public class L1ReconnectConfigImpl implements ReconnectConfig {
  public final static String L2_L1RECONNECT_ENABLED = "l2.l1reconnect.enabled";
  public final static String L2_L1RECONNECT_TIMEOUT = "l2.l1reconnect.timeout.millis";
  
  private boolean l1ReconnectEnabled;
  private int     l1ReconnectTimeout;
  
  public L1ReconnectConfigImpl() {
    l1ReconnectEnabled = false;
    l1ReconnectTimeout = 0;
  }
  
  public L1ReconnectConfigImpl(boolean l1ReconnectEnabled, int l1ReconnectTimeout){
    this.l1ReconnectEnabled = l1ReconnectEnabled;
    this.l1ReconnectTimeout = l1ReconnectTimeout;
  }

  public int getReconnectTimeout() {
    return l1ReconnectTimeout;
  }

  public boolean getReconnectEnabled() {
    return l1ReconnectEnabled;
  }

}