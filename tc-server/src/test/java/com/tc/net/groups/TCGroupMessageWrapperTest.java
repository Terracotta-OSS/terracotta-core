/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.groups;

import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.Enrollment;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ClearTextSocketEndpointFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageFactory;
import com.tc.net.protocol.tcm.TCMessageFactoryImpl;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.net.core.ProductID;
import com.tc.util.State;
import com.tc.util.UUID;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import com.tc.net.protocol.tcm.TCAction;

/*
 * This test really belongs in the TC Messaging module but it's dependencies
 * currently prevent that.  It needs some heavy refactoring.
 */
/*
 * Test case for TC-Group-Comm Tribes' GroupMessage sent via TCMessage
 */
public class TCGroupMessageWrapperTest extends TestCase {

  private final static String                     LOCALHOST      = "localhost";
  MessageMonitor                                  monitor        = new NullMessageMonitor();
  final TCMessageFactory                          msgFactory     = new TCMessageFactoryImpl(monitor);
  final TCMessageRouter                           msgRouter      = new TCMessageRouterImpl();
  private TCConnectionManager                     clientConns;
  private TCConnectionManager                     serverConns;  
  private CommunicationsManager                   clientComms;
  private CommunicationsManager                   serverComms;
  private ChannelManager                          channelManager;
  private final LinkedBlockingQueue<GroupMessage> queue          = new LinkedBlockingQueue<>(10);
  private static final long                       timeout        = 1000;
  private final TimeUnit                          unit           = TimeUnit.MILLISECONDS;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    clientConns = new TCConnectionManagerImpl("TestCommsMgr-Client", 0, new ClearTextSocketEndpointFactory());
    clientComms = new CommunicationsManagerImpl(monitor, new TCMessageRouterImpl(),
                                                new PlainNetworkStackHarnessFactory(), clientConns,
                                                new NullConnectionPolicy(), new DisabledHealthCheckerConfigImpl(),
                                                new TransportHandshakeErrorNullHandler(),  Collections.emptyMap(),
                                                Collections.emptyMap());
    serverConns = new TCConnectionManagerImpl("TestCommsMgr-Server", 0, new ClearTextSocketEndpointFactory());
    serverComms = new CommunicationsManagerImpl(monitor, new TCMessageRouterImpl(),
                                                new PlainNetworkStackHarnessFactory(), serverConns,
                                                new NullConnectionPolicy(), new DisabledHealthCheckerConfigImpl(),
                                                new TransportHandshakeErrorNullHandler(),  Collections.emptyMap(),
                                                Collections.emptyMap());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      clientComms.getConnectionManager().closeAllConnections();

      for (int i = 0; i < 30; i++) {
        if (channelManager.getChannels().length == 0) {
          break;
        }
        Thread.sleep(100);
      }

    } finally {
      try {
        clientComms.shutdown();
        clientConns.shutdown();
      } finally {
        serverComms.shutdown();
        serverConns.shutdown();
      }
    }
  }

  private NetworkListener initServer() throws Exception {
    serverComms.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    ((CommunicationsManagerImpl) serverComms).getMessageRouter().routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE,
                                                                                  new TCMessageSink() {
                                                                                    @Override
                                                                                    public void putMessage(TCAction message)
                                                                                        throws UnsupportedMessageTypeException {
                                                                                      try {
                                                                                        TCGroupMessageWrapper mesg = (TCGroupMessageWrapper) message;
                                                                                        mesg.hydrate();
                                                                                        queue.offer(mesg.getGroupMessage(),
                                                                                                    timeout, unit);
                                                                                      } catch (Exception e) {
                                                                                        throw new RuntimeException(e);
                                                                                      }
                                                                                    }
                                                                                  });
    NetworkListener lsnr = serverComms.createListener(new InetSocketAddress(TCSocketAddress.LOOPBACK_IP, 0), (c)->true,
                                                      new DefaultConnectionIdFactory(), (MessageTransport t)->true);

    lsnr.start(new HashSet<>());
    return (lsnr);
  }

  private ClientMessageChannel openChannel(NetworkListener lsnr) throws Exception {
    ClientMessageChannel channel;
    clientComms.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    channel = clientComms
        .createClientChannel(ProductID.SERVER,
                             3000);
    channel.open(InetSocketAddress.createUnresolved(LOCALHOST, lsnr.getBindPort()));

    assertTrue(channel.isConnected());

    assertEquals(1, channelManager.getChannels().length);
    return (channel);
  }

  private void verifyGroupMessage(GroupMessage src, GroupMessage dst) {
    System.out.println("XXX receivedMesg class:" + dst.getClass().getName());

    assertEquals(src.getClass().getName(), dst.getClass().getName());
    assertEquals(src.getType(), dst.getType());
    assertEquals(src.getMessageID(), dst.getMessageID());
  }

  private GroupMessage sendGroupMessage(AbstractGroupMessage sendMesg) throws Exception {
    NetworkListener lsnr = initServer();
    channelManager = lsnr.getChannelManager();
    assertEquals(0, channelManager.getChannels().length);

    ClientMessageChannel channel = openChannel(lsnr);

    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) channel.createMessage(TCMessageType.GROUP_WRAPPER_MESSAGE);
    wrapper.setGroupMessage(sendMesg);
    wrapper.send();
    GroupMessage receivedMesg = queue.poll(timeout, unit);
    assertNotNull(receivedMesg);
    verifyGroupMessage(sendMesg, receivedMesg);
    return (receivedMesg);
  }

  public void testClusterStateMessage() throws Exception {
    AbstractGroupMessage sendMesg = new ClusterStateMessage(ClusterStateMessage.OPERATION_SUCCESS, new MessageID(1000));
    sendGroupMessage(sendMesg);
  }

  public void testGroupZapNodeMessage() throws Exception {
    long weights[] = new long[] { 1, 23, 44, 78 };
    AbstractGroupMessage sendMesg = new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST,
                                                    L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "Zapping node", weights);
    sendGroupMessage(sendMesg);
  }

  public void testL2StateMessage() throws Exception {
    long weights[] = new long[] { 1, 23, 44, 78 };
    Enrollment enroll = new Enrollment(makeNodeID("test"), true, weights);
    AbstractGroupMessage sendMesg = new L2StateMessage(L2StateMessage.START_ELECTION, enroll, new State("dummy"));
    sendGroupMessage(sendMesg);
  }

  private ServerID makeNodeID(String name) {
    return (new ServerID(name, UUID.getUUID().toString().getBytes()));
  }

}
