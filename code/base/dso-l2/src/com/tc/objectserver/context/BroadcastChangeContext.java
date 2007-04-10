/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.managedobject.BackReferences;
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
  private final BackReferences      includeIDs;

  public BroadcastChangeContext(ServerTransaction tx,
                                GlobalTransactionID lowGlobalTransactionIDWatermark, NotifiedWaiters notifiedWaiters,
                                BackReferences includeIDs) {
    this.tx = tx;
    this.lowGlobalTransactionIDWatermark = lowGlobalTransactionIDWatermark;
    this.notifiedWaiters = notifiedWaiters;
    this.includeIDs = includeIDs;
  }

  public BackReferences getIncludeIDs() {
    return includeIDs;
  }

  public List getChanges() {
    return tx.getChanges();
  }

  public LockID[] getLockIDs() {
    return tx.getLockIDs();
  }

  public ChannelID getChannelID() {
    return tx.getChannelID();
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
