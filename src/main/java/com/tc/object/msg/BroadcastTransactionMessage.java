/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.tx.TransactionID;
import com.tc.server.ServerEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BroadcastTransactionMessage extends TCMessage, Recyclable {

  void initialize(List<? extends DNA> prunedChanges, ObjectStringSerializer aSerializer, TransactionID txID,
                  NodeID commitID, GlobalTransactionID gtx,
                  GlobalTransactionID lowGlobalTransactionIDWatermark, Collection<ClientServerExchangeLockContext> notifies,
                  Map<LogicalChangeID, LogicalChangeResult> logicalInvokeResults,
                  Collection<ServerEvent> events);

  Collection<DNA> getObjectChanges();

  TransactionID getTransactionID();

  NodeID getCommitterID();

  GlobalTransactionID getGlobalTransactionID();

  GlobalTransactionID getLowGlobalTransactionIDWatermark();

  Collection<ClientServerExchangeLockContext> getNotifies();

  List<ServerEvent> getEvents();

  Map<LogicalChangeID, LogicalChangeResult> getLogicalChangeResults();
}
