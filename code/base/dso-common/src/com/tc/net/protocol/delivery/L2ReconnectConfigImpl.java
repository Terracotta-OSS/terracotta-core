/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2ReconnectConfigImpl implements ReconnectConfig {
  private boolean l2ReconnectEnabled;
  private int     l2ReconnectTimeout;
  private int     l2ReconnectSendQueueCap;

  public L2ReconnectConfigImpl() {
    l2ReconnectEnabled = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED);
    l2ReconnectTimeout = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT);
    l2ReconnectSendQueueCap = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_SENDQUEUE_CAP);
  }

  public int getReconnectTimeout() {
    return l2ReconnectTimeout;
  }

  public boolean getReconnectEnabled() {
    return l2ReconnectEnabled;
  }

  public int getSendQueueCapacity() {
    return l2ReconnectSendQueueCap;
  }

}
