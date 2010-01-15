/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;

public class SyncWriteTransactionReceivedContext implements EventContext {
  private final long     batchID;
  private final ClientID cid;

  public SyncWriteTransactionReceivedContext(long batchID, ClientID cid) {
    this.batchID = batchID;
    this.cid = cid;
  }

  public long getBatchID() {
    return batchID;
  }

  public ClientID getClientID() {
    return cid;
  }
}
