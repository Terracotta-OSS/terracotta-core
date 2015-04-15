/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.ServerEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterServerEventBuffer implements ServerEventBuffer {

  private final static Multimap<ClientID, ServerEvent> EMPTY_MAP = ImmutableListMultimap.of();
  private final ConcurrentMap<GlobalTransactionID, Multimap<ClientID, ServerEvent>> eventMap  = Maps.newConcurrentMap();


  @Override
  public final void storeEvent(final GlobalTransactionID gtxId, final ServerEvent serverEvent,
                               final Set<ClientID> clients) {
    if (eventMap.get(gtxId) == null) {
      eventMap.put(gtxId, ArrayListMultimap.<ClientID, ServerEvent> create(1, 1));
    }

    for (ClientID clientID : clients) {
      eventMap.get(gtxId).put(clientID, serverEvent);
    }
  }


  @Override
  public Multimap<ClientID, ServerEvent> getServerEventsPerClient(GlobalTransactionID gtxId) {
    final Multimap<ClientID, ServerEvent> eventsPerClient = eventMap.get(gtxId);
    return (eventsPerClient == null) ? EMPTY_MAP : eventsPerClient;
  }


  @Override
  public void removeEventsForTransaction(GlobalTransactionID globalTransactionID) {
    eventMap.remove(globalTransactionID);
  }


  @Override
  public void clearEventBufferBelowLowWaterMark(final GlobalTransactionID lowWatermark) {
    for (GlobalTransactionID gtxID : eventMap.keySet()) {
      if (gtxID.lessThan(lowWatermark)) {
        eventMap.remove(gtxID);
      }
    }
  }

}
