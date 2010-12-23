/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.IndexSyncContext;
import com.tc.l2.context.SyncIndexesRequest;
import com.tc.l2.objectserver.L2IndexStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.sequence.SequenceGenerator;
import com.tc.util.sequence.SequenceGenerator.SequenceGeneratorException;

public class L2IndexSyncRequestHandler extends AbstractEventHandler {

  private static final TCLogger     logger = TCLogging.getLogger(L2IndexSyncRequestHandler.class);

  private final L2IndexStateManager l2IndexStateManager;
  private final SequenceGenerator   sequenceGenerator;

  private Sink                      sendSync;

  public L2IndexSyncRequestHandler(L2IndexStateManager l2IndexStateManager, SequenceGenerator sequenceGenerator) {
    this.l2IndexStateManager = l2IndexStateManager;
    this.sequenceGenerator = sequenceGenerator;
  }

  @Override
  public void handleEvent(EventContext context) {
    SyncIndexesRequest request = (SyncIndexesRequest) context;
    doSyncIndexRequest(request);
  }

  // TODO:: Update stats so that admin console reflects these data
  private void doSyncIndexRequest(SyncIndexesRequest request) {
    NodeID nodeID = request.getNodeID();
    IndexSyncContext indexSyncContext = l2IndexStateManager.getIndexToSyncContext(nodeID);
    if (indexSyncContext != null) {
      try {
        indexSyncContext.setSequenceID(sequenceGenerator.getNextSequence(nodeID));
      } catch (SequenceGeneratorException e) {
        throw new AssertionError(e);
      }
      sendSync.add(indexSyncContext);
    }
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.sendSync = oscc.getStage(ServerConfigurationContext.INDEXES_SYNC_SEND_STAGE).getSink();
  }
}
