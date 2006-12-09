/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.stats.counter.Counter;

import java.util.Collection;
import java.util.Set;

/**
 * Converts the request into a call to the objectManager with the proper next steps initialized I'm not convinced that
 * this stage is necessary. May be able to merge it with another stage.
 *
 * @author steve
 */
public class ManagedObjectRequestHandler extends AbstractEventHandler {

  private ObjectRequestManager  objectRequestManager;
  private ClientStateManager    stateManager;
  private ChannelStats          channelStats;
  private final Counter         globalObjectRequestCounter;
  private final Counter         globalObjectFlushCounter;
  private Sink                  respondObjectRequestSink;

  private static final TCLogger logger = TCLogging.getLogger(ManagedObjectRequestHandler.class);

  public ManagedObjectRequestHandler(Counter globalObjectRequestCounter, Counter globalObjectFlushCounter) {
    this.globalObjectRequestCounter = globalObjectRequestCounter;
    this.globalObjectFlushCounter = globalObjectFlushCounter;
  }

  public void handleEvent(EventContext context) {
    if (context instanceof RequestManagedObjectMessage) {
      handleEventFromClient((RequestManagedObjectMessage) context);
    } else if (context instanceof ManagedObjectRequestContext) {
      handleEventFromServer((ManagedObjectRequestContext) context);
    }
  }

  private void handleEventFromServer(ManagedObjectRequestContext context) {
    Collection ids = context.getRequestedObjectIDs();
    // XXX::TODO:: Server initiated lookups are not updated to the channel counter for now
    final int numObjectsRequested = ids.size();
    if (numObjectsRequested != 0) {
      globalObjectRequestCounter.increment(numObjectsRequested);
    }
    objectRequestManager.requestObjects(ids, context, context.getMaxRequestDepth());
  }

  private void handleEventFromClient(RequestManagedObjectMessage rmom) {

    MessageChannel channel = rmom.getChannel();
    Collection requestedIDs = rmom.getObjectIDs();
    ChannelID channelID = rmom.getChannelID();
    Set removedIDs = rmom.getRemoved();
    int maxRequestDepth = rmom.getRequestDepth();

    final int numObjectsRequested = requestedIDs.size();
    if (numObjectsRequested != 0) {
      globalObjectRequestCounter.increment(numObjectsRequested);
      channelStats.getCounter(channel, ChannelStats.OBJECT_REQUEST_RATE).increment(numObjectsRequested);
    }

    final int numObjectsRemoved = removedIDs.size();
    if (numObjectsRemoved != 0) {
      globalObjectFlushCounter.increment(numObjectsRemoved);
      channelStats.getCounter(channel, ChannelStats.OBJECT_FLUSH_RATE).increment(numObjectsRemoved);
    }

    long t = System.currentTimeMillis();
    stateManager.removeReferences(channelID, removedIDs);
    t = System.currentTimeMillis() - t;
    if (t > 1000 || numObjectsRemoved > 100000) {
      logger.warn("Time to Remove " + numObjectsRemoved + " is " + t + " ms");
    }
    if (numObjectsRequested > 0) {
      ManagedObjectRequestContext reqContext = new ManagedObjectRequestContext(channelID, rmom.getRequestID(),
                                                                               requestedIDs, maxRequestDepth,
                                                                               this.respondObjectRequestSink);
      objectRequestManager.requestObjects(requestedIDs, reqContext, maxRequestDepth);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    objectRequestManager = oscc.getObjectRequestManager();
    stateManager = oscc.getClientStateManager();
    channelStats = oscc.getChannelStats();
    this.respondObjectRequestSink = oscc.getStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE).getSink();
  }

}
