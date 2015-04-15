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
package com.tc.object.gtx;

import com.tc.abortable.AbortedOperationException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.properties.TCPropertiesImpl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClientGlobalTransactionManagerImpl implements
    ClientGlobalTransactionManager {

  private static final TCLogger             logger               = TCLogging
                                                                     .getLogger(ClientGlobalTransactionManagerImpl.class);

  private static final int                  ALLOWED_LWM_DELTA    = 100;
  private static final int                  LWM_IGNORE_MULTIPLIER = TCPropertiesImpl.getProperties()
                                                                      .getInt("lwm.ignore.multiplier", 100);
  private final Set                               applied              = new HashSet();
  private final SortedMap                         globalTransactionIDs = new TreeMap();

  private GlobalTransactionID               lowWatermark         = GlobalTransactionID.NULL_ID;
  private final RemoteTransactionManager    remoteTransactionManager;
  private int                               ignoredCount         = 0;

  public ClientGlobalTransactionManagerImpl(final RemoteTransactionManager remoteTransactionManager) {
    this.remoteTransactionManager = remoteTransactionManager;
  }

  @Override
  public synchronized void cleanup() {
    // remoteTxnManager will be cleanup from clientHandshakeCallbacks
    applied.clear();
    globalTransactionIDs.clear();
    lowWatermark = GlobalTransactionID.NULL_ID; // need to do this, discuss
    ignoredCount = 0;
  }

  // For testing
  @Override
  public synchronized int size() {
    return this.applied.size();
  }

  // For testing
  public int getAllowedLowWaterMarkDelta() {
    return ALLOWED_LWM_DELTA;
  }

  @Override
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

  @Override
  public synchronized GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return this.lowWatermark;
  }

  @Override
  public synchronized void setLowWatermark(final GlobalTransactionID lowWatermark, final NodeID nodeID) {
    if (this.lowWatermark.toLong() > lowWatermark.toLong()) {
      // XXX::This case is possible when the server crashes, Eventually the server will catch up
      logger.warn("Low water mark lower than exisiting one : mine : " + this.lowWatermark + " server sent : "
                  + lowWatermark);
      return;
    }
    if (this.lowWatermark.toLong() + ALLOWED_LWM_DELTA > lowWatermark.toLong()) {
      if (this.ignoredCount++ > ALLOWED_LWM_DELTA * LWM_IGNORE_MULTIPLIER) {
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

  @Override
  public void flush(final LockID lockID) throws AbortedOperationException {
    this.remoteTransactionManager.flush(lockID);
  }

  @Override
  public void waitForServerToReceiveTxnsForThisLock(final LockID lock) throws AbortedOperationException {
    this.remoteTransactionManager.waitForServerToReceiveTxnsForThisLock(lock);
  }

  @Override
  public boolean asyncFlush(final LockID lockID, final LockFlushCallback callback) {
    return this.remoteTransactionManager.asyncFlush(lockID, callback);
  }
}
