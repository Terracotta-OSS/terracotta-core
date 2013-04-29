package com.tc.objectserver.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ServerEventType;
import com.tc.object.msg.ServerEventMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.event.ServerEvent;
import com.tc.objectserver.event.ServerEventListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifier implements ServerEventListener {

  private static final TCLogger LOG = TCLogging.getLogger(InClusterServerEventNotifier.class);

  private final Map<ServerEventType, Map<ClientID, Set<String>>> registry = Maps.newEnumMap(ServerEventType.class);

  private final DSOChannelManager channelManager;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public InClusterServerEventNotifier(final DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  @Override
  public final void handleServerEvent(final ServerEvent event) {
    lock.readLock().lock();
    try {
      final Map<ClientID, Set<String>> clientToDestMap = registry.get(event.getType());
      if (clientToDestMap != null) {
        for (Map.Entry<ClientID, Set<String>> entry : clientToDestMap.entrySet()) {
          final Set<String> destinations = entry.getValue();
          if (destinations.contains(event.getCacheName())) {
            sendNotification(entry.getKey(), event);
          }
        }
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  void sendNotification(final ClientID clientId, final ServerEvent event) {
    try {
      final MessageChannel channel = channelManager.getActiveChannel(clientId);
      final ServerEventMessage msg = (ServerEventMessage)channel.createMessage(
          TCMessageType.SERVER_EVENT_MESSAGE);
      msg.setCacheName(event.getCacheName());
      msg.setType(event.getType());
      msg.setKey(event.getKey());
      // TODO create a SEDA stage to send asynchronously ?
      msg.send();
    } catch (NoSuchChannelException e) {
      LOG.warn("Cannot find channel for client: " + clientId
               + ". The client will no longer receive server notifications.");
      unregisterClient(clientId);
    }
  }

  /**
   * Registration is relatively rare operation comparing to notification.
   * So we can loop thru the registry instead of maintaining a reversed data structure.
   */
  public final void register(final ClientID clientId, final String destination, final Set<ServerEventType> events) {
    lock.writeLock().lock();
    try {
      for (ServerEventType event : events) {
        Map<ClientID, Set<String>> clientToDestMap = registry.get(event);
        if (clientToDestMap == null) {
          clientToDestMap = Maps.newHashMap();
          registry.put(event, clientToDestMap);
        }

        Set<String> destinations = clientToDestMap.get(clientId);
        if (destinations == null) {
          destinations = Sets.newHashSet();
          clientToDestMap.put(clientId, destinations);
        }
        destinations.add(destination);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public final void unregister(final ClientID clientId, final String destination) {
    lock.writeLock().lock();
    try {
      for (Map<ClientID, Set<String>> clientToDestMap : registry.values()) {
        final Set<String> destinations = clientToDestMap.get(clientId);
        if (destinations != null) {
          destinations.remove(destination);
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void unregisterClient(final ClientID clientId) {
    lock.writeLock().lock();
    try {
      for (Map<ClientID, Set<String>> clientToDestMap : registry.values()) {
        clientToDestMap.remove(clientId);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}
