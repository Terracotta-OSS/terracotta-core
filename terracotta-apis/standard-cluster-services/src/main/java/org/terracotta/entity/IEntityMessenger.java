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


/**
 * The common interface for a built-in service which provides the ability for an entity to asynchronously message itself.This is also the underlying mechanism which allows entities to contact each other (as they can safely expose code which
 doesn't touch their state, but calls through to this interface, thus allowing a future invocation).This is because an entity can't safely call into another one since its execution state is undefined, relative to another
 entity, and any action taken by such a cross-entity call would not be replicated to passive entities.
 * @param <M>
 * @param <R>
 */
public interface IEntityMessenger<M extends EntityMessage, R extends EntityResponse> {
  /**
   * Sends a message to create an unrelated entity.
   * @param type string representation of the type of entity to be created
   * @param name name of the entity
   * @param version version of the entity
   * @param configuration configuration of the entity
   */
  void create(String type, String name, long version, byte[] configuration);
  /**
   * Sends a message to delete the entity.  This method should be called from the
   * {@link org.terracotta.entity.ActiveServerEntity#disconnected(org.terracotta.entity.ClientDescriptor) disconnected}
   * body or it will fail due to client references still on the entity.
   */
  void destroySelf();
  /**
   * Sends a message to reconfigure the entity.  This method should be called from the
   * {@link org.terracotta.entity.ActiveServerEntity#disconnected(org.terracotta.entity.ClientDescriptor) disconnected}
   * body or it will fail due to client references still on the entity.
   */
  void reconfigureSelf(byte[] configuration);
  /**
   * Same as the messageSelf will no callback.
   *
   * @param message
   * @throws MessageCodecException
   */
  void messageSelf(M message) throws MessageCodecException;
  /**
   * Asynchronously send a message to the entity instance which looked up the service instance.
   *
   * @param message The message to send.
   * @param response A callback used when the result of the invoke is available.  NOTE: callback delivered
   * on the thread which the invoke occurs
   * @throws MessageCodecException The message could not be serialized.
   */
  void messageSelf(M message, Consumer<MessageResponse<R>> response) throws MessageCodecException;
  /**
   * If a response callback is registered, the response will take this form.
   * @param <T> type of the message response
   */
  public interface MessageResponse<T extends EntityResponse> {
    /**
     * Was an exception thrown in the execution of this message.
     *
     * @return true if a message resulted in an exception during invoke
     */
    boolean wasExceptionThrown();
    /**
     * An exception thrown during execution of the message on the active server.
     * @return null if no exception, else the exception that was thrown during invoke
     */
    Exception getException();
    /**
     * @return the response of the invoke or null if an exception that occurred
     */
    T getResponse();
  }
}
