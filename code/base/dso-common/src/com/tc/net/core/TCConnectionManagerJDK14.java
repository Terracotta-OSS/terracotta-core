/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

/**
 * JDK 1.4 implementation of TCConnectionManager interface
 * 
 * @author teck
 */
class TCConnectionManagerJDK14 extends AbstractTCConnectionManager {
  TCConnectionManagerJDK14(TCComm comm) {
    super(comm);

    if (comm != null) {
      if (!(comm instanceof TCCommJDK14)) { throw new IllegalArgumentException("Illegal TCComm instance type: "
                                                                               + comm.getClass().getName()); }
    }
  }

  protected TCConnection createConnectionImpl(TCProtocolAdaptor adaptor, TCConnectionEventListener listener) {
    return new TCConnectionJDK14(listener, (TCCommJDK14) comm, adaptor, this);
  }

  protected TCListener createListenerImpl(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog, boolean reuseAddr)
      throws IOException {
    ServerSocketChannel ssc = ServerSocketChannel.open();
    ssc.configureBlocking(false);
    ServerSocket serverSocket = ssc.socket();
    serverSocket.setReuseAddress(reuseAddr);
    serverSocket.setReceiveBufferSize(64 * 1024);

    try {
      serverSocket.bind(new InetSocketAddress(addr.getAddress(), addr.getPort()), backlog);
    } catch (IOException ioe) {
      logger.warn("Unable to bind socket on address " + addr.getAddress() + ", port " + addr.getPort() + ", " + ioe.getMessage());
      throw ioe;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Bind: " + serverSocket.getLocalSocketAddress());
    }

    TCListenerJDK14 rv = new TCListenerJDK14(ssc, factory, (TCCommJDK14) comm, getConnectionListener(), this);
    ((TCCommJDK14) comm).requestAcceptInterest(rv, ssc);

    return rv;
  }
}