/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClientGlobalTransactionManagerImpl implements ClientGlobalTransactionManager {

  private static final TCLogger             logger               = TCLogging
                                                                     .getLogger(ClientGlobalTransactionManagerImpl.class);

  private static final int                  ALLOWED_LWM_DELTA    = 100;
  private final Set                         applied              = new HashSet();
  private final SortedMap                   globalTransactionIDs = new TreeMap();

  private GlobalTransactionID               lowWatermark         = GlobalTransactionID.NULL_ID;
  private final RemoteTransactionManager    remoteTransactionManager;
  private int                               ignoredCount         = 0;
  private final PreTransactionFlushCallback preTransactionFlushCallback;

  public ClientGlobalTransactionManagerImpl(final RemoteTransactionManager remoteTransactionManager,
                                            final PreTransactionFlushCallback preTransactionFlushCallback) {
    this.remoteTransactionManager = remoteTransactionManager;
    this.preTransactionFlushCallback = preTransactionFlushCallback;
  }

  // For testing
  public synchronized int size() {
    return this.applied.size();
  }

  // For testing
  public int getAllowedLowWaterMarkDelta() {
    return ALLOWED_LWM_DELTA;
  }

  public synchronized boolean startApply(final NodeID committerID, final TransactionID transactionID,
                                         final GlobalTransactionID gtxID, final NodeID remoteGroupID) {
    if (gtxID.lessThan(getLowGlobalTransactionIDWatermark())) {
      // formatting
      throw new UnknownTransactionError("Attempt to apply a transaction lower than the low watermark: gtxID = " + gtxID
                                        + ", low watermark = " + getLowGlobalTransactionIDWatermark());
    }
    final ServerTransactionID serverTransactionID = new ServerTransactionID(committerID, transactionID);
    this.globalTransactionIDs.put(gtxID, serverTransactionID);
    return this.applied.add(serverTransactionID);
  }

  public synchronized GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return this.lowWatermark;
  }

  public synchronized void setLowWatermark(final GlobalTransactionID lowWatermark, final NodeID nodeID) {
    if (this.lowWatermark.toLong() > lowWatermark.toLong()) {
      // XXX::This case is possible when the server crashes, Eventually the server will catch up
      logger.warn("Low water mark lower than exisiting one : mine : " + this.lowWatermark + " server sent : "
                  + lowWatermark);
      return;
    }
    if (this.lowWatermark.toLong() + ALLOWED_LWM_DELTA > lowWatermark.toLong()) {
      if (this.ignoredCount++ > ALLOWED_LWM_DELTA * 100) {
        logger.warn("Current Low water Mark = " + this.lowWatermark + " Server sent " + lowWatermark);
        logger.warn("Server didnt send a Low water mark higher than ALLOWED_LWM_DELTA for " + this.ignoredCount
                    + " times. applied.size() = " + this.applied.size() + " Resetting count.");
        this.ignoredCount = 0;
      }
      return;
    }
    this.ignoredCount = 0;
    this.lowWatermark = lowWatermark;
    final Map toDelete = this.globalTransactionIDs.headMap(lowWatermark);
    for (final Iterator i = toDelete.entrySet().iterator(); i.hasNext();) {
      final Entry e = (Entry) i.next();
      final ServerTransactionID stxID = (ServerTransactionID) e.getValue();
      i.remove();
      this.applied.remove(stxID);
    }
  }

  public void flush(final LockID lockID, boolean noLocksLeftOnClient) {
    if (noLocksLeftOnClient) {
      preTransactionFlushCallback.preTransactionFlush(lockID);
    }
    this.remoteTransactionManager.flush(lockID);
  }

  public void waitForServerToReceiveTxnsForThisLock(final LockID lock) {
    this.remoteTransactionManager.waitForServerToReceiveTxnsForThisLock(lock);
  }

  public boolean asyncFlush(final LockID lockID, final LockFlushCallback callback, boolean noLocksLeftOnClient) {
    if (noLocksLeftOnClient) {
      preTransactionFlushCallback.preTransactionFlush(lockID);
    }
    return this.remoteTransactionManager.asyncFlush(lockID, callback);
  }
}
