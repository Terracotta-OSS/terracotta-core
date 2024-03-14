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

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReference;
import com.tc.util.Assert;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;

/**
 *
 */
public class TCSocketEndpointReaderTest {
  
  public TCSocketEndpointReaderTest() {
  }

  /**
   * Test of readFromSocket method, of class TCSocketEndpointReader.
   */
  @Test
  public void testReadFromSocket() throws Exception {
    SocketEndpoint endpoint = mock(SocketEndpoint.class);
    when(endpoint.readTo(any())).then((InvocationOnMock iom) -> {
      for (ByteBuffer b : (ByteBuffer[])iom.getArgument(0)) {
        b.position(b.limit());
      }
      return SocketEndpoint.ResultType.SUCCESS;
    });
    Consumer<TCByteBuffer> returns = mock(Consumer.class);
    Function<Integer, TCByteBuffer> allocator = TCByteBufferFactory::getInstance;
    try (TCSocketEndpointReader reader = new TCSocketEndpointReader(allocator, returns)) {
      try (TCReference ref = reader.readFromSocket(endpoint, 32)) {
        Assert.assertEquals(ref.available(), 32);
        verify(endpoint).readTo(any());
        try (TCReference ref2 = reader.readFromSocket(endpoint, 32)) {
          verify(returns, never()).accept(any());
        }
      }
    }
    verify(returns, times(2)).accept(any());
  }
  /**
   * Test of readFromSocket method, of class TCSocketEndpointReader.
   */
  @Test
  public void testBigRead() throws Exception {
    SocketEndpoint endpoint = mock(SocketEndpoint.class);
    when(endpoint.readTo(any())).then((InvocationOnMock iom) -> {
      for (ByteBuffer b : ((ByteBuffer[])iom.getArgument(0))) {
        b.position(b.limit());
      }
      return SocketEndpoint.ResultType.SUCCESS;
    });
    Consumer<TCByteBuffer> returns = mock(Consumer.class);
    Function<Integer, TCByteBuffer> allocator = s->TCByteBufferFactory.getInstance(TCByteBufferFactory.getFixedBufferSize());
    try (TCSocketEndpointReader reader = new TCSocketEndpointReader(allocator, returns)) {
      try (TCReference ref = reader.readFromSocket(endpoint, TCByteBufferFactory.getFixedBufferSize() * 2)) {
        Assert.assertEquals(ref.available(), TCByteBufferFactory.getFixedBufferSize() * 2);
        verify(endpoint).readTo(any());
        try (TCReference ref2 = reader.readFromSocket(endpoint, 32)) {
          verify(returns, never()).accept(any());
        }
      }
    }
    verify(returns, times(3)).accept(any());
  } 
  
  /**
   * Test of readFromSocket method, of class TCSocketEndpointReader.
   */
  @Test
  public void testOverflow() throws Exception {
    SocketEndpoint endpoint = mock(SocketEndpoint.class);
    when(endpoint.readTo(any())).then((InvocationOnMock iom) -> {
      ByteBuffer[] bb = ((ByteBuffer[])iom.getArgument(0));
      if (bb.length == 1) {
        return SocketEndpoint.ResultType.OVERFLOW;
      } else {
        for (ByteBuffer b : bb) {
          b.position(b.limit());
        }
        return SocketEndpoint.ResultType.SUCCESS;
      }

    });
    Consumer<TCByteBuffer> returns = mock(Consumer.class);
    Function<Integer, TCByteBuffer> allocator = s->TCByteBufferFactory.getInstance(TCByteBufferFactory.getFixedBufferSize());
    try (TCSocketEndpointReader reader = new TCSocketEndpointReader(allocator, returns)) {
      try (TCReference ref = reader.readFromSocket(endpoint, TCByteBufferFactory.getFixedBufferSize())) {
        Assert.assertEquals(TCByteBufferFactory.getFixedBufferSize(), ref.available());
        verify(endpoint, times(2)).readTo(any());
        try (TCReference ref2 = reader.readFromSocket(endpoint, 32)) {
          verify(returns, never()).accept(any());
        }
      }
    }
    verify(returns, times(3)).accept(any());
  }
}
