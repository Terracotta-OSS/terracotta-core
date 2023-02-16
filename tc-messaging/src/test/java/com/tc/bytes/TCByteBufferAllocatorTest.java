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
package com.tc.bytes;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TCByteBufferAllocatorTest {

  @Test
  public void testAddAndComplete() {
    Supplier<TCByteBuffer> bufsrc = mock(Supplier.class);
    when(bufsrc.get()).thenReturn(TCByteBufferFactory.getInstance(512));
    Queue<TCByteBuffer> returns = new LinkedList<>();
    TCByteBufferAllocator alloc = new TCByteBufferAllocator(bufsrc, returns);
    TCByteBuffer check = alloc.add();
    TCReference ref = alloc.complete();
    ref.close();
    verify(bufsrc).get();
    assertEquals(returns.size(), 1);
    assertEquals(returns.poll(), check);
    try {
      alloc.add();
      throw new AssertionError("should have failed with illegal state");
    } catch (Exception e) {
      // expected
    }
  }
}
