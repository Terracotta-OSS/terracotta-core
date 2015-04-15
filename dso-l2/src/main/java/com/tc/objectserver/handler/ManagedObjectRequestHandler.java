/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
import com.tc.util.BitSetObjectIDSet;
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
  private final AtomicLong      repeatLookups = new AtomicLong();
  private ObjectRequestManager  objectRequestManager;

  private static final TCLogger logger        = TCLogging.getLogger(ManagedObjectRequestHandler.class);

  public ManagedObjectRequestHandler(Counter globalObjectRequestCounter) {
    this.globalObjectRequestCounter = globalObjectRequestCounter;
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
    Set<ObjectID> requestedIDs = new BitSetObjectIDSet(rmom.getRequestedObjectIDs());
    ClientID clientID = (ClientID) rmom.getSourceNodeID();
    ObjectIDSet removedIDs = new BitSetObjectIDSet(rmom.getRemoved());

    final int numObjectsRequested = requestedIDs.size();
    if (numObjectsRequested != 0) {
      this.globalObjectRequestCounter.increment(numObjectsRequested);
      this.channelStats.notifyReadOperations(channel, numObjectsRequested);
    }

    final int numObjectsRemoved = removedIDs.size();

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
