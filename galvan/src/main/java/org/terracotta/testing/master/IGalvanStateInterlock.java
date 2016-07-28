/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.testing.master;


/**
 * The state interlock represents the coordination of components running in different logical areas of the framework,
 *  potentially on different threads.  The general strategy is that all wait/notify operations, within the harness, must
 *  happen in the same place.  This interface is the abstract representation of that interlock.
 * While it is possible that there be many interlocks or places where multi-threaded state could be of consequence, the
 *  implementation is responsible for ensuring that they all share the same monitor so that they do not race, require
 *  explicit cooperation, or become out of sync.
 * 
 * Note that the methods which check or wait for the interlock state will also throw if the test has already failed.
 */
public interface IGalvanStateInterlock {
  // ----- REGISTRATION -----
  /**
   * Registers a new server within the interlock.  The server is assumed to not yet be running.
   * 
   * @param newServer The server to register.
   */
  public void registerNewServer(ServerProcess newServer);
  /**
   * Registers a new client within the interlock.  Note that all servers an interlock knows about are assumed to be
   *  running, as it will drop them when they terminate.
   * 
   * @param runningClient The running client to register.
   */
  public void registerRunningClient(ClientRunner runningClient);

  // ----- WAITING-----
  /**
   * Waits until there are no more clients registered to the interlock.
   * 
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public void waitForClientTermination() throws GalvanFailureException;
  /**
   * Waits until a server has become active, within the interlock.
   * 
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public void waitForActiveServer() throws GalvanFailureException;
  /**
   * Waits until the given server has informed the interlock that it is running.
   * 
   * @param startingUpServer The server instance to check.
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public void waitForServerRunning(ServerProcess startingUpServer) throws GalvanFailureException;
  /**
   * Waits until the given server has informed the interlock that it has terminated.
   * 
   * @param terminatedServer The server instance to check.
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public void waitForServerTermination(ServerProcess terminatedServer) throws GalvanFailureException;
  /**
   * Waits until all running servers, known to the interlock, have entered a known (active or passive) state.
   * 
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public void waitForAllServerReady() throws GalvanFailureException;

  // ----- CHECK STATE-----
  /**
   * Gets the active server, if there is one.
   * 
   * @return The active server instance or null, if there isn't one.
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public ServerProcess getActiveServer() throws GalvanFailureException;
  /**
   * Gets one passive server, if there is one.  Note that there may be multiple passives and this method can choose which
   *  one it returns.
   * 
   * @return A passive server instance or null, if there isn't one.
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public ServerProcess getOnePassiveServer() throws GalvanFailureException;
  /**
   * Gets a terminated server, if there is one.  Note that there may be multiple terminated server and this method can
   *  choose which one it returns.
   * 
   * @return A terminated server instance or null, if there isn't one.
   * @throws GalvanFailureException The test failure description, if it already failed.
   */
  public ServerProcess getOneTerminatedServer() throws GalvanFailureException;

  // ----- CHANGE STATE-----
  /**
   * Notifies the interlock that the given server became active.  Note that it must have already been registered with
   *  the interlock.
   * 
   * @param server The newly-active server.
   */
  public void serverBecameActive(ServerProcess server);
  /**
   * Notifies the interlock that the given server became passive.  Note that it must have already been registered with
   *  the interlock.
   * 
   * @param server The newly-passive server.
   */
  public void serverBecamePassive(ServerProcess server);
  /**
   * Notifies the interlock that the given server went offline (became terminated).  Note that it must have already been
   *  registered with the interlock.
   * 
   * @param server The now-terminated server.
   */
  public void serverDidShutdown(ServerProcess server);
  /**
   * Notifies the interlock that the given server came online (but it not yet in a known, active/passive, state).  Note
   *  that it must have already been registered with the interlock.
   * 
   * @param server The now-online server.
   */
  public void serverDidStartup(ServerProcess server);
  /**
   * Notifies the interlock that the given client terminated.  Note that it must have already been registered with the
   *  interlock.
   * 
   * @param client The now-terminated client.
   */
  public void clientDidTerminate(ClientRunner client);

  // ----- CLEANUP-----
  /**
   * Forces all the online servers and clients, known to the interlock, to go terminate.  It then waits for them to report
   * that they are offline.
   * Note that this doesn't throw, on test failure, since it is assumed that this is being called AFTER the test has run.
   */
  public void forceShutdown();
}