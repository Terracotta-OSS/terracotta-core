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

  @Override
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

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.channelManager = scc.getChannelManager();
    this.clusterStateMgr = scc.getL2Coordinator().getReplicatedClusterStateManager();
  }
}
