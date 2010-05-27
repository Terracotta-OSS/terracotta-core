/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestType;
import com.tc.object.msg.GetSizeServerMapRequestMessage;
import com.tc.object.msg.GetValueServerMapRequestMessage;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.stats.counter.Counter;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class ServerMapRequestHandler extends AbstractEventHandler implements EventHandler {

  private ServerMapRequestManager serverTCMapRequestManager;
  private final Counter         globalGetValueRequestCounter;
  private final Counter         globalGetSizeRequestCounter;
  private ChannelStats channelStats;

  public ServerMapRequestHandler(Counter globalGetSizeRequestCounter, Counter globalGetValueRequestCounter) {
    this.globalGetSizeRequestCounter = globalGetSizeRequestCounter;
    this.globalGetValueRequestCounter = globalGetValueRequestCounter;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof GetSizeServerMapRequestMessage) {
      final GetSizeServerMapRequestMessage smContext = (GetSizeServerMapRequestMessage) context;
      globalGetSizeRequestCounter.increment();
      this.channelStats.notifyServerMapRequest(ServerMapRequestType.GET_SIZE, smContext.getChannel(), 1);
      this.serverTCMapRequestManager.requestSize(smContext.getRequestID(), smContext.getClientID(), smContext
          .getMapID());
    } else {
      final GetValueServerMapRequestMessage smContext = (GetValueServerMapRequestMessage) context;
      final Map<ObjectID, Collection<ServerMapGetValueRequest>> requests = smContext.getRequests();
      int numRequests = requests.size();
      globalGetValueRequestCounter.increment(numRequests);
      this.channelStats.notifyServerMapRequest(ServerMapRequestType.GET_VALUE_FOR_KEY, smContext.getChannel(), numRequests);
      for (final Entry<ObjectID, Collection<ServerMapGetValueRequest>> e : requests.entrySet()) {
        this.serverTCMapRequestManager.requestValues(smContext.getClientID(), e.getKey(), e.getValue());
      }
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.serverTCMapRequestManager = oscc.getServerTCMapRequestManager();
    this.channelStats = oscc.getChannelStats();
  }

}
