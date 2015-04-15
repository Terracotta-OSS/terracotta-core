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
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ResentServerTransaction implements ServerTransaction {

  private final ServerTransaction orgTxn;

  public ResentServerTransaction(ServerTransaction wrapped) {
    orgTxn = wrapped;
  }

  @Override
  public void setGlobalTransactionID(GlobalTransactionID gid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID() {
    return orgTxn.getGlobalTransactionID();
  }

  @Override
  public SequenceID getClientSequenceID() {
    return orgTxn.getClientSequenceID();
  }

  @Override
  public TxnBatchID getBatchID() {
    return orgTxn.getBatchID();
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    return orgTxn.getSerializer();
  }

  @Override
  public LockID[] getLockIDs() {
    return orgTxn.getLockIDs();
  }

  @Override
  public NodeID getSourceID() {
    return orgTxn.getSourceID();
  }

  @Override
  public TransactionID getTransactionID() {
    return orgTxn.getTransactionID();
  }

  @Override
  public ServerTransactionID getServerTransactionID() {
    return orgTxn.getServerTransactionID();
  }

  @Override
  public List getChanges() {
    return orgTxn.getChanges();
  }

  @Override
  public Map getNewRoots() {
    return orgTxn.getNewRoots();
  }

  @Override
  public TxnType getTransactionType() {
    return orgTxn.getTransactionType();
  }

  @Override
  public ObjectIDSet getObjectIDs() {
    return orgTxn.getObjectIDs();
  }

  @Override
  public ObjectIDSet getNewObjectIDs() {
    return orgTxn.getNewObjectIDs();
  }

  @Override
  public Collection getNotifies() {
    return orgTxn.getNotifies();
  }

  @Override
  public MetaDataReader[] getMetaDataReaders() {
    return orgTxn.getMetaDataReaders();
  }

  @Override
  public boolean isActiveTxn() {
    return orgTxn.isActiveTxn();
  }

  @Override
  public boolean isResent() {
    return true;
  }

  @Override
  public int getNumApplicationTxn() {
    return orgTxn.getNumApplicationTxn();
  }

  @Override
  public long[] getHighWaterMarks() {
    return orgTxn.getHighWaterMarks();
  }

  @Override
  public boolean isSearchEnabled() {
    return orgTxn.isSearchEnabled();
  }

  @Override
  public boolean isEviction() {
    return false;
  }

}
