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
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.TransactionStore;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.gtx.TransactionCommittedError;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.Sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestTransactionStore implements TransactionStore {

  public final NoExceptionLinkedQueue                                 leastContextQueue             = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue                                 commitContextQueue            = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue                                 loadContextQueue              = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue                                 nextTransactionIDContextQueue = new NoExceptionLinkedQueue();

  private final Map<ServerTransactionID, GlobalTransactionDescriptor> volatileMap                   = new HashMap<ServerTransactionID, GlobalTransactionDescriptor>();
  private final SortedSet<GlobalTransactionID>                        ids                           = new TreeSet<GlobalTransactionID>();
  private final Map<ServerTransactionID, GlobalTransactionDescriptor> durableMap                    = new HashMap<ServerTransactionID, GlobalTransactionDescriptor>();
  public final Sequence                                               idSequence;

  public TestTransactionStore(Sequence sequence) {
    this.idSequence = sequence;
  }

  public void restart() throws Exception {
    volatileMap.clear();
    ids.clear();
    volatileMap.putAll(durableMap);
    for (Object element : volatileMap.values()) {
      GlobalTransactionDescriptor gdesc = (GlobalTransactionDescriptor) element;
      GlobalTransactionID gid = gdesc.getGlobalTransactionID();
      ids.add(gid);
    }
  }

  @Override
  public GlobalTransactionDescriptor getOrCreateTransactionDescriptor(ServerTransactionID stxid) {
    GlobalTransactionDescriptor rv = volatileMap.get(stxid);
    if (rv == null) {
      nextTransactionIDContextQueue.put(stxid);
      rv = new GlobalTransactionDescriptor(stxid, new GlobalTransactionID(idSequence.next()));
      basicPut(volatileMap, rv);
      ids.add(rv.getGlobalTransactionID());
    }
    return rv;
  }

  private void basicPut(Map<ServerTransactionID, GlobalTransactionDescriptor> map, GlobalTransactionDescriptor txID) {
    map.put(txID.getServerTransactionID(), txID);
  }

  @Override
  public void commitTransactionDescriptor(ServerTransactionID stxID) {
    GlobalTransactionDescriptor txID = getTransactionDescriptor(stxID);
    if (txID.isCommitted()) { throw new TransactionCommittedError("Already committed : " + txID); }
    try {
      commitContextQueue.put(new Object[] { txID });
      if (!volatileMap.containsValue(txID)) throw new AssertionError();
      basicPut(durableMap, txID);
      txID.commitComplete();
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID stxid) {
    try {
      loadContextQueue.put(stxid);
      return volatileMap.get(stxid);
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public GlobalTransactionID getLeastGlobalTransactionID() {
    leastContextQueue.put(new Object());
    if (ids.isEmpty()) {
      return GlobalTransactionID.NULL_ID;
    } else {
      return ids.first();
    }
  }

  @Override
  public Collection<GlobalTransactionDescriptor> clearCommitedTransactionsBelowLowWaterMark(ServerTransactionID lowWaterMark) {
    List<GlobalTransactionDescriptor> removedGDs = new ArrayList<GlobalTransactionDescriptor>();
    for (final Entry<ServerTransactionID, GlobalTransactionDescriptor> e : volatileMap
        .entrySet()) {
      ServerTransactionID sid = e.getKey();
      if (sid.getSourceID().equals(lowWaterMark.getSourceID())) {
        GlobalTransactionDescriptor gdesc = e.getValue();
        if (gdesc.getClientTransactionID().toLong() < lowWaterMark.getClientTransactionID().toLong()) {
          ids.remove(gdesc.getGlobalTransactionID());
          durableMap.remove(sid);
          removedGDs.add(gdesc);
        }
      }
    }
    return removedGDs;
  }

  @Override
  public GlobalTransactionDescriptor clearCommittedTransaction(final ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor descriptor = volatileMap.remove(serverTransactionID);
    if (descriptor != null) {
      durableMap.remove(serverTransactionID);
      ids.remove(descriptor.getGlobalTransactionID());
    }
    return descriptor;
  }

  @Override
  public void shutdownNode(NodeID nid) {
    throw new ImplementMe();
  }

  @Override
  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    GlobalTransactionDescriptor rv = new GlobalTransactionDescriptor(stxnID, globalTransactionID);
    basicPut(volatileMap, rv);
  }

  @Override
  public void shutdownAllClientsExcept(Set cids) {
    throw new ImplementMe();
  }

  @Override
  public void clearCommitedTransactionsBelowLowWaterMark(GlobalTransactionID lowWaterMark) {
    throw new ImplementMe();
  }

}