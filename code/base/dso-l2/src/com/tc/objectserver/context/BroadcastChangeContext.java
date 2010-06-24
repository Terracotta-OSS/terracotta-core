/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.List;
import java.util.Map;

/**
 * Context need to broadcast the transaction to the interested nodes
 *
 * @author steve
 */
public class BroadcastChangeContext implements EventContext {
  private final ServerTransaction   tx;
  private final GlobalTransactionID lowGlobalTransactionIDWatermark;
  private final NotifiedWaiters     notifiedWaiters;
  private final ApplyTransactionInfo      applyInfo;

  public BroadcastChangeContext(ServerTransaction tx,
                                GlobalTransactionID lowGlobalTransactionIDWatermark, NotifiedWaiters notifiedWaiters,
                                ApplyTransactionInfo applyInfo) {
    this.tx = tx;
    this.lowGlobalTransactionIDWatermark = lowGlobalTransactionIDWatermark;
    this.notifiedWaiters = notifiedWaiters;
    this.applyInfo = applyInfo;
  }

  public ApplyTransactionInfo getApplyInfo() {
    return applyInfo;
  }

  public List getChanges() {
    return tx.getChanges();
  }

  public LockID[] getLockIDs() {
    return tx.getLockIDs();
  }

  public NodeID getNodeID() {
    return tx.getSourceID();
  }

  public TransactionID getTransactionID() {
    return tx.getTransactionID();
  }
  
  public TxnBatchID getBatchID() {
    return tx.getBatchID();
  }

  public TxnType getTransactionType() {
    return tx.getTransactionType();
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return tx.getGlobalTransactionID();
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return this.lowGlobalTransactionIDWatermark;
  }

  public ObjectStringSerializer getSerializer() {
    return tx.getSerializer();
  }

  public NotifiedWaiters getNewlyPendingWaiters() {
    return notifiedWaiters;
  }
  
  public Map getNewRoots() {
    return tx.getNewRoots();
  }
  
  public DmiDescriptor[] getDmiDescriptors() {
    return tx.getDmiDescriptors();
  }

}
