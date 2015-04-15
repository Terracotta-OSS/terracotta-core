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

  @Override
  public void cleanup() {
    // nothing to do
  }

  @Override
  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    if (GroupID.ALL_GROUPS.equals(requestTo) || remoteNode.equals(requestTo)) {
      handshakeMessage.setIsObjectIDsRequested(sequence.isBatchRequestPending());
    } else {
      handshakeMessage.setIsObjectIDsRequested(false);
    }
  }

  @Override
  public void pause(NodeID remoteNode, int disconnected) {
    // NOP
  }

  @Override
  public void unpause(NodeID remoteNode, int disconnected) {
    // NOP
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    // NOP
  }

}
