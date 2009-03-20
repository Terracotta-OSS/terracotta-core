/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.test.TCTestCase;

import java.util.HashMap;
import java.util.Map;

public class ServerTransactionIDTest extends TCTestCase {

  private ServerTransactionID get(int channel, int txn) {
    return new ServerTransactionID(new ClientID(channel), new TransactionID(txn));
  }

  public void test() {
    ServerTransactionID id1 = get(1, 1);
    ServerTransactionID id2 = get(2, 2);
    ServerTransactionID idNull = ServerTransactionID.NULL_ID;

    assertEquals(id1, get(1, 1));
    assertEquals(id2, get(2, 2));
    assertEquals(get(1, 1).hashCode(), id1.hashCode());
    assertEquals(get(2, 2).hashCode(), id2.hashCode());

    assertNotEquals(idNull, id1);
    assertNotEquals(idNull, id2);

    Map map = new HashMap();
    assertEquals(0, map.size());
    map.put(id1, "one");
    assertEquals(1, map.size());
    map.put(id2, "two");
    assertEquals(2, map.size());

    assertEquals("one", map.remove(id1));
    assertEquals("two", map.remove(id2));
    assertEquals(0, map.size());
  }

}
