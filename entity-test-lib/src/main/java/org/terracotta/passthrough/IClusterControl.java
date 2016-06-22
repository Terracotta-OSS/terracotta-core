/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import org.terracotta.connection.Connection;


/**
 * Exposes methods to control or wait for servers within the cluster being tested.
 * It is provided as a generic interface, not specifically coupled to the passthrough classes, so that other test harnesses
 * can provide implementations in order to generalize test code for in-process or multi-process implementations.
 */
public interface IClusterControl {
  /**
   * Restarts the active server in the cluster.
   * 
   * @throws Exception A failure in the restart, defined by the implementation.
   */
  public void restartActive() throws Exception;

  /**
   * Waits for the active server in the cluster to come online and determine that it is active.
   * 
   * @throws Exception A failure waiting, defined by the implementation.
   */
  public void waitForActive() throws Exception;

  /**
   * Terminates current active of this Stripe
   *
   * @throws Exception A failure in terminating the server, defined by the implementation
   */
  public void terminateActive() throws Exception;

  /**
   * Starts the last terminated server
   *
   * @throws Exception A failure in starting, defined by the implementation
   */
  public void startLastTerminatedServer() throws Exception;

  /**
   * Waits for the active server in the cluster to come online and determine that it is passive.
   * 
   * @throws Exception A failure waiting, defined by the implementation.
   */
  public void waitForPassive() throws Exception;

  /**
   * Creates a new connection to the active in the cluster.
   * 
   * @return The Connection on which operations can now be performed.
   */
  public Connection createConnectionToActive();

  /**
   * Shuts down the cluster, rendering any further calls on the receiver or any Connections undefined.
   */
  public void tearDown();
}
