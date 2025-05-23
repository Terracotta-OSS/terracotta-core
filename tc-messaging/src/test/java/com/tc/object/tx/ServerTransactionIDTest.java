/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.tx;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.tc.net.ClientID;
import com.tc.net.ServerID;
import org.junit.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ServerTransactionIDTest {

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

  @Test
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

    Map<ServerTransactionID, String> map = new HashMap<ServerTransactionID, String>();
    assertEquals(0, map.size());
    map.put(id1, "one");
    assertEquals(1, map.size());
    map.put(id2, "two");
    assertEquals(2, map.size());

    assertEquals("one", map.remove(id1));
    assertEquals("two", map.remove(id2));
    assertEquals(0, map.size());
  }

  @Test
  public void testSerialization() throws Exception {
    ServerTransactionID id1 = get(1, 1);
    ServerTransactionID id2 = getServerID((byte) 2, 2);

    serializeAndCompare(id1);
    serializeAndCompare(id2);

    Random e = new Random();
    for (int i = 1; i <= 10; i++) {
      serializeAndCompare(getServerID(e.nextInt(1099), i));
    }
  }

  @Test
  public void testValidTransactions() throws Exception {
    Assert.assertTrue(new TransactionID(0L).isValid());
    Assert.assertTrue(new TransactionID(1L).isValid());
    Assert.assertTrue(new TransactionID(Long.MAX_VALUE).isValid());
    Assert.assertFalse(new TransactionID(Long.MIN_VALUE).isValid());
    Assert.assertFalse(TransactionID.NULL_ID.isValid());
    Assert.assertTrue(TransactionID.NULL_ID.isNull());
  }
  
  private void serializeAndCompare(ServerTransactionID id) {
    byte[] b2 = id.getBytes();
    ServerTransactionID di2 = ServerTransactionID.createFrom(b2);
    assertEquals(id, di2);
  }

}
