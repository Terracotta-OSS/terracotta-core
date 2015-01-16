/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.tx.TransactionID;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.List;

public interface ClientHandshakeMessage extends TCMessage {

  List<SequenceID> getTransactionSequenceIDs();

  ObjectIDSet getObjectIDs();

  void setObjectIDs(ObjectIDSet objectIDs);

  ObjectIDSet getObjectIDsToValidate();

  void setObjectIDsToValidate(ObjectIDSet objectIDsToValidate);

  void addLockContext(ClientServerExchangeLockContext ctxt);

  Collection<ClientServerExchangeLockContext> getLockContexts();

  void setClientVersion(String v);

  String getClientVersion();

  void addTransactionSequenceIDs(List<SequenceID> transactionSequenceIDs);

  void addResentTransactionIDs(List<TransactionID> resentTransactionIDs);

  List<TransactionID> getResentTransactionIDs();

  void setIsObjectIDsRequested(boolean request);

  boolean isObjectIDsRequested();

  void setServerHighWaterMark(long serverHighWaterMark);

  long getServerHighWaterMark();

  void setEnterpriseClient(boolean isEnterpirseClient);

  boolean enterpriseClient();

  long getLocalTimeMills();

}
