package com.tc.object;

import com.google.common.collect.Maps;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ServerEventListenerMessageFactory;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.ServerEventMessage;
import com.tc.object.msg.UnregisterServerEventListenerMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventListenerManagerImpl implements ServerEventListenerManager {

  private static final TCLogger LOG = TCLogging.getLogger(ServerEventListenerManagerImpl.class);

  private final Map<String, SubscribedDestination> registry = Maps.newHashMap();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final ServerEventListenerMessageFactory messageFactory;
  private final GroupID stripeId;

  public ServerEventListenerManagerImpl(final ServerEventListenerMessageFactory messageFactory, final GroupID stripeId) {
    this.messageFactory = messageFactory;
    this.stripeId = stripeId;
  }

  @Override
  public void dispatch(final ServerEventMessage message) {
    final String cacheName = message.getCacheName();
    LOG.info("Server notification message has been received. Type: "
             + message.getType() + ", key: " + message.getKey()
             + ", cache: " + cacheName);

    lock.readLock().lock();
    try {
      final SubscribedDestination destination = registry.get(cacheName);
      if (destination == null) {
        throw new IllegalStateException("Could not find cache by name: " + cacheName);
      }
      destination.target.handleServerEvent(message.getType(), message.getKey());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void registerListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    registerOnServer(destination.getDestinationName(), listenTo);
    registerOnClient(destination, listenTo);
  }

  @Override
  public void unregisterListener(final ServerEventDestination destination) {
    unregisterOnServer(destination.getDestinationName());
    unregisterOnClient(destination);
  }

  private void registerOnClient(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    final String name = destination.getDestinationName();
    lock.writeLock().lock();
    try {
      final SubscribedDestination subscribed = registry.get(name);
      if (subscribed != null) {
        subscribed.eventTypes.addAll(listenTo);
      } else {
        registry.put(name, new SubscribedDestination(destination, listenTo));
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void unregisterOnClient(final ServerEventDestination destination) {
    lock.writeLock().lock();
    try {
      registry.remove(destination.getDestinationName());
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void registerOnServer(final String destinationName, final Set<ServerEventType> listenTo) {
    final RegisterServerEventListenerMessage msg = messageFactory.newRegisterServerEventListenerMessage(stripeId);
    msg.setDestination(destinationName);
    msg.setEventTypes(listenTo);
    msg.send();
  }

  private void unregisterOnServer(final String destinationName) {
    final UnregisterServerEventListenerMessage msg = messageFactory.newUnregisterServerEventListenerMessage(stripeId);
    msg.setDestination(destinationName);
    msg.send();
  }

  @Override
  public void cleanup() {
    registry.clear();
  }

  @Override
  public void pause(final NodeID remoteNode, final int disconnected) {
    //TODO: prevent clients from registering new listeners then paused
  }

  @Override
  public void unpause(final NodeID remoteNode, final int disconnected) {
    // on reconnect - resend all local mappings to server
    lock.readLock().lock();
    try {
      for (SubscribedDestination subscribed : registry.values()) {
        registerOnServer(subscribed.target.getDestinationName(), subscribed.eventTypes);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
  }

  @Override
  public void shutdown() {
  }

  private static final class SubscribedDestination {
    private final ServerEventDestination target;
    private final Set<ServerEventType> eventTypes;

    private SubscribedDestination(final ServerEventDestination target, final Set<ServerEventType> eventTypes) {
      this.target = target;
      this.eventTypes = eventTypes;
    }
  }

}

