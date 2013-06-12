/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TxnObjectGrouping implements PrettyPrintable {
  private static final int MAX_TXN_COUNT = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_OBJECTMANAGER_MAXTXNS_INTXNOBJECT_GROUPING);

  // Turns out Object.hasCode() is a bit pricey in terms of CPU time. Just using a simple counter to create "hashes"
  // This way the object groupings will be round-robinned around about the apply threads.
  private static int nextHash = 0;

  private int addedTxns = 0;
  private ServerTransactionID txnID;
  private boolean closed;
  private final Set<ServerTransactionID>  txns = new HashSet<ServerTransactionID>();
  private final Map<ObjectID, ManagedObject> objects = new HashMap<ObjectID, ManagedObject>();
  private final int hashCode = nextHash++;

  public TxnObjectGrouping(ServerTransactionID stxID) {
    addServerTransactionID(stxID);
  }

  private void checkClosed() {
    if (closed) {
      throw new IllegalStateException("Grouping is closed.");
    }
  }

  public synchronized boolean addServerTransactionID(ServerTransactionID stxID) {
    if (closed || ++addedTxns > MAX_TXN_COUNT) {
      return false;
    }
    if (txnID == null) {
      txnID = stxID;
    }
    txns.add(stxID);
    return true;
  }

  public synchronized ServerTransactionID getServerTransactionID() {
    return txnID;
  }

  public synchronized boolean containsAll(Collection<ObjectID> oids) {
    for (ObjectID oid : oids) {
      if (!objects.containsKey(oid)) {
        return false;
      }
    }
    return true;
  }

  public synchronized Map<ObjectID, ManagedObject> getObjects(Collection<ObjectID> oids) {
    checkClosed();
    Map<ObjectID, ManagedObject> managedObjectMap = new HashMap<ObjectID, ManagedObject>(oids.size());
    for (ObjectID oid : oids) {
      ManagedObject mo = objects.get(oid);
      if (mo == null) {
        throw new IllegalArgumentException("Missing object " + oid);
      }
      managedObjectMap.put(oid, mo);
    }
    return managedObjectMap;
  }

  public synchronized void addObject(ObjectID oid, ManagedObject mo) {
    objects.put(oid, mo);
  }

  public synchronized boolean transactionComplete(ServerTransactionID stxID) {
    checkClosed();
    if (!txns.remove(stxID)) {
      throw new IllegalArgumentException("Transaction " + stxID + " is not part of this transaction grouping.");
    }
    if (txns.isEmpty()) {
      closed = true;
      return true;
    } else {
      return false;
    }
  }

  public synchronized Collection<ManagedObject> getObjects() {
    return new ArrayList<ManagedObject>(objects.values());
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println("TransactionGrouping@" + System.identityHashCode(this));
    out.indent().println("txnID: ").visit(txnID).println();
    out.indent().println("objects: ").visit(objects.keySet()).println();
    return out;
  }

  @Override
  public synchronized String toString() {
    StringBuilder out = new StringBuilder();
    out.append("TransactionGrouping@").append(System.identityHashCode(this)).append("\n");
    out.append("\t").append("txnID: ").append(txnID).append("\n");
    out.append("\t").append("objects: ").append(objects).append("\n");
    return out.toString();
  }
}