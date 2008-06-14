/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.properties;

public class L1ReconnectConfigImpl implements ReconnectConfig {

  private boolean l1ReconnectEnabled;
  private int     l1ReconnectTimeout;
  private int     l1ReconnectSendQueueCap;
  private int     l1ReconnectMaxDelayedAcks;
  private int     l1ReconnectSendWindow;

  public L1ReconnectConfigImpl() {
    l1ReconnectEnabled = TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED);
    l1ReconnectTimeout = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS);
    l1ReconnectSendQueueCap = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_L1RECONNECT_SENDQUEUE_CAP);
    l1ReconnectMaxDelayedAcks = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_L1RECONNECT_MAX_DELAYEDACKS);
    l1ReconnectSendWindow = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_L1RECONNECT_SEND_WINDOW);
  }

  public L1ReconnectConfigImpl(boolean l1ReconnectEnabled, int l1ReconnectTimeout, int l1ReconnectSendQueueCap,
                               int l1ReconnectMaxDelayedAcks, int l1ReconnectSendWindow) {
    this.l1ReconnectEnabled = l1ReconnectEnabled;
    this.l1ReconnectTimeout = l1ReconnectTimeout;
    this.l1ReconnectSendQueueCap = l1ReconnectSendQueueCap;
    this.l1ReconnectMaxDelayedAcks = l1ReconnectMaxDelayedAcks;
    this.l1ReconnectSendWindow = l1ReconnectSendWindow;
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

  public int getMaxDelayAcks() {
    return l1ReconnectMaxDelayedAcks;
  }

  public int getSendWindow() {
    return l1ReconnectSendWindow;
  }

}