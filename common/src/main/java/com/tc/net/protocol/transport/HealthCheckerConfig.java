/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

public interface HealthCheckerConfig {

  // HC - HealthChecker

  /* HC enabled/disabled for this commsMgr */
  boolean isHealthCheckerEnabled();

  /* HC Name - describing what it is monitoring */
  String getHealthCheckerName();

  /* HC tests liveness of a connection when no message transaction is seen on it for more than ping_idle time */
  long getPingIdleTimeMillis();

  /* HC probes a connection once in ping_interval time after it is found idle for ping_idle time */
  long getPingIntervalMillis();

  /* HC probes a idle connection for ping_probes times before tagging it as dead */
  int getPingProbes();

  /**
   * When HC detected the peer has died by above probes, it can do additional checks to see any traces of life left out
   * <ol>
   * <li>check whether the peer is in Long GC
   * <li>more similar checks
   * </ol>
   * <br>
   * If the peer is unresponsive and not died, a grace * period is given before deciding it as dead.
   */
  boolean isSocketConnectOnPingFail();

  int getSocketConnectMaxCount();

  int getSocketConnectTimeout();

  /**
   * Checking time difference between hosts enabled/disabled.
   */
  boolean isCheckTimeEnabled();

  /**
   * If {@link #isCheckTimeEnabled()} is {@code true}, HC checks time difference
   * no more than once per this interval in milliseconds.
   */
  long getCheckTimeInterval();

  /**
   * HC logs a warning if {@link #isCheckTimeEnabled()} is {@code true}
   * and the time difference exceeds this threshold.
   */
  long getTimeDiffThreshold();

}
