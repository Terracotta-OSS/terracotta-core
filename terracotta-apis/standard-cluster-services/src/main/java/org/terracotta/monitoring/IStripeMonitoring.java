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

import java.io.Serializable;


/**
 * The interface which must be implemented by a monitoring component in order to receive the data entities passed into
 *  IMonitoringProducer, on a server within the stripe.
 *
 * Note that only the implementation on the current active server will receive this data but it will receive the data from
 *  the entire stripe.
 *
 * Note that the values used in these methods are Serializable since they may have come over the wire.
 */
public interface IStripeMonitoring {
  /**
   * Called when a server first becomes active to notify its IStripeMonitoring implementation that it will now start to
   *  receive the other calls in this interface.  The PlatformServer representing itself is provided so that it can identify
   *  the calls which are locally-originating.
   * NOTE:  This is only ever called on the consumerID 0 instance.
   *
   * @param self The description of the active server where this call occurs.
   */
  public void serverDidBecomeActive(PlatformServer self);

  /**
   * Called to notify the implementation when another server has first joined the stripe, meaning that messages may start
   *  arriving from this server.
   * NOTE:  This is only ever called on the consumerID 0 instance.
   *
   * @param server The description of the newly-arrived server.
   */
  public void serverDidJoinStripe(PlatformServer server);

  /**
   * Called to notify the implementation when another server has left the stripe, meaning that no more messages will be
   *  arriving from this server and any others from it are now stale.
   * NOTE:  This is only ever called on the consumerID 0 instance.
   *
   * @param server The description of the now-departed server.
   */
  public void serverDidLeaveStripe(PlatformServer server);

  /**
   * Adds a node to the internal data registry tree or replaces an existing one.
   * By default, new nodes have no children.
   *
   * @param sender The description of the server where the call originated.
   * @param parents The parent node names, starting from the root.
   * @param name The name of the node to create or replace.
   * @param value The value to set for the new node.
   * @return True if the node was created/replaced.  False if a parent couldn't be found.
   */
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value);

  /**
   * Removes a node from the internal data registry tree.
   *
   * @param sender The description of the server where the call originated.
   * @param parents The parent node names, starting from the root.
   * @param name The name of the node to remove.
   * @return True if the node was removed.  False if it or a parent couldn't be found.
   */
  public boolean removeNode(PlatformServer sender, String[] parents, String name);

  /**
   * Makes a best-efforts attempt to push named data to the interface.  This method differs from the add/remove node methods
   *  in that the implementation is allowed to drop, ignore, or overwrite the data at any time.
   * This interface is more appropriate for information such as statistics or other sampling information which may be dropped
   *  if the system is busy or otherwise saturated.
   * The implementation is free to impose whatever limiting heuristics it desires, meaning that the limiting may be based on
   *  storage size, element count, staleness before send, or any other approach.  Additionally, the limit may be applied
   *  globally or on a per-name basis.
   *
   * @param sender The description of the server where the call originated.
   * @param name A name given to identify the data.  An implementation may use this as its limiting heuristic, to ensure that
   *  infrequent data is not disproportionately over-written by very frequent data.
   * @param data The object to push.
   */
  public void pushBestEffortsData(PlatformServer sender, String name, Serializable data);
  /**
   * When a server fails over from passive to active, nodes are replayed once the server is active to rebuild the tree.  Not all
   * implementations may want this so make it a different call with default.
   *
   * @param sender The description of the server where the call originated.
   * @param parents path of the key-value
   * @param name A name given to identify the data.  An implementation may use this as its limiting heuristic, to ensure that
   *  infrequent data is not disproportionately over-written by very frequent data.
   * @param value Serializable value to be placed in the tree
   */
  default void replayNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    addNode(sender, parents, name, value);
  }

  /**
   * Deliver items locally on when the server is passive.  If returns true, deliver the key-value to passive
   * locally as well as to the active.
   * @return
   */

  default boolean requiresLocalDeliveryOnPassive() {
    return false;
  }
}
