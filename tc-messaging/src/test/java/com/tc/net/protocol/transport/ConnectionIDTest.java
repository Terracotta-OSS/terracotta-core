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
package com.tc.net.protocol.transport;

import java.io.IOException;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.ProductID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConnectionIDTest {
  
  @Test
  public void testSerializeFullyPopulated() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", ProductID.STRIPE);
    checkSerializeDeserialize(id);
  }

  @Test
  public void testSerializeEmptyPassword() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", ProductID.DIAGNOSTIC);
    checkSerializeDeserialize(id);
  }

  @Test
  public void testSerializeEmptyUsername() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", ProductID.DIAGNOSTIC);
    checkSerializeDeserialize(id);
  }

  @Test
  public void testNoCredentials() throws Exception {
    ConnectionID id = new ConnectionID("abcd", 1, "abcd", ProductID.DIAGNOSTIC);
    checkSerializeDeserialize(id);
  }

  @SuppressWarnings("resource")
  private static void checkSerializeDeserialize(ConnectionID id) throws IOException {
    TCByteBufferOutputStream outputStream = new TCByteBufferOutputStream();
    id.writeTo(outputStream);
    try (TCByteBufferInputStream inputStream = new TCByteBufferInputStream(outputStream.accessBuffers())) {
      ConnectionID after = ConnectionID.readFrom(inputStream);
      assertConnectionIDsEqual(id, after);
    }
  }

  private static void assertConnectionIDsEqual(ConnectionID expected, ConnectionID actual) {
    assertEquals(expected.getJvmID(), actual.getJvmID());
    assertEquals(expected.getChannelID(), actual.getChannelID());
    assertEquals(expected.getServerID(), actual.getServerID());
    assertEquals(expected.getProductId(), actual.getProductId());
  }
}
