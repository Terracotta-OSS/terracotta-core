package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.InterestType;
import com.tc.object.msg.ServerInterestMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.interest.Interest;
import com.tc.objectserver.interest.InterestListener;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterInterestNotifier implements InterestListener {

  private static final TCLogger LOG = TCLogging.getLogger(InClusterInterestNotifier.class);

  private final Map<InterestType, Map<ClientID, Set<String>>> registry =
      new EnumMap<InterestType, Map<ClientID, Set<String>>>(InterestType.class);

  private final DSOChannelManager channelManager;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public InClusterInterestNotifier(final DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  @Override
  public final void onInterest(final Interest interest) {
    lock.readLock().lock();
    try {
      final Map<ClientID, Set<String>> clientToDestMap = registry.get(interest.getType());
      if (clientToDestMap != null) {
        for (Map.Entry<ClientID, Set<String>> entry : clientToDestMap.entrySet()) {
          final Set<String> destinations = entry.getValue();
          if (destinations.contains(interest.getCacheName())) {
            sendNotification(entry.getKey(), interest);
          }
        }
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  void sendNotification(final ClientID clientId, final Interest interest) {
    try {
      final MessageChannel channel = channelManager.getActiveChannel(clientId);
      final ServerInterestMessage msg = (ServerInterestMessage)channel.createMessage(
          TCMessageType.SERVER_INTEREST_MESSAGE);
      msg.setCacheName(interest.getCacheName());
      msg.setInterestType(interest.getType());
      msg.setKey(interest.getKey());
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
  public final void register(final ClientID clientId, final String destination, final Set<InterestType> interests) {
    lock.writeLock().lock();
    try {
      for (InterestType interest : interests) {
        Map<ClientID, Set<String>> clientToDestMap = registry.get(interest);
        if (clientToDestMap == null) {
          clientToDestMap = new HashMap<ClientID, Set<String>>();
          registry.put(interest, clientToDestMap);
        }

        Set<String> destinations = clientToDestMap.get(clientId);
        if (destinations == null) {
          destinations = new HashSet<String>();
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
