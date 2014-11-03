package com.tc.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Maps;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.TaskRunner;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventListenerManagerImpl implements ServerEventListenerManager {

  private static final TCLogger LOG = TCLogging.getLogger(ServerEventListenerManagerImpl.class);

  private final Map<String, Map<ServerEventDestination, Set<ServerEventType>>> registry = Maps.newHashMap();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final long timeoutInterval;
  private final TaskRunner runner;


  public ServerEventListenerManagerImpl(TaskRunner runner) {
    this.runner = runner;
    TCProperties props = TCPropertiesImpl.getProperties();
    timeoutInterval = props.getLong(TCPropertiesConsts.L1_SERVER_EVENT_DELIVERY_TIMEOUT_INTERVAL, (3 * 60));
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
        LOG.warn("Could not find server event destinations for cache: "
                 + name + ". Incoming event: " + event);
        return;
      }

      boolean handlerFound = false;
      for (Map.Entry<ServerEventDestination, Set<ServerEventType>> destination : destinations.entrySet()) {
        final ServerEventDestination target = destination.getKey();
        final Set<ServerEventType> eventTypes = destination.getValue();
        if (eventTypes.contains(type)) {
          handlerFound = true;
          //now to submit and get a future
          ScheduledFuture future = runner.newTimer().schedule(new Runnable() {
            @Override
            public void run() {
              target.handleServerEvent(event);
            }
          }, 0, TimeUnit.MILLISECONDS);
          try {
            future.get(timeoutInterval, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            throw new TCRuntimeException("Interrupted exception thrown while dispatching server event", e);
          } catch (ExecutionException e) {
            throw new TCRuntimeException("Execution exception thrown while dispatching server event", e);
          } catch (TimeoutException e) {
            throw new TCRuntimeException("Dispatching events timed out", e);

          }
        }
      }

      if (!handlerFound) {
        LOG.warn("Could not find handler for server event: " + event);
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
        eventTypes.addAll(listenTo);
      }
    }
  }

  private void doUnregister(final ServerEventDestination destination, final Set<ServerEventType> listenTo) {
    final String name = destination.getDestinationName();

    final Map<ServerEventDestination, Set<ServerEventType>> destinations = registry.get(name);
    if (destinations != null) {
      final Set<ServerEventType> eventTypes = destinations.get(destination);
      if (eventTypes != null) {
        eventTypes.removeAll(listenTo);
        // handle potential cascading removals of parent entities
        if (eventTypes.isEmpty()) {
          destinations.remove(destination);
          if (destinations.isEmpty()) {
            registry.remove(name);
          }
        }
      }
    }
  }


  @Override
  public void cleanup() {
    lock.writeLock().lock();
    try {
      registry.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void pause(final NodeID remoteNode, final int disconnected) {
    // Do Nothing
  }

  @Override
  public void unpause(final NodeID remoteNode, final int disconnected) {
    // on reconnect - resend all server event registrations to server
    if (LOG.isDebugEnabled()) {
      LOG.debug("Client '" + remoteNode + "' is reconnected. Re-sending server event listener registrations");
    }

    for (Map<ServerEventDestination, Set<ServerEventType>> destinationMapping : registry.values()) {
      for (ServerEventDestination serverEventDestination : destinationMapping.keySet()) {
        try {
          serverEventDestination.resendEventRegistrations();
        } catch (TCNotRunningException e) {
          // We can potentially get TCNotRunningExceptions if a connection was just established as the client shuts down
          // since the client is going down anyways, just ignore it.
          if (LOG.isDebugEnabled()) {
            LOG.debug("Got a TCNotRunningException processing event listener re-registrations.");
          }
        }
      }
    }
  }

  @Override
  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    // Do Nothing
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    // Do Nothing
  }

}

