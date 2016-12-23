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
import com.tc.util.concurrent.SetOnceFlag;


public class ReplicationEnvelope {
  public static ReplicationEnvelope createAddPassiveEnvelope(NodeID dest, ReplicationMessage msg, Runnable sent, Runnable droppedWithoutSend) {
    Assert.assertNotNull(dest);
    Assert.assertNotNull(msg);
    Assert.assertTrue(ReplicationMessage.START == msg.getType());
    return new ReplicationEnvelope(dest, msg, sent, droppedWithoutSend);
  }

  public static ReplicationEnvelope createRemovePassiveEnvelope(NodeID dest, Runnable droppedWithoutSend) {
    Assert.assertNotNull(dest);
    return new ReplicationEnvelope(dest, null, null, droppedWithoutSend);
  }

  public static ReplicationEnvelope createReplicatedMessageEnvelope(NodeID dest, ReplicationMessage msg, Runnable droppedWithoutSend) {
    Assert.assertNotNull(dest);
    Assert.assertNotNull(msg);
    boolean isReplicatedNoop = ((ReplicationMessage.REPLICATE == msg.getType()) && (SyncReplicationActivity.ActivityType.NOOP == msg.getReplicationType()));
    if (isReplicatedNoop) {
      // This better be a real client (otherwise, the synthetic path should have been used).
      Assert.assertFalse(msg.getSource().isNull());
    }
    return new ReplicationEnvelope(dest, msg, null, droppedWithoutSend);
  }

  public static ReplicationEnvelope createSyntheticNoopEnvelope(NodeID dest, ReplicationMessage msg, Runnable droppedWithoutSend) {
    Assert.assertNotNull(dest);
    Assert.assertNotNull(msg);
    Assert.assertTrue(ReplicationMessage.REPLICATE == msg.getType());
    Assert.assertTrue(SyncReplicationActivity.ActivityType.NOOP == msg.getReplicationType());
    Assert.assertTrue(msg.getSource().isNull());
    return new ReplicationEnvelope(dest, msg, null, droppedWithoutSend);
  }


  private final NodeID dest;
  private final ReplicationMessage msg;
  private final Runnable droppedWithoutSend;
  private final Runnable sent;
  private final SetOnceFlag handled = new SetOnceFlag();

  /**
   * Creates an envelope containing a replication message to be sent to the ReplicationSender for processing.
   * 
   * @param dest The destination node of the message.
   * @param msg The message to send.
   * @param droppedWithoutSend A runnable to run if the message is dropped by ReplicationSender and will NOT be replicated to
   *  dest.
   */
  private ReplicationEnvelope(NodeID dest, ReplicationMessage msg, Runnable sent, Runnable droppedWithoutSend) {
    this.dest = dest;
    this.msg = msg;
    this.sent = sent;
    this.droppedWithoutSend = droppedWithoutSend;
  }
  
  public NodeID getDestination() {
    return dest;
  }
  
  public ReplicationMessage getMessage() {
    return msg;
  }
  
  public void sent() {
    handled.set();
    if (sent != null) {
      sent.run();
    }
  }
  
  public void droppedWithoutSend() {
    handled.set();
    if (droppedWithoutSend != null) {
      droppedWithoutSend.run();
    }
  }
  
  public boolean wasSentOrDropped() {
    return handled.isSet();
  }

  public boolean isRemovePassiveMessage() {
    return (null == this.msg);
  }

  public boolean isAddPassiveMessage() {
    return (null != this.msg) && (this.msg.getType() == ReplicationMessage.START);
  }

  public boolean isSyntheticNoopMessage() {
    boolean isSyntheticNoop = false;
    boolean isReplicatedNoop = ((ReplicationMessage.REPLICATE == this.msg.getType()) && (SyncReplicationActivity.ActivityType.NOOP == this.msg.getReplicationType()));
    if (isReplicatedNoop) {
      // This is synthetic if it has no source.
      // Otherwise, this is a special-case of a noop, which came from a client and must be replicated to the passive to
      // communicate that the client has gone away and persistors should do cleanup.
      isSyntheticNoop = this.msg.getSource().isNull();
    }
    return isSyntheticNoop;
  }
}
