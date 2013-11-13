/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.license.ProductID;

import java.util.Arrays;

import junit.framework.TestCase;

public class ConnectionIDTest extends TestCase {
  public void testSerialization() throws Exception {
    ConnectionID before = new ConnectionID("abcd", 1, "abcd", "abcd", "abcd".toCharArray(), ProductID.USER);
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream();
    before.writeTo(outputStream);
    TCByteBufferInputStream inputStream = new TCByteBufferInputStream(outputStream.toArray());
    ConnectionID after = ConnectionID.readFrom(inputStream);
    assertEquals(before.getJvmID(), after.getJvmID());
    assertEquals(before.getChannelID(), after.getChannelID());
    assertEquals(before.getServerID(), after.getServerID());
    assertEquals(before.getUsername(), after.getUsername());
    assertTrue(Arrays.equals(before.getPassword(), after.getPassword()));
    assertEquals(before.getProductId(), after.getProductId());
  }
}
