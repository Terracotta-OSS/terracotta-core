/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.groups.MessageID;
import com.tc.object.tx.ServerTransactionID;

public interface L2ObjectSyncAckManager {

  /**
   * Startup the object sync with sync messages coming from a particular node.
   */
  public void reset();

  /**
   * Add an object sync message to be ACKed upon completion.
   */
  public void addObjectSyncMessageToAck(final ServerTransactionID stxnID, final MessageID requestID);

  /**
   * Remove a pending ACK
   */
  public void removeAckForTxn(final ServerTransactionID stxnID);

  /**
   * Complete the object sync
   */
  public void objectSyncComplete();

  /**
   * ACK the object sync txn inline for use when the object sync transaction is ignored (when the L2 is already
   * PASSIVE-STANDBY).
   */
  public void ackObjectSyncTxn(final ServerTransactionID stxnID);
}
