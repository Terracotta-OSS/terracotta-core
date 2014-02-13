package com.tc.objectserver.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.server.ServerEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterServerEventBuffer implements ServerEventBuffer, DSOChannelManagerEventListener {

  private final ConcurrentMap<GlobalTransactionID, Multimap<ClientID, ServerEvent>> eventMap = Maps.newConcurrentMap();
  private final DSOChannelManager channelManager;

  public InClusterServerEventBuffer(final DSOChannelManager channelManager) {
    this.channelManager = channelManager;
    this.channelManager.addEventListener(this);
  }


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
  public Multimap<ClientID, ServerEvent> getServerEvent(GlobalTransactionID gtxId) {
    return eventMap.get(gtxId);
  }

  @Override
  public void channelCreated(final MessageChannel channel) {
    // ignore
  }

  @Override
  public void channelRemoved(final MessageChannel channel) {
    final ClientID clientId = channelManager.getClientIDFor(channel.getChannelID());
    if (clientId != null) {
      removeEventsForClient(clientId);
      // TODO: Should relay to passive??
    }
  }

  private void removeEventsForClient(final ClientID clientId) {
    for (Multimap<ClientID, ServerEvent> values : eventMap.values()) {
      values.removeAll(clientId);
    }
  }


  public void acknowledgement(final Set<GlobalTransactionID> acknowledgedGtxIds) {
    for (GlobalTransactionID gtxId : acknowledgedGtxIds) {
      eventMap.remove(gtxId);
    }

    // TODO: Relay to Passive the acked serverevents
  }

}
