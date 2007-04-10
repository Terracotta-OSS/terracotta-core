/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.object.tx.ServerTransactionID;

import java.util.SortedMap;
import java.util.TreeMap;

public class DefaultGlobalTransactionIDGenerator implements GlobalTransactionIDGenerator {

  SortedMap map = new TreeMap(GlobalTransactionID.COMPARATOR);
  long      id  = 0;

  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {

    GlobalTransactionID gid = (GlobalTransactionID) map.get(serverTransactionID);
    if (gid == null) {
      gid = new GlobalTransactionID(id++);
      map.put(serverTransactionID, gid);
    }
    return gid;
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    if (map.isEmpty()) {
      return GlobalTransactionID.NULL_ID;
    } else {
      GlobalTransactionID lowWaterMark = (GlobalTransactionID) map.firstKey();
      return lowWaterMark;
    }
  }
}
