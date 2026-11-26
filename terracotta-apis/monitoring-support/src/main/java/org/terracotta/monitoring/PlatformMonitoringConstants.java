/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
package org.terracotta.monitoring;


/**
 * Constants associated with the data that a platform implementation is expected to provide via the IMonitoringProducer
 * interface.
 * These constants refer to the places in the tree where platform data is registered.
 */
public class PlatformMonitoringConstants {
  /**
   * The name of the top-level node in the tree, used for reporting platform data.
   */
  public static final String PLATFORM_ROOT_NAME = "platform";
  /**
   * The name of the node in the tree which is the parent to all nodes representing connected clients.
   */
  public static final String CLIENTS_ROOT_NAME = "clients";
  /**
   * The name of the node in the tree which is the parent to all nodes representing entities.
   */
  public static final String ENTITIES_ROOT_NAME = "entities";
  /**
   * The name of the node in the tree which is the parent to all nodes representing client-entity fetched connections.
   */
  public static final String FETCHED_ROOT_NAME = "fetched";
  /**
   * The name of the node in the tree which is the parent to all raw connections.
   */
  public static final String CONNECTIONS_ROOT_NAME = "connections";
  /**
   * The name of the node in the tree which is the parent to all consistency voters.
   */
  public static final String VOTERS_ROOT_NAME = "voters";
  /**
   * The name of the node in the tree which stores the ServerState instance.
   * This is a direct child of PLATFORM_ROOT_NAME.
   */
  public static final String STATE_NODE_NAME = "state";

  /**
   * The path of the platform node, for manipulating its children.
   */
  public static final String[] PLATFORM_PATH = {PLATFORM_ROOT_NAME};
  /**
   * The path of the platform's clients node, for manipulating its children.
   */
  public static final String[] CLIENTS_PATH = {PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME};
  /**
   * The path of the platform's entities node, for manipulating its children.
   */
  public static final String[] ENTITIES_PATH = {PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME};
  /**
   * The path of the platform's fetched node, for manipulating its children.
   */
  public static final String[] FETCHED_PATH = {PLATFORM_ROOT_NAME, FETCHED_ROOT_NAME};
  /**
   * The path of the platform's raw connections, for manipulating its children.
   */
  public static final String[] CONNECTIONS_PATH = {PLATFORM_ROOT_NAME, CONNECTIONS_ROOT_NAME};
  /**
   * The path of the platform's voters node, for manipulating its children.
   */
  public static final String[] VOTERS_PATH = {PLATFORM_ROOT_NAME, VOTERS_ROOT_NAME};
  /**
   * Set in STATE_NODE_NAME when the server enters a stopped state (typically only during startup).
   */
  public static final String SERVER_STATE_STOPPED = "STOPPED";
  /**
   * Set in STATE_NODE_NAME when the server enters the active state.
   */
  public static final String SERVER_STATE_ACTIVE = "ACTIVE";
  /**
   * Set in STATE_NODE_NAME when the server enters the passive state.
   */
  public static final String SERVER_STATE_PASSIVE = "PASSIVE";
  /**
   * Set in STATE_NODE_NAME when the server enters the passive syncing to the active.
   */
  public static final String SERVER_STATE_SYNCHRONIZING = "SYNCHRONIZING";
  /**
   * Set in STATE_NODE_NAME when the server is new and has no cluster state.
   */
  public static final String SERVER_STATE_UNINITIALIZED = "UNINITIALIZED";
}