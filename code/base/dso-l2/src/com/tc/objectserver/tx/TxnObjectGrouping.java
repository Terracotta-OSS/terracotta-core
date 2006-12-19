/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.tx.ServerTransactionID;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TxnObjectGrouping implements PrettyPrintable {

  private final ServerTransactionID txID;
  private final Set                 txns          = new HashSet();
  private final Set                 pendingApplys = new HashSet();
  private final Map                 objects;
  private final Map                 newRootsMap;

  public TxnObjectGrouping(ServerTransactionID sTxID, Map newRootsMap) {
    this.txID = sTxID;
    this.newRootsMap = newRootsMap;
    this.txns.add(sTxID);
    this.pendingApplys.add(sTxID);
    this.objects = new HashMap();
  }

  public TxnObjectGrouping(Map lookedupObjects) {
    this.txID = ServerTransactionID.NULL_ID;
    this.newRootsMap = Collections.EMPTY_MAP;
    objects = lookedupObjects;
  }

  public boolean applyComplete(ServerTransactionID txnId) {
    Assert.assertTrue(pendingApplys.remove(txnId));
    return pendingApplys.isEmpty();
  }

  public ServerTransactionID getServerTransactionID() {
    return txID;
  }

  public Map getObjects() {
    // System.err.println(System.identityHashCode(this) + " : Callign getObjects: " + objects);
    return objects;
  }

  public Map getNewRoots() {
    return newRootsMap;
  }

  public void merge(TxnObjectGrouping oldGrouping) {
    Assert.assertTrue(this.txID != ServerTransactionID.NULL_ID);
    txns.addAll(oldGrouping.txns);
    objects.putAll(oldGrouping.objects);
    pendingApplys.addAll(oldGrouping.pendingApplys);
    newRootsMap.putAll(oldGrouping.newRootsMap);
  }

  public int hashCode() {
    return txID.hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof TxnObjectGrouping) {
      TxnObjectGrouping other = (TxnObjectGrouping) o;
      return (txID.equals(other.txID));
    }
    return false;
  }

  public Collection getApplyPendingTxns() {
    return pendingApplys;
  }

  public Collection getTxnIDs() {
    return txns;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println("TransactionGrouping@"+ System.identityHashCode(this));
    out.indent().println("txnID: ").visit(txID).println();
    out.indent().println("txns: ").visit(txns).println();
    out.indent().println("objects: ").visit(objects.keySet()).println();
    out.indent().println("pendingApplys: ").visit(pendingApplys).println();
    out.indent().println("newRootsMap: ").visit(newRootsMap).println();
    return out;
  }
  
  public String toString() {
    StringBuffer out = new StringBuffer();
    out.append("TransactionGrouping@"+ System.identityHashCode(this)).append("\n");
    out.append("\t").append("txnID: ").append(txID).append("\n");
    out.append("\t").append("txns: ").append(txns).append("\n");
    out.append("\t").append("objects: ").append(objects.keySet()).append("\n");
    out.append("\t").append("pendingApplys: ").append(pendingApplys).append("\n");
    out.append("\t").append("newRootsMap: ").append(newRootsMap).append("\n");
    return out.toString();
  }
  

}