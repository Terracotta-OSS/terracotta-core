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
import com.tc.util.concurrent.SetOnceFlag;


public class ReplicationEnvelope {
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
  public ReplicationEnvelope(NodeID dest, ReplicationMessage msg, Runnable sent, Runnable droppedWithoutSend) {
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
}
