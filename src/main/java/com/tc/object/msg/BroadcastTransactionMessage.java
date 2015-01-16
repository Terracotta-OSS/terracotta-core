/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.server.ServerEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BroadcastTransactionMessage extends TCMessage {

  void initialize(List<? extends DNA> prunedChanges, ObjectStringSerializer aSerializer, LockID[] lids, long cid, TransactionID txID,
                  NodeID commitID, GlobalTransactionID gtx, TxnType txnType,
                  GlobalTransactionID lowGlobalTransactionIDWatermark, Collection<ClientServerExchangeLockContext> notifies, Map<String, ObjectID> newRoots,
                  Map<LogicalChangeID, LogicalChangeResult> logicalInvokeResults,
                  Collection<ServerEvent> events);

  List<LockID> getLockIDs();

  TxnType getTransactionType();

  Collection<DNA> getObjectChanges();

  long getChangeID();

  TransactionID getTransactionID();

  NodeID getCommitterID();

  GlobalTransactionID getGlobalTransactionID();

  GlobalTransactionID getLowGlobalTransactionIDWatermark();

  Collection<ClientServerExchangeLockContext> getNotifies();

  Map<String, ObjectID> getNewRoots();

  List<ServerEvent> getEvents();
}
