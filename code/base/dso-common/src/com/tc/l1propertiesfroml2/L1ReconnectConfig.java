/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l1propertiesfroml2;

public interface L1ReconnectConfig {
  public final String L2_L1RECONNECT_ENABLED = "l2.l1reconnect.enabled";
  public final String L2_L1RECONNECT_TIMEOUT = "l2.l1reconnect.timeout.millis";

  public boolean getReconnectEnabled();

  public int getL1ReconnectTimeout();
}
