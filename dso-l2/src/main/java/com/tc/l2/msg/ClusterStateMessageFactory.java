/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.ha.ClusterState;
import com.tc.net.groups.GroupMessage;
import com.tc.net.protocol.transport.ConnectionID;

public class ClusterStateMessageFactory {

  public static GroupMessage createNextAvailableObjectIDMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.OBJECT_ID);
    msg.initMessage(state);
    return msg;
  }

  public static GroupMessage createOKResponse(ClusterStateMessage msg) {
    ClusterStateMessage response = new ClusterStateMessage(ClusterStateMessage.OPERATION_SUCCESS, msg.getMessageID());
    return response;
  }

  public static GroupMessage createNGSplitBrainResponse(ClusterStateMessage msg) {
    ClusterStateMessage response = new ClusterStateMessage(ClusterStateMessage.OPERATION_FAILED_SPLIT_BRAIN, msg
        .getMessageID());
    return response;
  }

  public static GroupMessage createClusterStateMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.COMPLETE_STATE);
    msg.initMessage(state);
    return msg;
  }

  public static GroupMessage createNewConnectionCreatedMessage(ConnectionID connID) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.NEW_CONNECTION_CREATED, connID);
    return msg;
  }

  public static GroupMessage createConnectionDestroyedMessage(ConnectionID connID) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.CONNECTION_DESTROYED, connID);
    return msg;
  }

  public static GroupMessage createNextAvailableGlobalTransactionIDMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.GLOBAL_TRANSACTION_ID);
    msg.initMessage(state);
    return msg;
  }

  public static GroupMessage createNextAvailableDGCIterationMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.DGC_ID);
    msg.initMessage(state);
    return msg;
  }

}
