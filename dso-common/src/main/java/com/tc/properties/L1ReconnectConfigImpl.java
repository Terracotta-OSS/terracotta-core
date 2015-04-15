/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.properties;

import com.tc.net.protocol.delivery.AbstractReconnectConfig;

public class L1ReconnectConfigImpl extends AbstractReconnectConfig {

  private static final String NAME = "L2->L1 Reconnect Config";

  public L1ReconnectConfigImpl() {
    super(TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED), TCPropertiesImpl
        .getProperties().getInt(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS), TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_L1RECONNECT_SENDQUEUE_CAP), TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_L1RECONNECT_MAX_DELAYEDACKS), TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_L1RECONNECT_SEND_WINDOW), NAME);
  }

  public L1ReconnectConfigImpl(boolean l1ReconnectEnabled, int l1ReconnectTimeout, int l1ReconnectSendQueueCap,
                               int l1ReconnectMaxDelayedAcks, int l1ReconnectSendWindow) {
    super(l1ReconnectEnabled, l1ReconnectTimeout, l1ReconnectSendQueueCap, l1ReconnectMaxDelayedAcks,
          l1ReconnectSendWindow, NAME);
  }

}
