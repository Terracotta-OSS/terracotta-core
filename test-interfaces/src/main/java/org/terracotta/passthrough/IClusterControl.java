/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
   * Force starts a single server if there is one currently offline, from the initial configuration. If all servers are running,
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
