/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.idprovider.impl;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.util.sequence.BatchSequenceReceiver;

public class ObjectIDClientHandshakeRequester implements ClientHandshakeCallback {

  private final BatchSequenceReceiver sequence;
  private final GroupID               requestTo;

  public ObjectIDClientHandshakeRequester(BatchSequenceReceiver sequence) {
    this(sequence, GroupID.ALL_GROUPS);
  }

  public ObjectIDClientHandshakeRequester(BatchSequenceReceiver sequence, GroupID requestTo) {
    this.sequence = sequence;
    this.requestTo = requestTo;
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    if (GroupID.ALL_GROUPS.equals(requestTo) || remoteNode.equals(requestTo)) {
      handshakeMessage.setIsObjectIDsRequested(sequence.isBatchRequestPending());
    } else {
      handshakeMessage.setIsObjectIDsRequested(false);
    }
  }

  public void pause(NodeID remoteNode, int disconnected) {
    // NOP
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    // NOP
  }

  public void shutdown() {
    // NOP
  }

}
