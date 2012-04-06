/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;
import com.tc.util.SequenceGenerator;

import java.util.Collections;

public class Ping implements TCMessageSink {

  private final int         port;
  private final LinkedQueue queue = new LinkedQueue();

  Ping(int port) {
    this.port = port;
  }

  public void ping() throws Exception {
    TCMessageRouter messageRouter = new TCMessageRouterImpl();
    CommunicationsManager comms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(),
                                                                messageRouter, new PlainNetworkStackHarnessFactory(),
                                                                new NullConnectionPolicy(),
                                                                new DisabledHealthCheckerConfigImpl(),
                                                                Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    comms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);

    ClientMessageChannel channel = null;
    try {
      channel = comms
          .createClientChannel(new NullSessionManager(), 0, "127.0.0.1", this.port, 3000,
                               new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo("127.0.0.1",
                                                                                                       this.port) }));

      SequenceGenerator sg = new SequenceGenerator();

      channel.open();
      messageRouter.routeMessageType(TCMessageType.PING_MESSAGE, this);
      for (int i = 0; i < 400; i++) {
        PingMessage pingMsg = (PingMessage) channel.createMessage(TCMessageType.PING_MESSAGE);
        pingMsg.initialize(sg);

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
    Server server = new Server();

    Ping ping = new Ping(server.getPort());
    ping.ping();

    server.shutdown();
  }

  public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
    try {
      queue.put(message);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  private static class Server implements TCMessageSink {

    private final CommunicationsManager comms;
    private final NetworkListener       listener;

    Server() throws Exception {
      TCMessageRouter messageRouter = new TCMessageRouterImpl();
      comms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), messageRouter,
                                            new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy(),
                                            new DisabledHealthCheckerConfigImpl(), Collections.EMPTY_MAP,
                                            Collections.EMPTY_MAP);
      comms.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);

      messageRouter.routeMessageType(TCMessageType.PING_MESSAGE, this);

      listener = comms.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                      new DefaultConnectionIdFactory());
      listener.start(Collections.EMPTY_SET);

      System.out.println("Server listening on " + listener.getBindPort());
    }

    public void shutdown() {
      comms.shutdown();
    }

    public int getPort() {
      return listener.getBindPort();
    }

    @Override
    public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
      try {
        PingMessage ping = (PingMessage) message;
        message.hydrate();
        System.out.println("server recv: " + ping.getSequence());
        message.getChannel().createMessage(TCMessageType.PING_MESSAGE).send();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
