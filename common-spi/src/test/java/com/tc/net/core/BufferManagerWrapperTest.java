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
package com.tc.net.core;

import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class BufferManagerWrapperTest {
  
  public BufferManagerWrapperTest() {
  }

  /**
   */
  @Test
  public void testWriteFromZeroInZeroOut() throws Exception {
    ByteBuffer[] ref = new ByteBuffer[0];
    BufferManager buffer = mock(BufferManager.class);
    when(buffer.forwardToWriteBuffer(any())).thenReturn(0);
    BufferManagerWrapper instance = new BufferManagerWrapper(buffer);
    SocketEndpoint.ResultType result = instance.writeFrom(ref);
    assertEquals(SocketEndpoint.ResultType.ZERO, result);
  }
  /**
   */
  @Test
  public void testWriteFromBytesInZeroOut() throws Exception {
    ByteBuffer[] ref = new ByteBuffer[] {ByteBuffer.allocate(32)};
    BufferManager buffer = mock(BufferManager.class);
    when(buffer.forwardToWriteBuffer(any())).thenReturn(0);
    when(buffer.sendFromBuffer()).thenReturn(0);
    BufferManagerWrapper instance = new BufferManagerWrapper(buffer);
    SocketEndpoint.ResultType result = instance.writeFrom(ref);
    assertEquals(SocketEndpoint.ResultType.ZERO, result);
  }
  /**
   */
  @Test
  public void testWriteFromBytesInBytesOut() throws Exception {
    ByteBuffer[] ref = new ByteBuffer[] {ByteBuffer.allocate(32)};
    BufferManager buffer = mock(BufferManager.class);
    when(buffer.forwardToWriteBuffer(any())).thenAnswer(a->{
      ByteBuffer target = (ByteBuffer)a.getArgument(0);
      target.position(target.limit());
      return 32;
    });
    when(buffer.sendFromBuffer()).thenReturn(32);
    BufferManagerWrapper instance = new BufferManagerWrapper(buffer);
    SocketEndpoint.ResultType result = instance.writeFrom(ref);
    assertEquals(SocketEndpoint.ResultType.SUCCESS, result);
  }
  /**
   */
  @Test
  public void testReadToZeroInZeroOut() throws Exception {
    ByteBuffer[] ref = new ByteBuffer[0];
    BufferManager buffer = mock(BufferManager.class);
    when(buffer.forwardFromReadBuffer(any())).thenReturn(0);
    BufferManagerWrapper instance = new BufferManagerWrapper(buffer);
    SocketEndpoint.ResultType result = instance.readTo(ref);
    assertEquals(SocketEndpoint.ResultType.ZERO, result);
  }
  /**
   */
  @Test
  public void testReadToBytesInZeroOut() throws Exception {
    ByteBuffer[] ref = new ByteBuffer[] {ByteBuffer.allocate(32)};
    BufferManager buffer = mock(BufferManager.class);
    when(buffer.recvToBuffer()).thenReturn(0);
    when(buffer.forwardFromReadBuffer(any())).thenReturn(0);
    BufferManagerWrapper instance = new BufferManagerWrapper(buffer);
    SocketEndpoint.ResultType result = instance.readTo(ref);
    assertEquals(SocketEndpoint.ResultType.ZERO, result);
  }

  /**
   */
  @Test
  public void testReadToBytesInBytesOut() throws Exception {
    ByteBuffer[] ref = new ByteBuffer[] {ByteBuffer.allocate(32)};
    BufferManager buffer = mock(BufferManager.class);
    when(buffer.recvToBuffer()).thenReturn(32);
    when(buffer.forwardFromReadBuffer(any())).thenAnswer(a->{
      ByteBuffer b = (ByteBuffer)a.getArgument(0);
      b.position(b.limit());
      return 32;
    });
    BufferManagerWrapper instance = new BufferManagerWrapper(buffer);
    SocketEndpoint.ResultType result = instance.readTo(ref);
    assertEquals(SocketEndpoint.ResultType.SUCCESS, result);
  }
}
