/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.invalidation.Invalidations;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.locks.ClientServerExchangeLockContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ClientHandshakeMessage extends TCMessage {

  public List getTransactionSequenceIDs();

  public Set getObjectIDs();

  public Invalidations getObjectIDsToValidate();

  public void addLockContext(ClientServerExchangeLockContext ctxt);

  public Collection<ClientServerExchangeLockContext> getLockContexts();

  public void setClientVersion(String v);

  public String getClientVersion();

  public void addTransactionSequenceIDs(List transactionSequenceIDs);

  public void addResentTransactionIDs(List resentTransactionIDs);

  public List getResentTransactionIDs();

  public void setIsObjectIDsRequested(boolean request);

  public boolean isObjectIDsRequested();

  public void setServerHighWaterMark(long serverHighWaterMark);

  public long getServerHighWaterMark();

  public void setEnterpriseClient(boolean isEnterpirseClient);

  public boolean enterpriseClient();

  public long getLocalTimeMills();

}