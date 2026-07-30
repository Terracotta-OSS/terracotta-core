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
 * <p>The methods specifically supported by passive entities.  Note that a passive doesn't know anything about
 *  connected clients, nor can it return data to them.</p>
 * <p>Note that passive entities have no invoke response but we still accept the generic R so that accessors of the codec
 *  will see a consistent type landscape.</p>
 */
public interface PassiveServerEntity<M extends EntityMessage, R extends EntityResponse> extends CommonServerEntity<M, R> {
  /**
   * <p>Invoke a call on the given entity.  Note that passive entities can't return data to the client.</p>
   * <p>This method is called both in the cases of normal client->server method invocation but also in the case of
   *  server->server passive synchronization.</p>
   * <p>Note that the thread used to make this call is determined by consulting the entity's ConcurrencyStrategy so it may be
   *  called concurrently with other invokes.</p>
   * <p>A note about passive synchronization messages:  When the active is given the opportunity to synchronize the data
   *  under a given key, the messages it creates and sends for synchronization will be executed in that key, on the passive,
   *  via this invoke() entry-point.</p>
   * <p>This is the preferred entry point to override, and the method that
   * will be called. Currently it calls the other invoke, but that is merely for
   * compatibility in transition.</p>
   *
   * @param context invoke context for this call
   * @param message The message from a client or upstream active server
   */
  void invokePassive(InvokeContext context, M message) throws EntityUserException;

  /**
   * <p>Called on {@link ConcurrencyStrategy#MANAGEMENT_KEY} to notify the receiver that it is about to start receiving
   *  synchronization messages.</p>
   * <p>Note that this is called after createNew().</p>
   */
  void startSyncEntity();

  /**
   * Called on {@link ConcurrencyStrategy#MANAGEMENT_KEY} to notify the receiver synchronization is complete and it will see
   *  no more synchronization messages.
   */
  void endSyncEntity();

  /**
   * <p>Called on concurrencyKey to notify the receiver that it is about to start receiving synchronization messages on this
   *  key.</p>
   * <p>To clarify:  the concurrencyKey is both the argument (so the entity can logically act on it) but is also the key
   *  where this call is executed.</p>
   */
  void startSyncConcurrencyKey(int concurrencyKey);

  /**
   * <p>Called on concurrencyKey to notify the receiver that synchronization of this key is complete and no more
   *  synchronization messages will be received on that key.</p>
   * <p>Note that no invokes will be made on this key until this method has been called.</p>
   * <p>To clarify:  the concurrencyKey is both the argument (so the entity can logically act on it) but is also the key
   *  where this call is executed.</p>
   */
  void endSyncConcurrencyKey(int concurrencyKey);
}
