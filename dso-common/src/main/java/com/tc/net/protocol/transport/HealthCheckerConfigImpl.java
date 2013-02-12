/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.properties.TCProperties;

import java.util.concurrent.TimeUnit;

/**
 * Main implementation of the Health Checker Config. Health Checker related tc.properties are read and a config data
 * structure is built which is passed on to various health checker modules.
 * 
 * @author Manoj
 */
public class HealthCheckerConfigImpl implements HealthCheckerConfig {

  private final boolean    enable;
  private final long       pingIdleTime;
  private final long       pingInterval;
  private final int        pingProbes;
  private final boolean    doSocketConnect;
  private final int        socketConnectTimeout;
  private final int        socketConnectMaxCount;
  private final String     name;
  private final boolean    checkTimeEnabled;
  private final long       checkTimeInterval;
  private final long       timeDiffThreshold;

  // Default ping probe values in milliseconds
  private static final int DEFAULT_PING_IDLETIME          = 45000;
  private static final int DEFAULT_PING_INTERVAL          = 15000;
  private static final int DEFAULT_PING_PROBECNT          = 3;
  private static final int DEFAULT_SCOKETCONNECT_MAXCOUNT = 3;
  private static final int DEFAULT_SOCKETCONNECT_TIMEOUT  = 2;
  private static final long DEFAULT_CHECK_TIME_INTERVAL   = TimeUnit.MINUTES.toMillis(5L);
  private static final long DEFAULT_TIME_DIFF_THRESHOLD   = TimeUnit.MINUTES.toMillis(5L);

  public HealthCheckerConfigImpl(TCProperties healthCheckerProperties, String hcName) {
    this.pingIdleTime = healthCheckerProperties.getLong("ping.idletime");
    this.pingInterval = healthCheckerProperties.getLong("ping.interval");
    this.pingProbes = healthCheckerProperties.getInt("ping.probes");
    this.name = hcName;
    this.doSocketConnect = healthCheckerProperties.getBoolean("socketConnect");
    this.enable = healthCheckerProperties.getBoolean("ping.enabled");
    this.socketConnectMaxCount = healthCheckerProperties.getInt("socketConnectCount");
    this.socketConnectTimeout = healthCheckerProperties.getInt("socketConnectTimeout");
    this.checkTimeEnabled = healthCheckerProperties.getBoolean("checkTime.enabled");
    this.checkTimeInterval = healthCheckerProperties.getLong("checkTime.interval");
    this.timeDiffThreshold = healthCheckerProperties.getLong("checkTime.threshold");
  }

  // Default Ping-Probe cycles. No SocketConnect check
  public HealthCheckerConfigImpl(String name) {
    this(DEFAULT_PING_IDLETIME, DEFAULT_PING_INTERVAL, DEFAULT_PING_PROBECNT, name, false);
  }

  // Custom SocketConnect check. Default SocketConnect values
  public HealthCheckerConfigImpl(long idle, long interval, int probes, String name, boolean socketConnect) {
    this(idle, interval, probes, name, socketConnect, DEFAULT_SCOKETCONNECT_MAXCOUNT, DEFAULT_SOCKETCONNECT_TIMEOUT);
  }

  // All Custom values
  public HealthCheckerConfigImpl(long idle, long interval, int probes, String name, boolean extraCheck,
                                 int socketConnectMaxCount, int socketConnectTimeout) {
    this(idle, interval, probes, name, extraCheck, socketConnectMaxCount, socketConnectTimeout,
        DEFAULT_CHECK_TIME_INTERVAL, DEFAULT_TIME_DIFF_THRESHOLD);
  }

    // All Custom values
  public HealthCheckerConfigImpl(long idle, long interval, int probes, String name, boolean extraCheck,
                                 int socketConnectMaxCount, int socketConnectTimeout,
                                 long checkTimeInterval, long timeDiffThreshold) {
    this.pingIdleTime = idle;
    this.pingInterval = interval;
    this.pingProbes = probes;
    this.name = name;
    this.doSocketConnect = extraCheck;
    this.enable = true;
    this.socketConnectMaxCount = socketConnectMaxCount;
    this.socketConnectTimeout = socketConnectTimeout;
    this.checkTimeEnabled = true;
    this.checkTimeInterval = checkTimeInterval;
    this.timeDiffThreshold = timeDiffThreshold;
  }

  @Override
  public boolean isSocketConnectOnPingFail() {
    return doSocketConnect;
  }

  @Override
  public boolean isHealthCheckerEnabled() {
    return enable;
  }

  @Override
  public long getPingIdleTimeMillis() {
    return this.pingIdleTime;
  }

  @Override
  public long getPingIntervalMillis() {
    return this.pingInterval;
  }

  @Override
  public int getPingProbes() {
    return this.pingProbes;
  }

  @Override
  public String getHealthCheckerName() {
    return this.name;
  }

  @Override
  public int getSocketConnectMaxCount() {
    return this.socketConnectMaxCount;
  }

  @Override
  public int getSocketConnectTimeout() {
    return this.socketConnectTimeout;
  }

  @Override
  public String getCallbackPortListenerBindAddress() {
    throw new AssertionError("CallbackPort Listener not needed for servers");
  }

  @Override
  public int getCallbackPortListenerBindPort() {
    throw new AssertionError("CallbackPort Listener not needed for servers");
  }

  @Override
  public boolean isCallbackPortListenerNeeded() {
    return false;
  }

  @Override
  public boolean isCheckTimeEnabled() {
    return this.checkTimeEnabled;
  }

  @Override
  public long getCheckTimeInterval() {
    return this.checkTimeInterval;
  }

  @Override
  public long getTimeDiffThreshold() {
    return this.timeDiffThreshold;
  }
}
