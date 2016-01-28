/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.core.api;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityID;
import com.tc.util.State;


/**
 * The generic interface of the component which collects topology and life cycle related event data in order to relay that
 * information over to any attached event monitoring system.
 */
public interface ITopologyEventCollector {
  /**
   * Called when the server first enters the given state.
   * 
   * @param state The new server state now set.
   */
  public void serverDidEnterState(State state);

  /**
   * Called when a client connects for the first time.  Note that this won't be called if the connection is deemed to be a
   * reconnection of a previously connected client.
   * 
   * @param channel The channel used by the client.
   * @param client The client.
   */
  public void clientDidConnect(MessageChannel channel, ClientID client);

  /**
   * Called when a client explicitly and safely disconnects or when the reconnect window for an missing client closes.
   * 
   * @param channel The now-closed channel used by the client.
   * @param client The client.
   */
  public void clientDidDisconnect(MessageChannel channel, ClientID client);

  /**
   * Called when an entity is explicitly created in response to a request from a client.
   * 
   * @param id The unique identifier for this entity.
   * @param isActive Whether or not it was created in active mode.
   */
  public void entityWasCreated(EntityID id, boolean isActive);

  /**
   * Called when an entity is explicitly destroyed in response to a request from a client.
   * 
   * @param id The unique identifier for this entity.
   */
  public void entityWasDestroyed(EntityID id);

  /**
   * Called when an entity is reloaded from an existing state, either on restart (loading from disk) on fail-over promotion
   * of passive to active (which is potentially in-memory, only, as the server process remains).
   * 
   * @param id The unique identifier for this entity.
   * @param isActive Whether or not it was created in active mode.
   */
  public void entityWasReloaded(EntityID id, boolean isActive);

  /**
   * Called when a given client successfully fetches a specific entity.  Note that the same client can fetch a given entity
   * multiple times, with overlapping extents.
   * A non-existent entity cannot be fetched and all open fetches of a client must be released before it can be destroyed.
   * 
   * @param client The client.
   * @param id The unique identifier for this entity.
   */
  public void clientDidFetchEntity(ClientID client, EntityID id);

  /**
   * Called when a given client successfully releases a specific entity.  Note that the same client can fetch a given entity
   * multiple times, with overlapping extents, and releasing one does not invalidate the others.
   * A non-existent entity cannot be fetched and all open fetches of a client must be released before it can be destroyed.
   * 
   * @param client The client.
   * @param id The unique identifier for this entity.
   */
  public void clientDidReleaseEntity(ClientID client, EntityID id);
}
