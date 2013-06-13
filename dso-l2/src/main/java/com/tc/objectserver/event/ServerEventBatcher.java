package com.tc.objectserver.event;

import com.google.common.collect.Maps;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ServerEventBatchMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerEvent;
import com.tc.util.concurrent.TaskRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Accumulates server events in a buffer, periodically drains it and sends batches to clients.
 */
public class ServerEventBatcher implements Runnable {

  private static final TCLogger LOG = TCLogging.getLogger(ServerEventBatcher.class);

  private final DSOChannelManager channelManager;
  // multiple producers, single consumer
  private final BlockingQueue<ClientEnvelope> buffer;

  public ServerEventBatcher(final DSOChannelManager channelManager, final TaskRunner taskRunner) {
    this.channelManager = channelManager;
    final int queueSize = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_QUEUE_SIZE, 4096);
    final long interval = TCPropertiesImpl.getProperties()
        .getLong(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_INTERVAL_MS, 50L);
    buffer = new ArrayBlockingQueue<ClientEnvelope>(queueSize);
    taskRunner.newTimer("Server event queue batcher")
        .scheduleWithFixedDelay(this, 20L, interval, TimeUnit.MILLISECONDS);
  }

  @Override
  public void run() {
    if (!buffer.isEmpty()) {
      drain();
    }
  }

  /**
   * Uses a retention policy similar to {@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy}.
   */
  public void add(final ClientID clientId, final ServerEvent event) {
    while (!buffer.offer(new ClientEnvelope(clientId, event))) {
      drain();
    }
  }

  private void drain() {
    final List<ClientEnvelope> toProcess = new ArrayList<ClientEnvelope>(buffer.size());
    buffer.drainTo(toProcess);
    final Map<ClientID, List<ServerEvent>> groups = partition(toProcess);
    // send batch messages for each client
    for (final Map.Entry<ClientID, List<ServerEvent>> entry : groups.entrySet()) {
      send(entry.getKey(), entry.getValue());
    }
  }

  Map<ClientID, List<ServerEvent>> partition(final Collection<ClientEnvelope> envelopes) {
    final Map<ClientID, List<ServerEvent>> groups = Maps.newHashMap();
    // partition by client id
    for (final ClientEnvelope envelope : envelopes) {
      List<ServerEvent> events = groups.get(envelope.clientId);
      if (events == null) {
        events = new ArrayList<ServerEvent>();
        groups.put(envelope.clientId, events);
      }
      events.add(envelope.event);
    }
    return groups;
  }

  void send(final ClientID clientId, final List<ServerEvent> events) {
    final MessageChannel channel;
    try {
      channel = channelManager.getActiveChannel(clientId);
    } catch (NoSuchChannelException e) {
      LOG.warn("Cannot find channel for client: " + clientId + ". The client will no longer receive server events.");
      return;
    }
    // combine events in message batches, one batch per client
    final ServerEventBatchMessage msg = (ServerEventBatchMessage)channel.createMessage(TCMessageType.SERVER_EVENT_BATCH_MESSAGE);
    msg.setEvents(events);
    msg.send();

    if (LOG.isDebugEnabled()) {
      LOG.debug(events.size() + " server events have been sent to client '" + clientId + "'");
    }
  }

  /**
   * Simple holder for buffered events.
   */
  static final class ClientEnvelope {
    final ClientID clientId;
    final ServerEvent event;

    ClientEnvelope(final ClientID clientId, final ServerEvent event) {
      this.clientId = clientId;
      this.event = event;
    }
  }

}
