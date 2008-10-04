/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.context.ObjectRequestServerContextImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.stats.counter.Counter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;

/**
 * Converts the request into a call to the objectManager with the proper next steps initialized I'm not convinced that
 * this stage is necessary. May be able to merge it with another stage.
 * 
 * @author steve
 */
public class ManagedObjectRequestHandler extends AbstractEventHandler {

  private ClientStateManager    stateManager;
  private ChannelStats          channelStats;

  private final Counter         globalObjectRequestCounter;
  private final Counter         globalObjectFlushCounter;
  private ObjectRequestManager  objectRequestManager;

  private static final TCLogger logger = TCLogging.getLogger(ManagedObjectRequestHandler.class);

  public ManagedObjectRequestHandler(Counter globalObjectRequestCounter, Counter globalObjectFlushCounter) {
    this.globalObjectRequestCounter = globalObjectRequestCounter;
    this.globalObjectFlushCounter = globalObjectFlushCounter;
  }

  public void handleEvent(EventContext context) {
    if (context instanceof RequestManagedObjectMessage) {
      handleEventFromClient((RequestManagedObjectMessage) context);
    } else if (context instanceof ObjectRequestServerContextImpl) {
      handleEventFromServer((ObjectRequestServerContextImpl) context);
    }
  }

  private void handleEventFromServer(ObjectRequestServerContextImpl context) {
    Collection<ObjectID> ids = context.getLookupIDs();
    // XXX::TODO:: Server initiated lookups are not updated to the channel counter for now
    final int numObjectsRequested = ids.size();
    if (numObjectsRequested != 0) {
      globalObjectRequestCounter.increment(numObjectsRequested);
    }
    objectRequestManager.requestObjects(context.getRequestedNodeID(), context.getRequestID(), context.getLookupIDs(),
                                        context.getMaxRequestDepth(), context.isServerInitiated(), context
                                            .getRequestingThreadName());
  }

  private void handleEventFromClient(RequestManagedObjectMessage rmom) {
    MessageChannel channel = rmom.getChannel();
    ObjectIDSet requestedIDs = rmom.getObjectIDs();
    ClientID clientID = (ClientID) rmom.getSourceNodeID();
    ObjectIDSet removedIDs = rmom.getRemoved();
    int maxRequestDepth = rmom.getRequestDepth();

    final int numObjectsRequested = requestedIDs.size();
    if (numObjectsRequested != 0) {
      globalObjectRequestCounter.increment(numObjectsRequested);
      channelStats.notifyObjectRequest(channel, numObjectsRequested);
    }

    final int numObjectsRemoved = removedIDs.size();
    if (numObjectsRemoved != 0) {
      globalObjectFlushCounter.increment(numObjectsRemoved);
      channelStats.notifyObjectRemove(channel, numObjectsRemoved);
    }

    long t = System.currentTimeMillis();
    stateManager.removeReferences(clientID, removedIDs);
    t = System.currentTimeMillis() - t;
    if (t > 1000 || numObjectsRemoved > 100000) {
      logger.warn("Time to Remove " + numObjectsRemoved + " is " + t + " ms");
    }
    if (numObjectsRequested > 0) {
      objectRequestManager.requestObjects(clientID, rmom.getRequestID(), requestedIDs, maxRequestDepth, false, rmom
          .getRequestingThreadName());
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    stateManager = oscc.getClientStateManager();
    channelStats = oscc.getChannelStats();
    objectRequestManager = oscc.getObjectRequestManager();
  }

}
