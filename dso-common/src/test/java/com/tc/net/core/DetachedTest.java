package com.tc.net.core;

import com.tc.net.protocol.NullProtocolAdaptor;
import com.tc.test.TCTestCase;

import java.net.Socket;
import java.nio.channels.SocketChannel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DetachedTest extends TCTestCase {

  // see: DEV-7965
  public void testReadAndWriteFromClosedPipeSocket() throws Exception {
    // mock the socket and its channel
    SocketChannel socketChannel = mock(SocketChannel.class);
    Socket socket = mock(Socket.class);
    when(socketChannel.socket()).thenReturn(socket);

    // create the TCConnectionImpl
    TCConnectionManagerImpl connectionManager = new TCConnectionManagerImpl();
    NullProtocolAdaptor adaptor = new NullProtocolAdaptor();
    SocketParams socketParams = new SocketParams();
    CoreNIOServices coreNIOServices = new CoreNIOServices("mock core nio thread", null, socketParams);
    TCConnectionImpl tcConnection = new TCConnectionImpl(null, adaptor, socketChannel, connectionManager, coreNIOServices, socketParams, null);

    // detach the connection -> this creates the pipe socket, then close it
    Socket pipeSocket = tcConnection.detach();
    pipeSocket.close();

    // check that write returns fine
    assertEquals(0, tcConnection.doWrite());
    // check that read returns fine
    assertEquals(0, tcConnection.doRead());
  }

}
