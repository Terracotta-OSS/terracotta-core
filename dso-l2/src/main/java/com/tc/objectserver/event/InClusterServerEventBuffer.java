package com.tc.objectserver.event;

import com.google.common.collect.ArrayListMultimap;
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

  private final ConcurrentMap<GlobalTransactionID, Multimap<ClientID, ServerEvent>> eventMap = Maps.newConcurrentMap();


  @Override
  public final void storeEvent(final GlobalTransactionID gtxId, final ServerEvent serverEvent,
                               final Set<ClientID> clients) {
    if (eventMap.get(gtxId) == null) {
      eventMap.put(gtxId, ArrayListMultimap.<ClientID, ServerEvent> create());
    }

    for (ClientID clientID : clients) {
      eventMap.get(gtxId).put(clientID, serverEvent);
    }
  }

  @Override
  public Multimap<ClientID, ServerEvent> getServerEventsPerClient(GlobalTransactionID gtxId) {
    return eventMap.get(gtxId);
  }


  @Override
  public void removeEventsForClient(final ClientID clientId) {
    for (Multimap<ClientID, ServerEvent> values : eventMap.values()) {
      values.removeAll(clientId);
    }
  }


  // TODO: To be linked with broadcast acks
  public void acknowledgement(final Set<GlobalTransactionID> acknowledgedGtxIds) {
    for (GlobalTransactionID gtxId : acknowledgedGtxIds) {
      eventMap.remove(gtxId);
    }

    // TODO: Relay to Passive the acked serverevents
  }

}
