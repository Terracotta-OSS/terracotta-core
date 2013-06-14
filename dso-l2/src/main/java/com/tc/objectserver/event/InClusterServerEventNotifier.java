package com.tc.objectserver.event;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifier implements ServerEventListener, DSOChannelManagerEventListener {

  private static final TCLogger LOG = TCLogging.getLogger(InClusterServerEventNotifier.class);

  private final Map<ServerEventType, Map<ClientID, Set<String>>> registry = Maps.newEnumMap(ServerEventType.class);
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final DSOChannelManager channelManager;
  private final ServerEventBatcher batcher;

  public InClusterServerEventNotifier(final DSOChannelManager channelManager, final ServerEventBatcher batcher) {
    this.batcher = batcher;
    this.channelManager = channelManager;
    this.channelManager.addEventListener(this);
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
            batcher.add(entry.getKey(), event);
          }
        }
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Registration is relatively rare operation comparing to event firing.
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

        if (LOG.isDebugEnabled()) {
          LOG.debug("Client '" + clientId + "' has registered server event listener for cache '"
                    + destination + "'. Event types: " + events);
        }
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

      if (LOG.isDebugEnabled()) {
        LOG.debug("Client '" + clientId + "' has unregistered server event listener for cache '"
                  + destination + "'");
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

  @Override
  public void channelCreated(final MessageChannel channel) {
    // ignore
  }

  @Override
  public void channelRemoved(final MessageChannel channel) {
    final ClientID clientId = channelManager.getClientIDFor(channel.getChannelID());
    if (clientId != null) {
      unregisterClient(clientId);
    }
  }
}
