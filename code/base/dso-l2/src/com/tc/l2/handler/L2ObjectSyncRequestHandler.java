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
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2ObjectSyncRequestHandler extends AbstractEventHandler {

  private static final TCLogger      logger                    = TCLogging.getLogger(L2ObjectSyncRequestHandler.class);
  private static final int           L2_OBJECT_SYNC_BATCH_SIZE = TCPropertiesImpl
                                                                   .getProperties()
                                                                   .getInt(
                                                                           TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE);

  private final L2ObjectStateManager l2ObjectStateMgr;
  private Sink                       dehydrateSink;
  private ObjectManager              objectManager;

  public L2ObjectSyncRequestHandler(L2ObjectStateManager objectStateManager) {
    l2ObjectStateMgr = objectStateManager;
    if (L2_OBJECT_SYNC_BATCH_SIZE <= 0) {
      throw new AssertionError(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE
                               + " cant be less than or equal to zero.");
    } else if (L2_OBJECT_SYNC_BATCH_SIZE > 5000) {
      logger.warn(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE + " set too high : "
                  + L2_OBJECT_SYNC_BATCH_SIZE);
    }

  }

  public void handleEvent(EventContext context) {
    SyncObjectsRequest request = (SyncObjectsRequest) context;
    doSyncObjectsRequest(request);
  }

  // TODO:: Update stats so that admin console reflects these data
  private void doSyncObjectsRequest(SyncObjectsRequest request) {
    NodeID nodeID = request.getNodeID();
    ManagedObjectSyncContext lookupContext = l2ObjectStateMgr.getSomeObjectsToSyncContext(nodeID,
                                                                                          L2_OBJECT_SYNC_BATCH_SIZE,
                                                                                          dehydrateSink);
    if (lookupContext != null) {
      objectManager.lookupObjectsFor(ClientID.NULL_ID, lookupContext);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.dehydrateSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE).getSink();
  }
}
