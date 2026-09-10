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

package org.terracotta.entity;


/**
 * Communicator allowing server-side entities to push messages to client-side entities.
 *
 * @author twu
 */
public interface ClientCommunicator {

  /**
   * Send a message to the client-side of an entity.
   *
   * @param clientDescriptor The client side instance to send to
   * @param message The message to send
   */
  void sendNoResponse(ClientDescriptor clientDescriptor, EntityResponse message);
  /**
   * Unilaterally closes the server-side connection associated with entity ClientDescriptor.
   * Any other entity ClientDescriptors associated with the connection will also be invalidated
   * There is no notification as to whether this method has succeeded in its goal as all interactions
   * are asynchronous.  For confirmation of connection closer, consult entity based callbacks.
   *
   * @param clientDescriptor The client side instance the connection to be closed is associated with
   */
  void closeClientConnection(ClientDescriptor clientDescriptor);
}
