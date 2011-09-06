/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.exception.TCRuntimeException;
import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.msg.ObjectIDBatchRequest;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.sequence.ObjectIDSequence;

public class RequestObjectIDBatchHandler extends AbstractEventHandler {
  private final ObjectIDSequence        sequenceProvider;
  private ReplicatedClusterStateManager clusterStateMgr;
  private DSOChannelManager             channelManager;

  public RequestObjectIDBatchHandler(ObjectIDSequence sequenceProvider) {
    this.sequenceProvider = sequenceProvider;
  }

  public synchronized void handleEvent(EventContext context) {
    final ObjectIDBatchRequest m = (ObjectIDBatchRequest) context;
    final NodeID nodeID = m.getRequestingNodeID();
    try {
      int batchSize = m.getBatchSize();
      MessageChannel channel = channelManager.getActiveChannel(nodeID);
      ObjectIDBatchRequestResponseMessage response = (ObjectIDBatchRequestResponseMessage) channel
          .createMessage(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE);
      long ids = sequenceProvider.nextObjectIDBatch(batchSize);
      if (ids > ObjectID.MAX_ID) {
        // Since we use a byte for GroupId
        throw new TCRuntimeException("Ran out of ObjectIDs : Max : " + ObjectID.MAX_ID + " Got : " + ids);
      }
      this.clusterStateMgr.publishNextAvailableObjectID(ids + batchSize);
      response.initialize(ids, ids + batchSize);
      response.send();
    } catch (NoSuchChannelException e) {
      getLogger().warn("Not Sending Object ID Request because the channel " + nodeID + " was not active : " + e);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.clusterStateMgr = scc.getL2Coordinator().getReplicatedClusterStateManager();
  }
}
