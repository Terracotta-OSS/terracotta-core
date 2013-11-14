/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.license.ProductID;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class ConnectionIDTest extends TestCase {
  public void testSerializeFullyPopulated() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", "abcd", "abcd".toCharArray(), ProductID.USER);
    checkSerializeDeserialize(id);
  }

  public void testSerializeEmptyPassword() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", "abcd", null, ProductID.TMS);
    checkSerializeDeserialize(id);
  }

  public void testSerializeEmptyUsername() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", null, "abcd".toCharArray(), ProductID.TMS);
    checkSerializeDeserialize(id);
  }

  public void testNoCredentials() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", null, null, ProductID.TMS);
    checkSerializeDeserialize(id);
  }

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
