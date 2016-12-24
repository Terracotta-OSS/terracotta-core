/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.msg;

import com.tc.net.NodeID;
import com.tc.util.Assert;


public class ReplicationReplicateMessageIntent extends ReplicationIntent {
  public static ReplicationReplicateMessageIntent createReplicatedMessageEnvelope(NodeID dest, ReplicationMessage msg, Runnable droppedWithoutSend) {
    Assert.assertNotNull(dest);
    Assert.assertNotNull(msg);
    boolean isReplicatedNoop = ((ReplicationMessage.REPLICATE == msg.getType()) && (SyncReplicationActivity.ActivityType.NOOP == msg.getReplicationType()));
    if (isReplicatedNoop) {
      // This better be a real client (otherwise, the synthetic path should have been used).
      Assert.assertFalse(msg.getSource().isNull());
    }
    return new ReplicationReplicateMessageIntent(dest, msg, null, droppedWithoutSend);
  }
  
  public static ReplicationReplicateMessageIntent createReplicatedMessageDebugEnvelope(NodeID dest, ReplicationMessage msg, Runnable sent, Runnable droppedWithoutSend) {
    Assert.assertNotNull(dest);
    Assert.assertNotNull(msg);
    boolean isReplicatedNoop = ((ReplicationMessage.REPLICATE == msg.getType()) && (SyncReplicationActivity.ActivityType.NOOP == msg.getReplicationType()));
    if (isReplicatedNoop) {
      // This better be a real client (otherwise, the synthetic path should have been used).
      Assert.assertFalse(msg.getSource().isNull());
    }
    return new ReplicationReplicateMessageIntent(dest, msg, sent, droppedWithoutSend);
  }

  private final ReplicationMessage msg;

  private ReplicationReplicateMessageIntent(NodeID dest, ReplicationMessage msg, Runnable sent, Runnable droppedWithoutSend) {
    super(dest, sent, droppedWithoutSend);
    this.msg = msg;
  }
  
  public ReplicationMessage getMessage() {
    return msg;
  }
}
