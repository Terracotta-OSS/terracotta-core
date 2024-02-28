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

import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReferenceSupport;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static junit.framework.TestCase.fail;
import org.junit.Assert;
import org.junit.Test;
import org.terracotta.utilities.test.net.PortManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import com.tc.net.protocol.transport.WireProtocolHeader;
import com.tc.net.protocol.transport.WireProtocolMessage;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectableChannel;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.doAnswer;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 */
public class TCConnectionImplTest {

  @Test
  public void testFinishConnection() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        final AtomicBoolean createManager = new AtomicBoolean(true);
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);
        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenAnswer(invocationOnMock -> {
          verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class), any(ScatteringByteChannel.class));
          verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class), any(ScatteringByteChannel.class));
          return new ClearTextSocketEndpoint((SocketChannel) invocationOnMock.getArguments()[0]);
        });
        TCConnection conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
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
      }
    }
  }

  @Test
  public void testWriteEndsWhenClose() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        final AtomicBoolean createManager = new AtomicBoolean(true);
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).then(new Answer() {
          @Override
          public Object answer(InvocationOnMock iom) throws Throwable {
            ByteBuffer[] bytes = (ByteBuffer[])iom.getArgument(0);
            for (ByteBuffer b : bytes) {
              b.position(b.limit());
            }
            return SocketEndpoint.ResultType.SUCCESS;
          }
        });

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenAnswer(
            invocationOnMock -> {
              verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class),
                  any(ScatteringByteChannel.class));
              return bufferManager;
            });
        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);

        WireProtocolMessage msg = mock(WireProtocolMessage.class);
        when(msg.prepareToSend()).thenReturn(Boolean.TRUE);
        when(msg.getHeader()).thenReturn(mock(WireProtocolHeader.class));
        when(msg.getWireProtocolHeader()).thenReturn(mock(WireProtocolHeader.class));
        when(msg.getEntireMessageData()).thenReturn(TCReferenceSupport.createGCReference(TCByteBufferFactory.wrap(new byte[512])));
        when(msg.getDataLength()).thenReturn(512);

        conn.putMessage(msg);

        new Thread(() -> sleepThenClose(conn)).start();
        Thread.sleep(4000);
        long w = conn.doWrite();

        Assert.assertEquals(0, w);
      }
    }
  }

  @Test
  public void testCloseBufferManager() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        final AtomicBoolean createManager = new AtomicBoolean(true);
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        doAnswer(a->{
          ((Runnable)a.getArgument(1)).run();
          return null;
        }).when(nioServiceThread).cleanupChannel(any(SocketChannel.class), any(Runnable.class));
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).thenReturn(SocketEndpoint.ResultType.SUCCESS);

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenAnswer(
            invocationOnMock -> {
              verify(nioServiceThread, never()).requestReadInterest(any(TCChannelReader.class),
                  any(ScatteringByteChannel.class));
              return bufferManager;
            });
        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);

        conn.close();

        verify(bufferManager).close();
      }
    }
  }

  @Test
  public void testCancelledMessages() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).thenReturn(SocketEndpoint.ResultType.SUCCESS);

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenReturn(bufferManager);

        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);
        
        TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[10];
        
        for (int x=0;x<msgs.length;x++) {
          TCActionNetworkMessage action = mock(TCActionNetworkMessage.class);
          when(action.commit()).thenReturn(Boolean.FALSE);
          when(action.load()).thenReturn(Boolean.TRUE);
          conn.putMessage(action);
          msgs[x] = action;
        }
        
        verify(nioServiceThread).requestWriteInterest(any(TCConnectionImpl.class), any(GatheringByteChannel.class));
        assertTrue(conn.doWrite() == 0);
        for (TCActionNetworkMessage msg : msgs) {
          verify(msg).complete();
        }
        verify(nioServiceThread).removeWriteInterest(any(TCConnectionImpl.class), any(SelectableChannel.class));
      }
    }
  }


  @Test
  public void testCancelledMessagesBeforeLoad() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).thenReturn(SocketEndpoint.ResultType.SUCCESS);

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenReturn(bufferManager);

        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);
        
        TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[10];
        
        for (int x=0;x<msgs.length;x++) {
          TCActionNetworkMessage action = mock(TCActionNetworkMessage.class);
          when(action.commit()).thenReturn(Boolean.FALSE);
          when(action.load()).thenReturn(Boolean.FALSE);
          conn.putMessage(action);
          msgs[x] = action;
        }
        
        verify(nioServiceThread).requestWriteInterest(any(TCConnectionImpl.class), any(GatheringByteChannel.class));
        assertTrue(conn.doWrite() == 0);
        for (TCActionNetworkMessage msg : msgs) {
          verify(msg).complete();
          verify(msg, never()).commit();
        }
        verify(nioServiceThread).removeWriteInterest(any(TCConnectionImpl.class), any(SelectableChannel.class));
      }
    }
  }


  @Test
  public void testBatchWhenFull() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).thenReturn(SocketEndpoint.ResultType.SUCCESS);

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenReturn(bufferManager);

        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);
        
        TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[1024];
        
        for (int x=0;x<msgs.length;x++) {
          TCActionNetworkMessage action = mock(TCActionNetworkMessage.class);
          when(action.commit()).thenReturn(Boolean.FALSE);
          when(action.load()).thenReturn(Boolean.FALSE);
          conn.putMessage(action);
          msgs[x] = action;
        }
        for (TCActionNetworkMessage msg : msgs) {
          verify(msg, never()).complete();
        }        
        
        TCActionNetworkMessage action = mock(TCActionNetworkMessage.class);
        when(action.commit()).thenReturn(Boolean.FALSE);
        when(action.load()).thenReturn(Boolean.FALSE);
        conn.putMessage(action);

        for (TCActionNetworkMessage msg : msgs) {
          verify(msg).complete(); // batching called which calls complete
        }       
        verify(action, never()).complete(); // this one is queued but not batched
      }
    }
  }

  @Test
  public void testClosedInFlightCompletes() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        doAnswer(a->{
          ((Runnable)a.getArgument(1)).run();
          return null;
        }).when(nioServiceThread).cleanupChannel(any(), any());
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).thenReturn(SocketEndpoint.ResultType.SUCCESS);

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenReturn(bufferManager);

        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);
        
        TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[512];
        
        conn.close();
        for (int x=0;x<msgs.length;x++) {
          TCActionNetworkMessage action = mock(TCActionNetworkMessage.class);
          when(action.commit()).thenReturn(Boolean.TRUE);
          when(action.load()).thenReturn(Boolean.TRUE);
          conn.putMessage(action);
          msgs[x] = action;
        }
        for (TCActionNetworkMessage msg : msgs) {
          verify(msg).complete();
        }
      }
    }
  }

  @Test
  public void testLoadOnFailCompletes() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        doAnswer(a->{
          ((Runnable)a.getArgument(1)).run();
          return null;
        }).when(nioServiceThread).cleanupChannel(any(), any());
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).thenReturn(SocketEndpoint.ResultType.SUCCESS);

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenReturn(bufferManager);

        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);
        
        TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[512];
        
        for (int x=0;x<msgs.length;x++) {
          TCActionNetworkMessage action = mock(TCActionNetworkMessage.class);
          when(action.commit()).thenReturn(Boolean.FALSE);
          when(action.load()).thenReturn(Boolean.FALSE);
          conn.putMessage(action);
          msgs[x] = action;
        }
        conn.doWrite();
        for (TCActionNetworkMessage msg : msgs) {
          verify(msg).complete();
        }
      }
    }
  }

  @Test
  public void testCommitFailedCompletes() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      int port = portRef.port();
      try (ServerSocket socket = new ServerSocket(port)) {
        TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
        TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
        TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
        final CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
        doAnswer(a->{
          ((Runnable)a.getArgument(1)).run();
          return null;
        }).when(nioServiceThread).cleanupChannel(any(), any());
        SocketParams socketParams = new SocketParams();
        BufferManagerFactory bufferManagerFactory = mock(BufferManagerFactory.class);

        SocketEndpoint bufferManager = mock(SocketEndpoint.class);
        when(bufferManager.writeFrom(any())).thenReturn(SocketEndpoint.ResultType.SUCCESS);

        when(bufferManagerFactory.createSocketEndpoint(any(SocketChannel.class), anyBoolean())).thenReturn(bufferManager);

        TCConnectionImpl conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams,
            bufferManagerFactory);
        InetSocketAddress addr = new InetSocketAddress("localhost", port);
        conn.connect(addr, 0);
        
        TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[512];
        
        for (int x=0;x<msgs.length;x++) {
          TCActionNetworkMessage action = mock(TCActionNetworkMessage.class);
          when(action.commit()).thenReturn(Boolean.FALSE);
          when(action.load()).thenReturn(Boolean.TRUE);
          conn.putMessage(action);
          msgs[x] = action;
        }
        conn.doWrite();
        for (TCActionNetworkMessage msg : msgs) {
          verify(msg).complete();
        }
      }
    }
  }
  
  private void sleepThenClose(TCConnectionImpl conn) {
    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    }
    conn.close();
  }
}
