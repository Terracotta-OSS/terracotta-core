package com.tctest.management;

import com.tc.exception.TCRuntimeException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnknownNameException;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.object.session.NullSessionManager;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by lorban on 29/04/14.
 */
public class ListRemoteServices implements TCMessageSink {

  private final int         port;
  private final BlockingQueue<TCMessage> queue = new LinkedBlockingQueue<TCMessage>();

  ListRemoteServices(int port) {
    this.port = port;
  }

  public void setupAndBusyLoop() throws Exception {
    TCMessageRouter messageRouter = new TCMessageRouterImpl();
    CommunicationsManager comms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(),
        messageRouter, new PlainNetworkStackHarnessFactory(),
        new NullConnectionPolicy(),
        new DisabledHealthCheckerConfigImpl(),
        Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    comms.addClassMapping(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, ListRegisteredServicesMessage.class);
    comms.addClassMapping(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE, ListRegisteredServicesResponseMessage.class);

    ClientMessageChannel channel = null;
    try {
      channel = comms.createClientChannel(new NullSessionManager(), 0, "127.0.0.1", this.port, 3000,
          new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo("127.0.0.1", this.port) })
      );

      channel.open();
      messageRouter.routeMessageType(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, this);
      messageRouter.routeMessageType(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE, this);
      messageRouter.routeMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, this);
      messageRouter.routeMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE, this);

      System.out.println("L1 ready");

      while (true) {
        ListRegisteredServicesMessage message = (ListRegisteredServicesMessage)getMessage(5000);
        ListRegisteredServicesResponseMessage responseMessage = (ListRegisteredServicesResponseMessage)message.getChannel().createMessage(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE);
        //responseMessage.setRemoteCallDescriptor(new RemoteCallDescriptor(message.getChannel().getLocalNodeID(), serviceID, "myMethod", new String[] { "java.lang.String", "int" }));
        responseMessage.send();
      }
    } catch (TCRuntimeException tcre) {
      // if interrupted, it was done to shut down
      if (!(tcre.getCause() instanceof InterruptedException)) {
        tcre.printStackTrace();
      }
    } finally {
      if (channel != null) {
        messageRouter.unrouteMessageType(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE);
        messageRouter.unrouteMessageType(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE);
        messageRouter.unrouteMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE);
        messageRouter.unrouteMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE);
      }
      comms.shutdown();
    }
  }

  private TCMessage getMessage(long timeout) {
    try {
      return queue.poll(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public static void main(String[] args) throws Throwable {
    final Server server = new Server();
    final Thread l1Thread = Thread.currentThread();

    Thread l2Thread = new Thread() {
      @Override
      public void run() {
        try {
          server.awaitL1();
          server.listRemoteServices();

          l1Thread.interrupt();

          server.shutdown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    l2Thread.setDaemon(true);
    l2Thread.start();

    ListRemoteServices client = new ListRemoteServices(server.getPort());
    client.setupAndBusyLoop();
  }

  @Override
  public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
    try {
      message.hydrate();
      queue.put(message);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    } catch (UnknownNameException e) {
      throw new TCRuntimeException(e);
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
  }

  private static class Server implements TCMessageSink {

    private final CommunicationsManager comms;
    private final NetworkListener listener;
    private final Semaphore putMsgSemaphore = new Semaphore(0, true);

    Server() throws Exception {
      TCMessageRouter messageRouter = new TCMessageRouterImpl();
      comms = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), messageRouter,
          new PlainNetworkStackHarnessFactory(), new NullConnectionPolicy(),
          new DisabledHealthCheckerConfigImpl(), Collections.EMPTY_MAP,
          Collections.EMPTY_MAP);
      comms.addClassMapping(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, ListRegisteredServicesMessage.class);
      comms.addClassMapping(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE, ListRegisteredServicesResponseMessage.class);
      comms.addClassMapping(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, InvokeRegisteredServiceMessage.class);
      comms.addClassMapping(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE, InvokeRegisteredServiceResponseMessage.class);

      messageRouter.routeMessageType(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE, this);
      messageRouter.routeMessageType(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE, this);
      messageRouter.routeMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_MESSAGE, this);
      messageRouter.routeMessageType(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE, this);

      listener = comms.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
          new DefaultConnectionIdFactory());
      listener.start(Collections.EMPTY_SET);

      System.out.println("Server listening on " + listener.getBindPort());
    }

    public void listRemoteServices() throws InterruptedException {
      //System.out.println("list remote services");
      MessageChannelInternal[] channels = listener.getChannelManager().getChannels();
      for (MessageChannelInternal channel : channels) {
        //System.out.println("server sending message to " + channel.getChannelID());
        TCMessage message = channel.createMessage(TCMessageType.LIST_REGISTERED_SERVICES_MESSAGE);
        message.send();
      }

      // wait for the response
      putMsgSemaphore.acquire();
    }

    public void awaitL1() throws InterruptedException {
      MessageChannelInternal[] channels = listener.getChannelManager().getChannels();
      while (channels.length == 0) {
        //System.out.println("no channels yet");
        Thread.sleep(100);
        channels = listener.getChannelManager().getChannels();
      }
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
        ListRegisteredServicesResponseMessage listRegisteredServicesResponseMessage = (ListRegisteredServicesResponseMessage) message;
        message.hydrate();

/*
        NodeID l1Node = listRegisteredServicesResponseMessage.getRemoteCallDescriptors().getL1Node();
        String methodName = listRegisteredServicesResponseMessage.getRemoteCallDescriptors().getMethodName();
        String argTypeNames = Arrays.toString(listRegisteredServicesResponseMessage.getRemoteCallDescriptors().getArgTypeNames());

        System.out.println("server recv from " + l1Node + " method " + methodName + "(" + argTypeNames + ")");
*/
      } catch (Exception e) {
        e.printStackTrace();
      }
      putMsgSemaphore.release();
    }

  }

}

