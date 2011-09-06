/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.EchoSink;
import com.tc.net.protocol.GenericNetworkMessageSink;
import com.tc.net.protocol.GenericProtocolAdaptor;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;

import java.io.IOException;

/**
 * A simple server instance that accepts GenericNetwork messages and delivers them to a sink
 * 
 * @author teck
 */
public class SimpleServer {
  private final GenericNetworkMessageSink sink;
  private final TCConnectionManager       connMgr;
  private final int                       port;
  private TCListener                      lsnr;

  public SimpleServer(GenericNetworkMessageSink sink) {
    this(sink, 0, 0);
  }

  public SimpleServer(GenericNetworkMessageSink sink, int port) {
    this(sink, port, 0);
  }

  public SimpleServer(GenericNetworkMessageSink sink, int port, int serverThreadCount) {
    this.sink = sink;
    this.port = port;
    this.connMgr = new TCConnectionManagerImpl("TestConnMgr", serverThreadCount,
                                                new HealthCheckerConfigImpl("DefaultConfigForActiveConnections"));
  }

  public TCConnectionManager getConnectionManager() {
    return this.connMgr;
  }

  public void start() throws IOException {
    TCSocketAddress addr = new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR, port);

    ProtocolAdaptorFactory factory = new ProtocolAdaptorFactory() {
      public TCProtocolAdaptor getInstance() {
        GenericProtocolAdaptor rv = new GenericProtocolAdaptor(sink);
        return rv;
      }
    };

    lsnr = connMgr.createListener(addr, factory, 4096, true);
  }

  public TCSocketAddress getServerAddr() {
    return lsnr.getBindSocketAddress();
  }

  public void stop() {
    if (lsnr != null) {
      lsnr.stop();
    }

    connMgr.shutdown();
  }

  private static void usage() {
    System.err.println("usage: SimpleServer <port> <verify>");
    System.exit(1);
  }

  public static void main(String args[]) throws Exception {
    if (args.length > 2) {
      usage();
    }

    int p = 0;
    boolean verify = false;

    if (args.length > 0) {
      p = Integer.parseInt(args[0]);
    }

    if (args.length > 1) {
      verify = Boolean.valueOf(args[1]).booleanValue();
    }

    SimpleServer server = new SimpleServer(new EchoSink(verify), p);
    server.start();
    System.out.println("Server started at: " + server.getServerAddr());

    Thread.sleep(Long.MAX_VALUE);
  }

}