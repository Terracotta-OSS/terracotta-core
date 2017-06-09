/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import java.io.IOException;
import java.util.Arrays;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.util.ProductID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectionIDTest {
  
  @Test
  public void testSerializeFullyPopulated() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", "abcd", "abcd".toCharArray(), ProductID.STRIPE);
    checkSerializeDeserialize(id);
  }

  @Test
  public void testSerializeEmptyPassword() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", "abcd", null, ProductID.DIAGNOSTIC);
    checkSerializeDeserialize(id);
  }

  @Test
  public void testSerializeEmptyUsername() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", null, "abcd".toCharArray(), ProductID.DIAGNOSTIC);
    checkSerializeDeserialize(id);
  }

  @Test
  public void testNoCredentials() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", null, null, ProductID.DIAGNOSTIC);
    checkSerializeDeserialize(id);
  }

  @SuppressWarnings("resource")
  private static void checkSerializeDeserialize(ConnectionID id) throws IOException {
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream();
    id.writeTo(outputStream);
    TCByteBufferInputStream inputStream = new TCByteBufferInputStream(outputStream.toArray());
    ConnectionID after = ConnectionID.readFrom(inputStream);
    assertConnectionIDsEqual(id, after);
  }

  private static void assertConnectionIDsEqual(ConnectionID expected, ConnectionID actual) {
    assertEquals(expected.getJvmID(), actual.getJvmID());
    assertEquals(expected.getChannelID(), actual.getChannelID());
    assertEquals(expected.getServerID(), actual.getServerID());
    assertEquals(expected.getUsername(), actual.getUsername());
    assertTrue(Arrays.equals(expected.getPassword(), actual.getPassword()));
    assertEquals(expected.getProductId(), actual.getProductId());
  }
}
