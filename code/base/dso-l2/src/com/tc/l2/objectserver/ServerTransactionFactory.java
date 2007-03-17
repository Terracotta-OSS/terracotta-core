/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.tx.ServerTransaction;

public class ServerTransactionFactory {

  private static long tid = 1;

  public static ServerTransaction createTxnFrom(ObjectSyncMessage syncMsg) {
    ObjectSyncServerTransaction txn = new ObjectSyncServerTransaction(getNextTransactionID(), syncMsg.getOids(),
                                                                      syncMsg.getDnaCount(), syncMsg.getSerializer(),
                                                                      syncMsg.getDNAs(), syncMsg.getRootsMap());
    return txn;
  }

  private static synchronized TransactionID getNextTransactionID() {
    return new TransactionID(tid++);
  }

}
