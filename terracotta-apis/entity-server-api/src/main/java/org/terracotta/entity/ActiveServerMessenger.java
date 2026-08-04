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

import java.util.function.Consumer;

public interface ActiveServerMessenger extends AutoCloseable {
  /**
   * Sends a message on the server.  The parent message associated with this context
   * has its retirement deferred until after this scheduled message has completed.
   *
   * @param message to enqueue on the server
   */
  void sendMessage(EntityMessage message);
  /**
   * Same as {@link #sendMessage(org.terracotta.entity.EntityMessage)} with result consumers
   * @param message to enqueue on the server
   * @param result consumer called with the response from the message
   * @param failure consumer called in an error occurs with exception
   */
  void sendMessage(EntityMessage message, Consumer<EntityResponse> result, Consumer<Exception> failure);
  /**
   * Defer retirement of the current context message using the message as the trigger
   * @param tag debugging tag for the handle
   * @param message release trigger message
   * @return a release handle to release retirement of the current message
   */
  ReleaseHandle deferRetirement(String tag, EntityMessage message);
  /**
   * Same as {@link #deferRetirement(java.lang.String, org.terracotta.entity.EntityMessage)}
   * @param tag debugging tag for the handle
   * @param message release trigger message
   * @param result consumer called with the response from the message
   * @param failure consumer called in an error occurs with exception
   * @return a release handle to release retirement of the current message
   */
  ReleaseHandle deferRetirement(String tag, EntityMessage message, Consumer<EntityResponse> result, Consumer<Exception> failure);

  /**
   * {@link AutoCloseable} without exception.
   */
  @Override
  void close();
  /**
   * A release handle to release a message with deferred retirement.
   *
   */
  interface ReleaseHandle {
    /**
     * Debugging tag
     * @return
     */
    String tag();
    /**
     * Release the deferred message for retirement.
     */
    void release();
  }
}
