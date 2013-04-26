package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.InterestListenerMessageFactory;
import com.tc.object.msg.RegisterInterestListenerMessage;
import com.tc.object.msg.ServerInterestMessage;
import com.tc.object.msg.UnregisterInterestListenerMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Eugene Shelestovich
 */
public class ServerInterestListenerManagerImpl implements ServerInterestListenerManager {

  private static final TCLogger LOG = TCLogging.getLogger(ServerInterestListenerManagerImpl.class);

  private final Map<String, SubscribedDestination> registry =
      new HashMap<String, SubscribedDestination>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final InterestListenerMessageFactory messageFactory;
  private final GroupID stripeId;

  public ServerInterestListenerManagerImpl(final InterestListenerMessageFactory messageFactory, final GroupID stripeId) {
    this.messageFactory = messageFactory;
    this.stripeId = stripeId;
  }

  @Override
  public void dispatchInterest(final ServerInterestMessage message) {
    final String cacheName = message.getCacheName();
    LOG.info("Server notification message has been received. Type: "
             + message.getInterestType() + ", key: " + message.getKey()
             + ", cache: " + cacheName);

    lock.readLock().lock();
    try {
      final SubscribedDestination destination = registry.get(cacheName);
      if (destination == null) {
        throw new IllegalStateException("Could not find cache by name: " + cacheName);
      }
      destination.target.handleInterest(message.getInterestType(), message.getKey());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void registerInterestListener(final InterestDestination destination, final Set<InterestType> listenTo) {
    registerOnServer(destination.getDestinationName(), listenTo);
    registerOnClient(destination, listenTo);
  }

  @Override
  public void unregisterInterestListener(final InterestDestination destination) {
    unregisterOnServer(destination.getDestinationName());
    unregisterOnClient(destination);
  }

  private void registerOnClient(final InterestDestination destination, final Set<InterestType> listenTo) {
    final String name = destination.getDestinationName();
    lock.writeLock().lock();
    try {
      final SubscribedDestination subscribed = registry.get(name);
      if (subscribed != null) {
        subscribed.interestTypes.addAll(listenTo);
      } else {
        registry.put(name, new SubscribedDestination(destination, listenTo));
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void unregisterOnClient(final InterestDestination destination) {
    lock.writeLock().lock();
    try {
      registry.remove(destination.getDestinationName());
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void registerOnServer(final String destinationName, final Set<InterestType> listenTo) {
    final RegisterInterestListenerMessage msg = messageFactory.newRegisterInterestListenerMessage(stripeId);
    msg.setDestination(destinationName);
    msg.setInterestTypes(listenTo);
    msg.send();
  }

  private void unregisterOnServer(final String destinationName) {
    final UnregisterInterestListenerMessage msg = messageFactory.newUnregisterInterestListenerMessage(stripeId);
    msg.setDestination(destinationName);
    msg.send();
  }

  @Override
  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    // on reconnect - resend all local mappings to server
    lock.readLock().lock();
    try {
      for (SubscribedDestination subscribed : registry.values()) {
        registerOnServer(subscribed.target.getDestinationName(), subscribed.interestTypes);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void cleanup() {
    registry.clear();
  }

  @Override
  public void pause(final NodeID remoteNode, final int disconnected) {
  }

  @Override
  public void unpause(final NodeID remoteNode, final int disconnected) {
  }

  @Override
  public void shutdown() {
  }

  private static final class SubscribedDestination {
    private final InterestDestination target;
    private final Set<InterestType> interestTypes;

    private SubscribedDestination(final InterestDestination target, final Set<InterestType> interestTypes) {
      this.target = target;
      this.interestTypes = interestTypes;
    }
  }

}

