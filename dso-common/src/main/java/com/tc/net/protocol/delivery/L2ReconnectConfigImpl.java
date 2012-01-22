/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2ReconnectConfigImpl extends AbstractReconnectConfig {

  private static final String NAME = "L2->L2 Reconnect Config";

  public L2ReconnectConfigImpl() {
    super(TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_SENDQUEUE_CAP),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_MAX_DELAYEDACKS),
          TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_SEND_WINDOW), NAME);
  }

}
