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

}
