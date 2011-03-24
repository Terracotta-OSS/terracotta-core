/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ClusterStateMessage;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.ObjectListSyncMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessage;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.ObjectSyncResetMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.l2.state.Enrollment;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageFactory;
import com.tc.net.protocol.tcm.TCMessageFactoryImpl;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.DisabledHealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorNullHandler;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.session.NullSessionManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.ObjectIDSet;
import com.tc.util.UUID;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

/*
 * Test case for TC-Group-Comm Tribes' GroupMessage sent via TCMessage
 */
public class TCGroupMessageWrapperTest extends TestCase {

  private final static String                     LOCALHOST      = "localhost";
  MessageMonitor                                  monitor        = new NullMessageMonitor();
  final NullSessionManager                        sessionManager = new NullSessionManager();
  final TCMessageFactory                          msgFactory     = new TCMessageFactoryImpl(sessionManager, monitor);
  final TCMessageRouter                           msgRouter      = new TCMessageRouterImpl();
  private CommunicationsManager                   clientComms;
  private CommunicationsManager                   serverComms;
  private ChannelManager                          channelManager;
  private final LinkedBlockingQueue<GroupMessage> queue          = new LinkedBlockingQueue(10);
  private static final long                       timeout        = 1000;
  private final TimeUnit                          unit           = TimeUnit.MILLISECONDS;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client", monitor, new TCMessageRouterImpl(),
                                                new PlainNetworkStackHarnessFactory(), null,
                                                new NullConnectionPolicy(), 0, new DisabledHealthCheckerConfigImpl(),
                                                new TransportHandshakeErrorNullHandler(), Collections.EMPTY_MAP,
                                                Collections.EMPTY_MAP);
    serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server", monitor, new TCMessageRouterImpl(),
                                                new PlainNetworkStackHarnessFactory(), null,
                                                new NullConnectionPolicy(), 0, new DisabledHealthCheckerConfigImpl(),
                                                new TransportHandshakeErrorNullHandler(), Collections.EMPTY_MAP,
                                                Collections.EMPTY_MAP);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      clientComms.getConnectionManager().closeAllConnections(5000);

      for (int i = 0; i < 30; i++) {
        if (channelManager.getChannels().length == 0) {
          break;
        }
        Thread.sleep(100);
      }

    } finally {
      try {
        clientComms.shutdown();
      } finally {
        serverComms.shutdown();
      }
    }
  }

  private NetworkListener initServer() throws Exception {
    serverComms.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    ((CommunicationsManagerImpl) serverComms).getMessageRouter().routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE,
                                                                                  new TCMessageSink() {
                                                                                    public void putMessage(TCMessage message)
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
    NetworkListener lsnr = serverComms.createListener(sessionManager,
                                                      new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0), true,
                                                      new DefaultConnectionIdFactory());

    lsnr.start(new HashSet());
    return (lsnr);
  }

  private ClientMessageChannel openChannel(NetworkListener lsnr) throws Exception {
    ClientMessageChannel channel;
    clientComms.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    channel = clientComms
        .createClientChannel(sessionManager,
                             0,
                             TCSocketAddress.LOOPBACK_IP,
                             lsnr.getBindPort(),
                             3000,
                             new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(LOCALHOST, lsnr
                                 .getBindPort()) }));
    channel.open();

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

  private GroupMessage sendGroupMessage(GroupMessage sendMesg) throws Exception {
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
    GroupMessage sendMesg = new ClusterStateMessage(ClusterStateMessage.OPERATION_SUCCESS, new MessageID(1000));
    sendGroupMessage(sendMesg);
  }

  public void testGCResultMessage() throws Exception {
    ObjectIDSet oidSet = new ObjectIDSet();
    for (long i = 1; i <= 100; ++i) {
      oidSet.add(new ObjectID(i));
    }
    GroupMessage sendMesg = new GCResultMessage(GCResultMessage.GC_RESULT, new GarbageCollectionInfo(), oidSet);
    sendGroupMessage(sendMesg);
  }

  public void testGroupZapNodeMessage() throws Exception {
    long weights[] = new long[] { 1, 23, 44, 78 };
    GroupMessage sendMesg = new GroupZapNodeMessage(GroupZapNodeMessage.ZAP_NODE_REQUEST,
                                                    L2HAZapNodeRequestProcessor.SPLIT_BRAIN, "Zapping node", weights);
    sendGroupMessage(sendMesg);
  }

  public void testL2StateMessage() throws Exception {
    long weights[] = new long[] { 1, 23, 44, 78 };
    Enrollment enroll = new Enrollment(makeNodeID("test"), true, weights);
    GroupMessage sendMesg = new L2StateMessage(L2StateMessage.START_ELECTION, enroll);
    sendGroupMessage(sendMesg);
  }

  public void testObjectListSyncMessage() throws Exception {
    GroupMessage sendMesg = new ObjectListSyncMessage(new MessageID(10), ObjectListSyncMessage.REQUEST);
    sendGroupMessage(sendMesg);
  }

  public void testObjectSyncCompleteMessage() throws Exception {
    GroupMessage sendMesg = new ObjectSyncCompleteMessage(ObjectSyncCompleteMessage.OBJECT_SYNC_COMPLETE, 100);
    sendGroupMessage(sendMesg);
  }

  public void testObjectSyncMessage() throws Exception {
    ObjectIDSet dnaOids = new ObjectIDSet();
    for (long i = 1; i <= 100; ++i) {
      dnaOids.add(new ObjectID(i));
    }
    int count = 10;
    TCByteBuffer[] serializedDNAs = new TCByteBuffer[] {};
    ObjectStringSerializer objectSerializer = new ObjectStringSerializerImpl();
    Map roots = new HashMap();
    long sID = 10;
    ObjectSyncMessage message = new ObjectSyncMessage(ObjectSyncMessage.MANAGED_OBJECT_SYNC_TYPE);
    message.initialize(new ServerTransactionID(new ServerID("hello", new byte[] { 34, 33, (byte) 234 }),
                                               new TransactionID(342)), dnaOids, count, serializedDNAs,
                       objectSerializer, roots, sID);
    sendGroupMessage(message);
  }

  public void testObjectSyncResetMessage() throws Exception {
    GroupMessage sendMesg = new ObjectSyncResetMessage(ObjectSyncResetMessage.REQUEST_RESET, new MessageID(10));
    sendGroupMessage(sendMesg);
  }

  public void testRelayedCommitTransactionMessage() throws Exception {
    NodeID nodeID = makeNodeID("test");
    TCByteBuffer[] buffer = new TCByteBuffer[] {};
    ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    Map sid2gid = new HashMap();
    long seqID = 100;
    GlobalTransactionID lowWaterMark = new GlobalTransactionID(200);

    GroupMessage sendMesg = new RelayedCommitTransactionMessage(nodeID, buffer, serializer, sid2gid, seqID,
                                                                lowWaterMark);
    sendGroupMessage(sendMesg);
  }

  public void testServerTxnAckMessage() throws Exception {
    NodeID nodeID = makeNodeID("test");
    MessageID messageID = new MessageID(100);
    Set serverTxnIDs = new HashSet(10);
    GroupMessage sendMesg = new ServerTxnAckMessage(nodeID, messageID, serverTxnIDs);
    sendGroupMessage(sendMesg);
  }

  private ServerID makeNodeID(String name) {
    return (new ServerID(name, UUID.getUUID().toString().getBytes()));
  }

}
