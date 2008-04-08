/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;

/**
 * Main implementation of the Health Checker Config. Health Checker related tc.properties are read and a config data
 * structure is built which is passed on to various health checker modules.
 *
 * @author Manoj
 */
public class HealthCheckerConfigImpl implements HealthCheckerConfig {

  private final boolean    enable;
  private final long        pingIdleTime;
  private final long        pingInterval;
  private final int        pingProbes;
  private final boolean    doSocketConnect;
  private final int        maxSocketConnectCount;
  private final String     name;

  // Default ping probe values in seconds
  private final static int PING_IDLETIME = 45;
  private final static int PING_INTERVAL = 15;
  private final static int PING_PROBECNT = 3;

  public HealthCheckerConfigImpl(TCProperties healthCheckerProperties, String hcName) {
    this.pingIdleTime = healthCheckerProperties.getLong("ping.idletime");
    this.pingInterval = healthCheckerProperties.getLong("ping.interval");
    this.pingProbes = healthCheckerProperties.getInt("ping.probes");
    this.name = hcName;
    this.doSocketConnect = healthCheckerProperties.getBoolean("socketConnect");
    this.enable = healthCheckerProperties.getBoolean("ping.enabled");
    this.maxSocketConnectCount = healthCheckerProperties.getInt("socketConnectCount");
  }

  public HealthCheckerConfigImpl(String name) {
    this(PING_IDLETIME, PING_INTERVAL, PING_PROBECNT, name, false);
  }

  public HealthCheckerConfigImpl(int idle, int interval, int probes, String name) {
    this(idle, interval, probes, name, false);
  }

  public HealthCheckerConfigImpl(int idle, int interval, int probes, String name, boolean extraCheck) {
    this.pingIdleTime = idle;
    this.pingInterval = interval;
    this.pingProbes = probes;
    this.name = name;
    this.doSocketConnect = extraCheck;
    this.enable = true;
    this.maxSocketConnectCount = 3; // DEFAULT
  }

  public boolean isSocketConnectOnPingFail() {
    return doSocketConnect;
  }

  public boolean isHealthCheckerEnabled() {
    return enable;
  }

  public long getPingIdleTimeMillis() {
    return this.pingIdleTime;
  }

  public long getPingIntervalMillis() {
    return this.pingInterval;
  }

  public int getPingProbes() {
    return this.pingProbes;
  }

  public String getHealthCheckerName() {
    return this.name;
  }

  public int getMaxSocketConnectCount() {
    return this.maxSocketConnectCount;
  }

}
