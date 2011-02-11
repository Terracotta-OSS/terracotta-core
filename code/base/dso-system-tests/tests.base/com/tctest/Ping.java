/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;

import java.util.Collections;

/**
 * TODO: document me!
 * 
 * @author teck
 */
public class Ping implements TCMessageSink {

  private String            hostname;
  private int               port;
  private final LinkedQueue queue = new LinkedQueue();

  Ping(String args[]) {
    switch (args.length) {
      case 0: {
        usage();
        break;
      }
      case 1: {
        hostname = args[0];
        break;
      }
      case 2: {
        hostname = args[0];
        port = Integer.parseInt(args[1]);

        if ((port <= 0) || (port > 0xFFFF)) {
          usage();
        }
        break;
      }
      default: {
        usage();
      }
    }
  }

  private void usage() {
    System.err.println("usage: ping <hostname> [port]");
    System.exit(1);
  }

  public void ping() throws Exception {
    TCMessageRouter messageRouter = new TCMessageRouterImpl();
    CommunicationsManager comms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(),
                                                                messageRouter, new PlainNetworkStackHarnessFactory(),
                                                                new NullConnectionPolicy(),
                                                                new DisabledHealthCheckerConfigImpl(),
                                                                Collections.EMPTY_MAP, Collections.EMPTY_MAP);

    ClientMessageChannel channel = null;
    try {
      channel = comms.createClientChannel(new NullSessionManager(), 0, this.hostname, this.port, 3000,
                                          new ConnectionAddressProvider(new ConnectionInfo[0]));
      channel.open();
      messageRouter.routeMessageType(TCMessageType.PING_MESSAGE, this);
      for (int i = 0; i < 400; i++) {
        PingMessage pingMsg = (PingMessage) channel.createMessage(TCMessageType.PING_MESSAGE);

        long start = System.currentTimeMillis();
        pingMsg.send();
        TCMessage pong = getMessage(5000);
        long end = System.currentTimeMillis();

        if (pong != null) {
          System.out.println("RTT millis: " + (end - start));
        } else {
          System.err.println("Ping Request Timeout : " + (end - start) + " ms");
        }
      }
    } finally {
      if (channel != null) {
        messageRouter.unrouteMessageType(TCMessageType.PING_MESSAGE);
      }
      comms.shutdown();
    }
  }

  private TCMessage getMessage(long timeout) {
    try {
      return (TCMessage) queue.poll(timeout);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public static void main(String[] args) throws Throwable {
    Ping ping = new Ping(args);
    ping.ping();
  }

  public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
    try {
      queue.put(message);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

}
