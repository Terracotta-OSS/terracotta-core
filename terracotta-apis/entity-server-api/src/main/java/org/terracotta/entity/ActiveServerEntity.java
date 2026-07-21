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
 * The methods specifically supported by active entities.  As the active is responsible for "driving" much of the
 *  interaction, it has more capabilities than the corresponding passive.
 */
public interface ActiveServerEntity<M extends EntityMessage, R extends EntityResponse> extends CommonServerEntity<M, R> {
  /**
   * <p>Indicate that the given client is now connected up to this ServerEntity.</p>
   * <p>This is called in response to a client-side fetchEntity() or the reconnect of a client which had previously fetched
   *  the entity, after a server restart or fail-over.</p>
   * <p>Note that this call is made on the {@link ConcurrencyStrategy#MANAGEMENT_KEY}, meaning that it is serialized with
   *  respect to all other messages enqueued for the entity.</p>
   *
   * @param clientDescriptor client-side instance which connected
   */
  void connected(ClientDescriptor clientDescriptor);

  /**
   * <p>Notify the ServerEntity that the given client has disconnected.</p>
   * <p>This is called in response to a client closing its previously fetched entity or when a client disappears from the
   *  cluster (due to a network failure, unexpected termination, etc).</p>
   * <p>Note that this call is made on the {@link ConcurrencyStrategy#MANAGEMENT_KEY}, meaning that it is serialized with
   *  respect to all other messages enqueued for the entity.</p>
   *
   * @param clientDescriptor client-side instance which disconnected
   */
  void disconnected(ClientDescriptor clientDescriptor);

  /**
   * <p>Invoke a call on the given entity.</p>
   * <p>Note that the thread used to make this call is determined by consulting the entity's ConcurrencyStrategy so it may be
   *  called concurrently with other invokes.</p>
   * <p>This is the preferred entry point to override, and the method that
   * will be called. Currently it calls the other invoke, but that is merely for
   * compatibility in transition.</p>
   *
   * @param context invocation context
   * @param message The message from a client
   * @return possible return value
   */
  default R invokeActive(ActiveInvokeContext<R> context, M message) throws EntityUserException {
    return invoke(context.getClientDescriptor(), message);
  }

  /**
   * <p>Invoke a call on the given entity.</p>
   * <p>Note that the thread used to make this call is determined by consulting the entity's ConcurrencyStrategy so it may be
   *  called concurrently with other invokes.</p>
   * <p>This is the legacy endpoint, and will not be called unless the other
   * invoke call is not overridden as it should be. Here for legacy transition.</p>
   *
   * @param descriptor client descriptor this invoke is sourced from
   * @param message The message from a client
   * @return possible return value
   */
  @Deprecated
  default R invoke(ClientDescriptor descriptor, M message) throws EntityUserException {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>Called when an entity was loaded from some persistent state and the entity is expected to already be known to the
   *  server.</p>
   * <p>Specifically, this refers to situations such as a restart or fail-over.  A given entity will always receive a single
   *  createNew() call but can receive any number of loadExisting() calls, in response to server life-cycle.</p>
   * <p>Note that this call is made on the {@link ConcurrencyStrategy#MANAGEMENT_KEY}, meaning that it is serialized with
   *  respect to all other messages enqueued for the entity.</p>
   */
  void loadExisting();

  /**
   * <p>This method will be called in a nonblocking manner as a first step in synchronizing a key of a passive entity.</p>
   *
   * <p>Note: this method is called in a completely nonblocking manner.  All messages associated with this key can run concurrently
   * in invoke with this method call, however no other synchronization calls will run until this method completes.</p>
   *
   * @param syncChannel The output channel to the passive
   * @param concurrencyKey The key of the data to be synchronized
   */
  default void prepareKeyForSynchronizeOnPassive(PassiveSynchronizationChannel<M> syncChannel, int concurrencyKey) {

  }
  /**
   * <p>Passes any information required to describe all entity data/state associated with the given concurrency key to a
   *  passive instance being synchronized to be consistent with the receiver.</p>
   *
   * <p>Note that this method is also run on the concurrencyKey specified, so it blocks other messages executed on that
   *  key.</p>
   *
   * @param syncChannel The output channel to the passive
   * @param concurrencyKey The key of the data to be synchronized
   */
  void synchronizeKeyToPassive(PassiveSynchronizationChannel<M> syncChannel, int concurrencyKey);
  /**
   * Called at the start of reconnect of a new active entity.  The reconnect handler receives all reconnect data
   * from the clients during a fail-over event.
   *
   * @return a handler to receive arbitrary reconnect data from clients.  If null is returned, all reconnect attempts
   * will be rejected and the connection associated with this entity fetch will be rejected
   */
  ReconnectHandler startReconnect();

  interface ReconnectHandler extends AutoCloseable {
    /**
     * <p>Called during client reconnect to allow the client to pass arbitrary extra data to the server-side entity so it can
     *  rebuild any in-memory state it had, related to the connected client.</p>
     * <p>Note that this is called AFTER connected() is called for this clientDescriptor.</p>
     * <p>Note that this call is made on the {@link ConcurrencyStrategy#MANAGEMENT_KEY}, meaning that it is serialized with
     *  respect to all other messages enqueued for the entity.</p>
     *
     * @param clientDescriptor The client-side instance which reconnected
     * @param extendedReconnectData Arbitrary data sent by the client-side instance to rebuild the server-side in-memory state
     * @throws ReconnectRejectedException When a reconnect by the client is not supported by the server, throwing this exception
     * forces the disconnect of the Connection associated with the entity attempting reconnect
     *
     */
    void handleReconnect(ClientDescriptor clientDescriptor, byte[] extendedReconnectData) throws ReconnectRejectedException;
    /**
     * called after all clients that have reconnected to the entity.  Empty default implementation provided so a functional
     * interface can be used if desired
     */
    @Override
    default void close() {

    }
  }
}
