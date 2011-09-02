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
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.stats.counter.Counter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

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
  private final AtomicLong      repeatLookups = new AtomicLong();
  private ObjectRequestManager  objectRequestManager;

  private static final TCLogger logger        = TCLogging.getLogger(ManagedObjectRequestHandler.class);

  public ManagedObjectRequestHandler(Counter globalObjectRequestCounter, Counter globalObjectFlushCounter) {
    this.globalObjectRequestCounter = globalObjectRequestCounter;
    this.globalObjectFlushCounter = globalObjectFlushCounter;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof RequestManagedObjectMessage) {
      handleEventFromClient((RequestManagedObjectMessage) context);
    } else {
      handleEventFromServer((ObjectRequestServerContext) context);
    }
  }

  private void handleEventFromServer(ObjectRequestServerContext context) {
    Collection<ObjectID> ids = context.getRequestedObjectIDs();
    // XXX::TODO:: Server initiated lookups are not updated to the channel counter for now
    final int numObjectsRequested = ids.size();
    if (numObjectsRequested != 0) {
      this.globalObjectRequestCounter.increment(numObjectsRequested);
    }
    this.objectRequestManager.requestObjects(context);
  }

  private void handleEventFromClient(RequestManagedObjectMessage rmom) {
    MessageChannel channel = rmom.getChannel();
    Set<ObjectID> requestedIDs = rmom.getRequestedObjectIDs();
    ClientID clientID = (ClientID) rmom.getSourceNodeID();
    ObjectIDSet removedIDs = rmom.getRemoved();

    final int numObjectsRequested = requestedIDs.size();
    if (numObjectsRequested != 0) {
      this.globalObjectRequestCounter.increment(numObjectsRequested);
      this.channelStats.notifyObjectRequest(channel, numObjectsRequested);
    }

    final int numObjectsRemoved = removedIDs.size();
    if (numObjectsRemoved != 0) {
      this.globalObjectFlushCounter.increment(numObjectsRemoved);
      this.channelStats.notifyObjectRemove(channel, numObjectsRemoved);
    }

    if (numObjectsRequested > 0 || numObjectsRemoved > 0) {
      long t = System.currentTimeMillis();
      this.stateManager.removeReferences(clientID, removedIDs, requestedIDs);
      t = System.currentTimeMillis() - t;
      if (t > 1000 || numObjectsRemoved > 100000) {
        logger.warn("Time to Remove " + numObjectsRemoved + " is " + t + " ms");
      }
    }
    long diff = repeatLookups.addAndGet(numObjectsRequested - requestedIDs.size());
    if (diff > 0 && diff % 100000 == 0) {
      logger.info(" Number of repeated/wasted lookups : " + diff);
    }

    if (requestedIDs.size() > 0) {
      this.objectRequestManager.requestObjects(rmom);
    }
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.stateManager = oscc.getClientStateManager();
    this.channelStats = oscc.getChannelStats();
    this.objectRequestManager = oscc.getObjectRequestManager();
  }

}
