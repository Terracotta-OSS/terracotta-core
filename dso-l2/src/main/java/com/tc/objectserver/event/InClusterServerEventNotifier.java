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

import java.util.Map;
import java.util.Set;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifier implements ServerEventListener, DSOChannelManagerEventListener,
    ServerEventBuffer {

  private final Map<GlobalTransactionID, Multimap<ClientID, ServerEvent>> eventMap = Maps.newHashMap(); //TODO: Do I need a concurrentmap
  private final DSOChannelManager channelManager;

  public InClusterServerEventNotifier(final DSOChannelManager channelManager) {
    this.channelManager = channelManager;
    this.channelManager.addEventListener(this);
  }


  @Override
  public final void handleServerEvent(final ServerEventWrapper eventWrapper) {
    if (eventMap.get(eventWrapper.getGtxId()) == null) {
      eventMap.put(eventWrapper.getGtxId(), ArrayListMultimap.<ClientID, ServerEvent>create());
    }

    for (ClientID clientID : eventWrapper.getClients()) {
      eventMap.get(eventWrapper.getGtxId()).put(clientID, eventWrapper.getEvent());
    }
  }

  @Override
  public void channelCreated(final MessageChannel channel) {
    // ignore
  }

  @Override
  public void channelRemoved(final MessageChannel channel) {
    final ClientID clientId = channelManager.getClientIDFor(channel.getChannelID());
    if (clientId != null) {
      // TODO: Should remove the entries for this client from the eventMap and relay to passive
    }
  }


  public void acknowledgement(final Set<GlobalTransactionID> acknowledgedGtxIds) {
    for (GlobalTransactionID gtxId : acknowledgedGtxIds) {
      eventMap.remove(gtxId);
    }

    // TODO: Relay to Passive the acked serverevents
  }

  @Override
  public Multimap<ClientID, ServerEvent> getServerEvent(GlobalTransactionID gtxId) {
    return eventMap.get(gtxId);
  }

}
