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

import com.tc.net.core.event.TCConnectionEventListener;

/**
 * Helps HealthChecker in doing extra checks to monitor peer node's health. Here, a socket connect is attempted to some
 * of the peer node's listening port.
 * 
 * @author Manoj
 */
public interface HealthCheckerSocketConnect extends TCConnectionEventListener {

  enum SocketConnectStartStatus {
    STARTED, NOT_STARTED, FAILED
  }

  public SocketConnectStartStatus start();

  /* Once in a probe interval, the health checker queries to get the connect status if wanted */
  public boolean probeConnectStatus();

  public void addSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener);

  public void removeSocketConnectEventListener(HealthCheckerSocketConnectEventListener listener);

  public void stop();
}
