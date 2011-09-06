/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BroadcastTransactionMessage extends TCMessage {

  public void initialize(List chges, ObjectStringSerializer aSerializer, LockID[] lids, long cid, TransactionID txID,
                         NodeID commitID, GlobalTransactionID gtx, TxnType txnType,
                         GlobalTransactionID lowGlobalTransactionIDWatermark, Collection notifies, Map newRoots,
                         DmiDescriptor[] dmis);

  public List getLockIDs();

  public TxnType getTransactionType();

  public Collection getObjectChanges();

  public long getChangeID();

  public TransactionID getTransactionID();

  public NodeID getCommitterID();

  public GlobalTransactionID getGlobalTransactionID();

  public GlobalTransactionID getLowGlobalTransactionIDWatermark();

  public Collection addNotifiesTo(List c);

  public Map getNewRoots();

  public List getDmiDescriptors();

}