/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.sequence.ObjectIDSequence;

/**
 * @author steve
 */
public class RequestObjectIDBatchHandler extends AbstractEventHandler {
  private final ObjectIDSequence        sequenceProvider;
  private ReplicatedClusterStateManager clusterStateMgr;

  public RequestObjectIDBatchHandler(ObjectIDSequence sequenceProvider) {
    this.sequenceProvider = sequenceProvider;
  }

  public synchronized void handleEvent(EventContext context) {
    ObjectIDBatchRequestMessage m = (ObjectIDBatchRequestMessage) context;
    int batchSize = m.getBatchSize();
    long id = m.getRequestID();
    MessageChannel channel = m.getChannel();
    ObjectIDBatchRequestResponseMessage response = (ObjectIDBatchRequestResponseMessage) channel
        .createMessage(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE);

    long ids = sequenceProvider.nextObjectIDBatch(batchSize);
    this.clusterStateMgr.publishNextAvailableObjectID(ids + batchSize);
    response.initialize(id, ids, ids + batchSize);
    response.send();
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.clusterStateMgr = scc.getL2Coordinator().getReplicatedClusterStateManager();
  }
}
