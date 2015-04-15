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
package com.tc.objectserver.context;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TxnObjectGrouping;
import com.tc.util.BitSetObjectIDSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApplyTransactionContext implements MultiThreadedEventContext {

  private final TxnObjectGrouping grouping;
  private final ServerTransaction txn;
  private final boolean needsApply;
  private final Collection<ObjectID> ignoredObjects;

  public ApplyTransactionContext(ServerTransaction txn, TxnObjectGrouping grouping, boolean needsApply,
                                 Collection<ObjectID> ignoredObjects) {
    this.txn = txn;
    this.needsApply = needsApply;
    this.grouping = grouping;
    this.ignoredObjects = ignoredObjects;
  }

  public Map<ObjectID, ManagedObject> getObjects() {
    Set<ObjectID> oids = new BitSetObjectIDSet(txn.getObjectIDs());
    oids.removeAll(ignoredObjects);
    return grouping.getObjects(oids);
  }

  public Set<ObjectID> allCheckedOutObjects() {
    Set<ObjectID> objects = new HashSet<ObjectID>();
    for (ManagedObject managedObject : grouping.getObjects()) {
      objects.add(managedObject.getID());
    }
    return objects;
  }

  public ServerTransaction getTxn() {
    return txn;
  }

  public boolean needsApply() {
    return needsApply;
  }

  public Collection<ObjectID> getIgnoredObjects() {
    return ignoredObjects;
  }

  @Override
  public Object getKey() {
    return grouping;
  }

}
