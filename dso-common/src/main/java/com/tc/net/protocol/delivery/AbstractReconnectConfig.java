/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.ReconnectConfig;

public class AbstractReconnectConfig implements ReconnectConfig {

  private final String          name;
  private final boolean         reconnectEnabled;
  private final int             reconnectTimeout;
  private final int             reconnectSendQueueCap;
  private final int             reconnectMaxDelayedAcks;
  private final int             reconnectSendWindow;
  private static final TCLogger logger = TCLogging.getLogger(AbstractReconnectConfig.class);

  public AbstractReconnectConfig(boolean reconnectEnabled, int reconnectTimeout, int reconnectSendQueueCap,
                                 int reconnectMaxDelayedAcks, int reconnectSendWindow, String name) {
    this.name = name;
    this.reconnectEnabled = reconnectEnabled;
    this.reconnectTimeout = reconnectTimeout;
    this.reconnectSendQueueCap = reconnectSendQueueCap;
    this.reconnectMaxDelayedAcks = reconnectMaxDelayedAcks;
    this.reconnectSendWindow = (reconnectSendWindow > 0 ? reconnectSendWindow : 0);
    validateConfig();
  }

  private void validateConfig() {

    if (reconnectMaxDelayedAcks <= 0) { throw new TCRuntimeException(
                                                                     name
                                                                         + " reconnectMaxDelayedAcks should be greater than 0"); }

    if (reconnectSendWindow <= 0) {
      logger.warn(name + " reconnectSendWindow is 0; Message Sender might not throttle for peer node respoonse");
    }

    if (reconnectMaxDelayedAcks >= reconnectSendWindow) { throw new TCRuntimeException(
                                                                                       name
                                                                                           + " : reconnectMaxDelayedAcks should be lesser than reconnectSendWindow"); }
  }

  public boolean getReconnectEnabled() {
    return reconnectEnabled;
  }

  public int getReconnectTimeout() {
    return reconnectTimeout;
  }

  public int getSendQueueCapacity() {
    return reconnectSendQueueCap;
  }

  public int getMaxDelayAcks() {
    return reconnectMaxDelayedAcks;
  }

  public int getSendWindow() {
    return reconnectSendWindow;
  }

}
