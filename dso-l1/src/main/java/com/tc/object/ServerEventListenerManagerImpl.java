package com.tc.object;

import com.google.common.collect.Maps;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.RegisterServerEventListenerMessage;
import com.tc.object.msg.ServerEventListenerMessageFactory;
import com.tc.object.msg.UnregisterServerEventListenerMessage;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventListenerManagerImpl implements ServerEventListenerManager {

  private static final TCLogger LOG = TCLogging.getLogger(ServerEventListenerManagerImpl.class);

  private final Map<String, Map<ServerEventDestination, Set<ServerEventType>>> registry = Maps.newHashMap();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final ServerEventListenerMessageFactory messageFactory;
  private final GroupID stripeId;

  public ServerEventListenerManagerImpl(final ServerEventListenerMessageFactory messageFactory, final GroupID stripeId) {
    this.messageFactory = messageFactory;
    this.stripeId = stripeId;
  }

  @Override
  public void dispatch(final ServerEvent event, final NodeID remoteNode) {
    checkNotNull(event);
    checkNotNull(remoteNode);

    final String name = event.getCacheName();
    final ServerEventType type = event.getType();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Server notification message has been received. Type: "
                + type + ", key: " + event.getKey() + ", cache: " + name);
    }

    lock.readLock().lock();
    try {
      final Map<ServerEventDestination, Set<ServerEventType>> destinations = registry.get(name);
      if (destinations == null) {
        throw new IllegalStateException("Could not find server event destinations for cache: "
                                        + name + ". Incoming event: " + event);
      }

      boolean handlerFound = false;
      for (Map.Entry<ServerEventDestination, Set<ServerEventType>> destination : destinations.entrySet()) {
        final ServerEventDestination target = destination.getKey();
        final Set<ServerEventType> eventTypes = destination.getValue();
        if (eventTypes.contains(type)) {
          handlerFound = true;
          target.handleServerEvent(event);
        }
      }

      if (!handlerFound) {
        throw new IllegalStateException("Could not find handler for server event: " + event);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void registerListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    checkNotNull(destination);
    checkArgument(listenTo != null && !listenTo.isEmpty());

    lock.writeLock().lock();
    try {
      doRegister(destination, listenTo);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void unregisterListener(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    checkNotNull(destination);
    checkArgument(listenTo != null && !listenTo.isEmpty());

    lock.writeLock().lock();
    try {
      doUnregister(destination, listenTo);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void doRegister(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    boolean routingUpdated = true;
    final String name = destination.getDestinationName();

    Map<ServerEventDestination, Set<ServerEventType>> destinations = registry.get(name);
    if (destinations == null) {
      destinations = Maps.newHashMap();
      destinations.put(destination, listenTo);
      registry.put(name, destinations);
    } else {
      final Set<ServerEventType> eventTypes = destinations.get(destination);
      if (eventTypes == null) {
        destinations.put(destination, listenTo);
      } else {
        // do not register twice for the same events
        routingUpdated = eventTypes.addAll(listenTo);
      }
    }

    if (routingUpdated) {
      doRegisterOnServer(name, listenTo);
    }
  }

  private void doRegisterOnServer(final String destinationName, final Set<ServerEventType> listenTo) {
    final RegisterServerEventListenerMessage msg = messageFactory.newRegisterServerEventListenerMessage(stripeId);
    msg.setDestination(destinationName);
    msg.setEventTypes(listenTo);
    msg.send();
  }

  private void doUnregister(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    final String name = destination.getDestinationName();

    final Map<ServerEventDestination, Set<ServerEventType>> destinations = registry.get(name);
    if (destinations != null) {
      final Set<ServerEventType> eventTypes = destinations.get(destination);
      if (eventTypes != null) {
        boolean routingUpdated = eventTypes.removeAll(listenTo);
        // handle potential cascading removals of parent entities
        if (eventTypes.isEmpty()) {
          destinations.remove(destination);
          if (destinations.isEmpty()) {
            registry.remove(name);
          }
        }

        if (routingUpdated) {
          doUnregisterOnServer(name, listenTo);
        }
      }
    }

  }

  private void doUnregisterOnServer(final String destinationName, final Set<ServerEventType> listenTo) {
    final UnregisterServerEventListenerMessage msg = messageFactory.newUnregisterServerEventListenerMessage(stripeId);
    msg.setDestination(destinationName);
    msg.setEventTypes(listenTo);
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
      if (LOG.isDebugEnabled()) {
        LOG.debug("Client '" + remoteNode + "' is reconnected. Re-sending server event listener registrations");
      }

      for (Map.Entry<String, Map<ServerEventDestination, Set<ServerEventType>>> entry : registry.entrySet()) {
        final Set<ServerEventType> eventTypes = EnumSet.noneOf(ServerEventType.class);
        final Map<ServerEventDestination, Set<ServerEventType>> destinations = entry.getValue();
        // collect all distinct event types from destinations registered for a given cache
        for (Set<ServerEventType> types : destinations.values()) {
          eventTypes.addAll(types);
        }
        doRegisterOnServer(entry.getKey(), eventTypes);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    //
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    //
  }

}

