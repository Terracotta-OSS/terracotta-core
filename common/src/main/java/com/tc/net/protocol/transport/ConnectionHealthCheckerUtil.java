/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class ConnectionHealthCheckerUtil {

  public static long getMaxIdleTimeForAlive(HealthCheckerConfig config, boolean considerPeerNodeLongGC) {
    // XXX should we check for disabled hc config ?
    if (considerPeerNodeLongGC) {
      return (config.getPingIdleTimeMillis() + (config.getPingIntervalMillis() * ((config.getSocketConnectTimeout() * config
          .getPingProbes()) * config.getSocketConnectMaxCount())));
    } else {
      return (config.getPingIdleTimeMillis() + (config.getPingIntervalMillis() * config.getPingProbes()));
    }

  }
}
