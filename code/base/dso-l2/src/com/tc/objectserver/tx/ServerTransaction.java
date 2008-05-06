/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransaction;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an atomic change to the states of objects on the server
 *
 * @author steve
 */

public interface ServerTransaction extends GlobalTransaction {

  public TxnBatchID getBatchID();

  public ObjectStringSerializer getSerializer();

  public LockID[] getLockIDs();

  public NodeID getSourceID();

  public TransactionID getTransactionID();

  public ServerTransactionID getServerTransactionID();

  public List getChanges();

  public Map getNewRoots();

  public TxnType getTransactionType();

  public Set getObjectIDs();

  public Set getNewObjectIDs();

  public Collection getNotifies();

  public DmiDescriptor[] getDmiDescriptors();

  public boolean needsBroadcast();

  /**
   * Number of actual client/application transactions that this server transaction contains. Txn folding on the client
   * might make the relationship not 1:1
   */
  public int getNumApplicationTxn();

}
