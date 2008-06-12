/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

public class L1ReconnectConfigImpl implements ReconnectConfig {

  private boolean l1ReconnectEnabled;
  private int     l1ReconnectTimeout;
  private int     l1ReconnectSendQueueCap;

  public L1ReconnectConfigImpl() {
    l1ReconnectEnabled = false;
    l1ReconnectTimeout = 0;
    l1ReconnectSendQueueCap = 0;
  }

  public L1ReconnectConfigImpl(boolean l1ReconnectEnabled, int l1ReconnectTimeout, int l1SendQueueCap) {
    this.l1ReconnectEnabled = l1ReconnectEnabled;
    this.l1ReconnectTimeout = l1ReconnectTimeout;
    this.l1ReconnectSendQueueCap = l1SendQueueCap;
  }

  public int getReconnectTimeout() {
    return l1ReconnectTimeout;
  }

  public boolean getReconnectEnabled() {
    return l1ReconnectEnabled;
  }

  public int getSendQueueCapacity() {
    return l1ReconnectSendQueueCap;
  }

}