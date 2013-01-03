/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TxnObjectGrouping implements PrettyPrintable {

  private ServerTransactionID             txID;
  private Map<ObjectID, ManagedObject>    objects;
  private Map<String, ObjectID>           newRootsMap;

  public TxnObjectGrouping(ServerTransactionID sTxID, Map<String, ObjectID> newRootsMap, Map<ObjectID, ManagedObject> objects) {
    this.txID = sTxID;
    if (newRootsMap.isEmpty()) {
      this.newRootsMap = Collections.EMPTY_MAP;
    } else {
      this.newRootsMap = new HashMap<String, ObjectID>(newRootsMap);
    }
    this.objects = objects;
  }

  public ServerTransactionID getServerTransactionID() {
    return txID;
  }

  public Map<ObjectID, ManagedObject> getObjects() {
    return objects;
  }

  public Map<String, ObjectID> getNewRoots() {
    return newRootsMap;
  }

  @Override
  public int hashCode() {
    return txID.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TxnObjectGrouping) {
      TxnObjectGrouping other = (TxnObjectGrouping) o;
      return (txID.equals(other.txID));
    }
    return false;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println("TransactionGrouping@" + System.identityHashCode(this));
    out.indent().println("txnID: ").visit(txID).println();
    out.indent().println("objects: ").visit(objects.keySet()).println();
    out.indent().println("newRootsMap: ").visit(newRootsMap).println();
    return out;
  }

  @Override
  public String toString() {
    StringBuffer out = new StringBuffer();
    out.append("TransactionGrouping@" + System.identityHashCode(this)).append("\n");
    out.append("\t").append("txnID: ").append(txID).append("\n");
    out.append("\t").append("objects: ").append(objects).append("\n");
    out.append("\t").append("newRootsMap: ").append(newRootsMap).append("\n");
    return out.toString();
  }
}