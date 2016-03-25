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

/**
 *
 */
public class ReplicationEnvelope {
  
  private final NodeID dest;
  private final ReplicationMessage msg;
  private final Runnable waitRelease;

  public ReplicationEnvelope(NodeID dest, ReplicationMessage msg, Runnable waitRelease) {
    this.dest = dest;
    this.msg = msg;
    this.waitRelease = waitRelease;
  }
  
  public NodeID getDestination() {
    return dest;
  }
  
  public ReplicationMessage getMessage() {
    return msg;
  }
  
  public void release() {
    if (waitRelease != null) {
      waitRelease.run();
    }
  }
}
