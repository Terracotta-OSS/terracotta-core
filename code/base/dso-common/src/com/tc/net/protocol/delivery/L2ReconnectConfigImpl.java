/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.l1propertiesfroml2.ReconnectConfig;
import com.tc.properties.TCPropertiesImpl;

public class L2ReconnectConfigImpl implements ReconnectConfig {
  public static final String L2_RECONNECT_ENABLE        = "l2.nha.tcgroupcomm.reconnect.enabled";

  public static final String L2_RECONNECT_TIMEOUT       = "l2.nha.tcgroupcomm.reconnect.timeout";

  // a hidden tc.properties only used for l2 proxy testing purpose
  public static final String L2_RECONNECT_PROXY_TO_PORT = "l2.nha.tcgroupcomm.l2proxytoport";

  private boolean            l2ReconnectEnabled;
  private int                l2ReconnectTimeout;

  public L2ReconnectConfigImpl() {
    l2ReconnectEnabled = TCPropertiesImpl.getProperties().getBoolean(L2_RECONNECT_ENABLE);
    l2ReconnectTimeout = TCPropertiesImpl.getProperties().getInt(L2_RECONNECT_TIMEOUT);
  }

  public int getReconnectTimeout() {
    return l2ReconnectTimeout;
  }

  public boolean getReconnectEnabled() {
    return l2ReconnectEnabled;
  }

}