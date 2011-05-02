/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.test.TCTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ServerTransactionIDTest extends TCTestCase {

  private ServerTransactionID get(int channel, int txn) {
    return new ServerTransactionID(new ClientID(channel), new TransactionID(txn));
  }

  private ServerTransactionID getServerID(byte b, int txn) {
    return new ServerTransactionID(new ServerID("aaabbbcccdffkdjgkrgjhfjghrjgruirbgjrbgjkrbgjnfjkn", new byte[] { 0, 1,
        23, 4, 5, 6, 7, 7, 8, 9, b }), new TransactionID(txn));
  }

  private ServerTransactionID getServerID(int size, int txn) {
    return new ServerTransactionID(new ServerID(createStr(size), createByteArray(size)), new TransactionID(txn));
  }

  private byte[] createByteArray(int size) {
    byte b[] = new byte[size];
    while (--size > 0) {
      b[size] = '5';
    }
    return b;
  }

  private String createStr(int size) {
    StringBuilder sb = new StringBuilder();
    while (size-- > 0) {
      sb.append('a');
    }
    return sb.toString();
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

  public void testSerialization() throws Exception {
    ServerTransactionID id1 = get(1, 1);
    ServerTransactionID id2 = getServerID((byte) 2, 2);

    serializeAndCompare(id1);
    serializeAndCompare(id2);

    Random e = new Random();
    for (int i = 0; i < 10; i++) {
      serializeAndCompare(getServerID(e.nextInt(1099), i));
    }
  }

  private void serializeAndCompare(ServerTransactionID id) {
    byte[] b2 = id.getBytes();
    System.out.println(" size : " + b2.length);
    ServerTransactionID di2 = ServerTransactionID.createFrom(b2);
    assertEquals(id, di2);
  }

}
