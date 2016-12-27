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


public class ReplicationAddPassiveIntent extends ReplicationIntent {
  public static ReplicationAddPassiveIntent createAddPassiveEnvelope(NodeID dest, SyncReplicationActivity activity, Runnable sent, Runnable droppedWithoutSend) {
    Assert.assertNotNull(dest);
    // TODO:  Determine if this activity can be synthesized at a lower level.
    Assert.assertNotNull(activity);
    return new ReplicationAddPassiveIntent(dest, activity, sent, droppedWithoutSend);
  }


  private final SyncReplicationActivity activity;

  private ReplicationAddPassiveIntent(NodeID dest, SyncReplicationActivity activity, Runnable sent, Runnable droppedWithoutSend) {
    super(dest, sent, droppedWithoutSend);
    this.activity = activity;
  }
  
  public SyncReplicationActivity getActivity() {
    return this.activity;
  }
}
