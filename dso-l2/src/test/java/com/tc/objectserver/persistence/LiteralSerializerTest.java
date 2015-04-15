/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.persistence;

import com.tc.test.TCTestCase;

import java.nio.ByteBuffer;

/**
 * @author tim
 */
public class LiteralSerializerTest extends TCTestCase {
  public void testNestedStrings() throws Exception {
    ByteBuffer s1 = LiteralSerializer.INSTANCE.transform("foo");
    ByteBuffer s2 = LiteralSerializer.INSTANCE.transform("bar");
    ByteBuffer combined = ByteBuffer.allocate(s1.remaining() + s2.remaining());
    combined.put(s1).put(s2).flip();
    assertEquals("foo", LiteralSerializer.INSTANCE.recover(combined));
    assertEquals("bar", LiteralSerializer.INSTANCE.recover(combined));
  }
}
