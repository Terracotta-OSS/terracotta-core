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
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.PortChooser;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static junit.framework.TestCase.fail;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TCConnectionImplTest {

  @Test
  public void testFinishConnection() throws Exception {
    int port = new PortChooser().chooseRandomPort();
    ServerSocket socket = new ServerSocket(port);
    final AtomicBoolean createManager = new AtomicBoolean(true);
    TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
    TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
    TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
    final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
    SocketParams socketParams = new SocketParams();
    BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);
    when(bufferManagerFactory.createBufferManager(any(SocketChannel.class), anyBoolean())).thenAnswer(invocationOnMock -> {
      verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class), any(ScatteringByteChannel.class));
      verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class), any(ScatteringByteChannel.class));
      return new ClearTextBufferManager((SocketChannel) invocationOnMock.getArguments()[0]);
    });
    TCConnection conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, bufferManagerFactory);
    TCSocketAddress addr = new TCSocketAddress("localhost", port);
    conn.connect(addr, 0);
    verify(listener).connectEvent(any(TCConnectionEvent.class));
    
    verify(nioServiceThread).requestReadInterest(any(TCChannelReader.class), any(ScatteringByteChannel.class));

    conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, mock(BufferManagerFactory.class));
    createManager.set(false);
    
    try {
      conn.connect(addr, 0);
      fail();
    } catch (IOException ioe) {
  // expected;
    }
    
    socket.close();
  }
  
  @Test
  public void testWriteEndsWhenClose() throws Exception {
    int port = new PortChooser().chooseRandomPort();
    ServerSocket socket = new ServerSocket(port);
    final AtomicBoolean createManager = new AtomicBoolean(true);
    TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
    TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
    TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
    final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
    SocketParams socketParams = new SocketParams();
    BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);
    
    BufferManager bufferManager = mock(BufferManager.class);
    when(bufferManager.sendFromBuffer()).thenReturn(0);
    when(bufferManager.forwardToWriteBuffer(any(ByteBuffer.class))).thenAnswer((i)->{
      return ((ByteBuffer)i.getArguments()[0]).remaining();
    });
   
    when(bufferManagerFactory.createBufferManager(any(SocketChannel.class), anyBoolean())).thenAnswer(invocationOnMock -> {
      verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class), any(ScatteringByteChannel.class));
      return bufferManager;
    });
    TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, bufferManagerFactory);
    TCSocketAddress addr = new TCSocketAddress("localhost", port);
    conn.connect(addr, 0);
        
    TCNetworkMessage msg = mock(TCNetworkMessage.class);
    when(msg.getEntireMessageData()).thenReturn(new TCByteBuffer[] {TCByteBufferFactory.wrap(new byte[512])});
    when(msg.getDataLength()).thenReturn(512);
    
    conn.putMessage(msg);
    
    new Thread(()->sleepThenClose(conn)).start();
    int w = conn.doWrite();
    
    Assert.assertEquals(0, w);
  }
  
  @Test
  public void testCloseBufferManager() throws Exception {
    int port = new PortChooser().chooseRandomPort();
    ServerSocket socket = new ServerSocket(port);
    final AtomicBoolean createManager = new AtomicBoolean(true);
    TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
    TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
    TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
    final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
    SocketParams socketParams = new SocketParams();
    BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);
    
    BufferManager bufferManager = mock(BufferManager.class);
    when(bufferManager.sendFromBuffer()).thenReturn(0);
    when(bufferManager.forwardToWriteBuffer(any(ByteBuffer.class))).thenAnswer((i)->{
      return ((ByteBuffer)i.getArguments()[0]).remaining();
    });
   
    when(bufferManagerFactory.createBufferManager(any(SocketChannel.class), anyBoolean())).thenAnswer(invocationOnMock -> {
      verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class), any(ScatteringByteChannel.class));
      return bufferManager;
    });
    TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, bufferManagerFactory);
    TCSocketAddress addr = new TCSocketAddress("localhost", port);
    conn.connect(addr, 0);
        
    conn.close(100);
    
    verify(bufferManager).close();
  }
  
  private void sleepThenClose(TCConnectionImpl conn) {
    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    }
    conn.close(100);
  }
}
