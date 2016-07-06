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


/**
 * Exposes methods to control or wait for servers within the cluster being tested.
 * It is provided as a generic interface, not specifically coupled to any one cluster implementation, so that various test
 * harnesses can interact with standard or partially mocked-out clusters while still remaining generic and portable.
 */
public interface IClusterControl {
  /**
   * Waits for a server to become active. Returns immediately if there already is one. Behavior is undefined if no servers
   * are running.
   * 
   * @throws Exception Implementation-defined failure.
   */
  public void waitForActive() throws Exception;

  /**
   * Waits until all running servers which are not active enter passive standby state. Returns immediately if they already
   * are in this state. Returns immediately if there are no running servers in an unknown state.
   * 
   * @throws Exception Implementation-defined failure.
   */
  public void waitForRunningPassivesInStandby() throws Exception;

  /**
   * Starts a single server if there is one currently offline, from the initial configuration. If all servers are running,
   * does nothing.
   * 
   * @throws Exception Implementation-defined failure.
   */
  public void startOneServer() throws Exception;

  /**
   * Starts all servers which are currently offline, from the initial configuration. If all servers are running, does
   * nothing.
   * 
   * @throws Exception Implementation-defined failure.
   */
  public void startAllServers() throws Exception;

  /**
   * Forces the currently-active server to terminate, returning once it has. Behavior is undefined if there is no active
   * server.
   *
   * @throws Exception Implementation-defined failure.
   */
  public void terminateActive() throws Exception;

  /**
   * Forces a currently-passive server to terminate, returning once it has. If there are no running passive servers, does
   * nothing.
   * 
   * @throws Exception Implementation-defined failure.
   */
  public void terminateOnePassive() throws Exception;

  /**
   * Forces all currently-running servers to terminate, returning once they have. If there are no running servers, does
   * nothing.
   * 
   * @throws Exception Implementation-defined failure.
   */
  public void terminateAllServers() throws Exception;
}
