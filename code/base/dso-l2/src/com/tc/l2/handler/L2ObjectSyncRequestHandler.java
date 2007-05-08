/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class L2ObjectSyncRequestHandler extends AbstractEventHandler {

  private final L2ObjectStateManager l2ObjectStateMgr;
  private Sink                       dehydrateSink;
  private ObjectManager              objectManager;

  public L2ObjectSyncRequestHandler(L2ObjectStateManager objectStateManager) {
    l2ObjectStateMgr = objectStateManager;
  }

  public void handleEvent(EventContext context) {
    SyncObjectsRequest request = (SyncObjectsRequest) context;
    doSyncObjectsRequest(request);
  }

  // TODO:: Update stats so that admin console reflects these data
  private void doSyncObjectsRequest(SyncObjectsRequest request) {
    NodeID nodeID = request.getNodeID();
    ManagedObjectSyncContext lookupContext = l2ObjectStateMgr.getSomeObjectsToSyncContext(nodeID, 500, dehydrateSink);
    // TODO:: Remove ChannelID from ObjectManager interface
    if (lookupContext != null) {
      objectManager.lookupObjectsFor(ChannelID.NULL_ID, lookupContext);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.dehydrateSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE).getSink();
  }
}
