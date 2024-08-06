/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
