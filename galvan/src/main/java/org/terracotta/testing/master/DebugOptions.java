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
 * Just a struct to batch together the options for debugging the inferior processes (as they are generally just pass through
 * to the relevant components).
 */
public class DebugOptions {
  /**
   * The port the setup client should listen on.
   */
  public int setupClientDebugPort;
  /**
   * The port the destroy client should list on.
   */
  public int destroyClientDebugPort;
  /**
   * The port number to use as the base for the test clients (each will pick ports, in order, starting with this one).
   */
  public int testClientDebugPortStart;
  /**
   * The port number to use as the starting-point for debug port assignments.
   * Setting this to <=0 will result in no server debugging.
   */
  public int serverDebugPortStart;
}
