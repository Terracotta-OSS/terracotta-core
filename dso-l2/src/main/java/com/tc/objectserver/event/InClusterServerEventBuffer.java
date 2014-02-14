package com.tc.objectserver.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.ServerEvent;

import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterServerEventBuffer implements ServerEventBuffer {

  private final static Multimap<ClientID, ServerEvent> EMPTY_MAP = ImmutableListMultimap.of();
  // TODO: Look if ConcurrentHashMap can give better performance
  private final SortedMap<GlobalTransactionID, Multimap<ClientID, ServerEvent>> eventMap  = Collections
                                                                                              .synchronizedSortedMap(new TreeMap());


  @Override
  public final void storeEvent(final GlobalTransactionID gtxId, final ServerEvent serverEvent,
                               final Set<ClientID> clients) {
    if (clients.isEmpty()) { return; }

    if (eventMap.get(gtxId) == null) {
      eventMap.put(gtxId, ArrayListMultimap.<ClientID, ServerEvent> create());
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
  public void removeEventsForClient(final ClientID clientId) {
    for (Multimap<ClientID, ServerEvent> values : eventMap.values()) {
      values.removeAll(clientId);
    }
  }


  @Override
  public void clearEventBufferBelowLowWaterMark(final GlobalTransactionID lowWatermark) {
    for (GlobalTransactionID gtxID : eventMap.headMap(lowWatermark).keySet()) {
      eventMap.remove(gtxID);
    }
  }

}
