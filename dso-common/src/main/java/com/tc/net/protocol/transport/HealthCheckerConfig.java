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
package com.tc.net.protocol.transport;

import java.util.Set;

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
   * RMP-343: L2 SocketConnect L1
   */
  boolean isCallbackPortListenerNeeded();

  String getCallbackPortListenerBindAddress();

  Set<Integer> getCallbackPortListenerBindPort();

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
