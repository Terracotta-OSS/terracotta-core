/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * TCListener implementation
 * 
 * @author teck
 */
final class TCListenerJDK14 extends AbstractTCListener {
  private final ServerSocketChannel         ssc;
  private final TCCommJDK14                 comm;
  private final TCConnectionEventListener   listener;
  private final AbstractTCConnectionManager parent;

  TCListenerJDK14(ServerSocketChannel ssc, ProtocolAdaptorFactory factory, TCCommJDK14 comm,
                  TCConnectionEventListener listener, AbstractTCConnectionManager parent) {
    super(ssc.socket(), factory);
    this.ssc = ssc;
    this.comm = comm;
    this.listener = listener;
    this.parent = parent;
  }

  protected void stopImpl(Runnable callback) {
    comm.stopListener(ssc, callback);
  }

  TCConnectionJDK14 createConnection(SocketChannel ch) {
    TCProtocolAdaptor adaptor = getProtocolAdaptorFactory().getInstance();
    TCConnectionJDK14 rv = new TCConnectionJDK14(listener, comm, adaptor, ch, parent);
    rv.finishConnect();
    parent.newConnection(rv);
    return rv;
  }
}