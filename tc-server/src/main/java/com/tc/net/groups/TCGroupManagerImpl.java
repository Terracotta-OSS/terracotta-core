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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.config.ServerConfigurationManager;
import com.tc.config.GroupConfiguration;
import com.tc.l2.L2DebugLogging;
import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.SocketEndpointFactory;
import com.tc.net.core.ClearTextSocketEndpointFactory;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerImpl;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHydrateSink;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandlerForGroupComm;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handler.ReceiveGroupMessageHandler;
import com.tc.objectserver.handler.TCGroupHandshakeMessageHandler;
import com.tc.objectserver.handler.TCGroupMemberDiscoveryHandler;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.net.core.ProductID;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.utils.L2Utils;
import com.tc.spi.Guardian;
import com.tc.util.TCTimeoutException;
import com.tc.util.UUID;
import org.terracotta.configuration.ServerConfiguration;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toSet;
import org.terracotta.server.ServerEnv;
import com.tc.net.protocol.tcm.TCAction;
import com.tc.net.utils.ConnectionLogger;
import java.util.Iterator;
import java.util.Properties;
import java.util.function.Supplier;


public class TCGroupManagerImpl implements GroupManager<AbstractGroupMessage>, ChannelManagerEventListener {
  private static final Logger logger = LoggerFactory.getLogger(TCGroupManagerImpl.class);

  public static final String                                HANDSHAKE_STATE_MACHINE_TAG = "TcGroupCommHandshake";
  
  private volatile int                                      serverCount;
  private final Supplier<Set<Node>>                         configuredNodes;
  
  private final String                                      version;
  private final InetSocketAddress                           relayLocation;
  private final ServerID                                    thisNodeID;
  private final int                                         groupPort;
  private final ConnectionPolicy                            connectionPolicy;
  private final CopyOnWriteArrayList<GroupEventsListener>   groupListeners              = new CopyOnWriteArrayList<>();
  private final Map<String, GroupMessageListener<? extends GroupMessage>>           messageListeners            = new ConcurrentHashMap<>();
  private final Map<MessageID, GroupResponseImpl>               pendingRequests             = new ConcurrentHashMap<>();
  private final AtomicBoolean                               isStopped                   = new AtomicBoolean(false);
  private final ConcurrentHashMap<ServerID, TCGroupMember>  members                     = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<MessageChannel, ServerID>  memberReceiver                     = new ConcurrentHashMap<>();
  private final Timer                                       handshakeTimer              = new Timer(ServerEnv.getServer().getIdentifier() +
                                                                                                    " - TC Group Manager Handshake timer",
                                                                                                    true);
  private final Set<NodeID>                                 zappedSet                   = Collections
                                                                                            .synchronizedSet(new HashSet<NodeID>());
  private final StageManager                                stageManager;
  private final AtomicBoolean                               alreadyJoined               = new AtomicBoolean(false);
  private final WeightGeneratorFactory                      weightGeneratorFactory;
  private final SocketEndpointFactory                        bufferManagerFactory;

  private CommunicationsManager                             communicationsManager;
  private TCConnectionManager                               connectionManager;
  private NetworkListener                                   groupListener;
  private TCGroupMemberDiscovery                            discover;
  private ZapNodeRequestProcessor                           zapNodeRequestProcessor     = new DefaultZapNodeRequestProcessor(
                                                                                                                             logger);
  private Stage<TCGroupMessageWrapper> receiveGroupMessageStage;
  private Stage<TCGroupHandshakeMessage> handshakeMessageStage;
  private Stage<DiscoveryStateMachine> discoveryStage;

  /*
   * Setup a communication manager which can establish channel from either sides.
   */
  public TCGroupManagerImpl(ServerConfigurationManager configSetupManager, StageManager stageManager,
                            TCConnectionManager comms,
                            ServerID thisNodeID, Node thisNode,
                            WeightGeneratorFactory weightGenerator, SocketEndpointFactory bufferManagerFactory) {
    this(configSetupManager, new NullConnectionPolicy(), stageManager, comms, thisNodeID, thisNode, weightGenerator,
         bufferManagerFactory);
  }

  public TCGroupManagerImpl(ServerConfigurationManager configSetupManager, ConnectionPolicy connectionPolicy,
                            StageManager stageManager, 
                            TCConnectionManager comms,
                            ServerID thisNodeID, Node thisNode,
                            WeightGeneratorFactory weightGenerator, SocketEndpointFactory bufferManagerFactory) {
    this.connectionPolicy = connectionPolicy;
    this.stageManager = stageManager;
    this.connectionManager = comms;
    this.thisNodeID = thisNodeID;
    this.bufferManagerFactory = bufferManagerFactory;
    this.version = configSetupManager.getProductInfo().version();
    this.relayLocation = configSetupManager.getRelayPeer();
    this.configuredNodes = ()-> {
      if (configSetupManager.isRelayDestination()) {
        return configSetupManager.getGroupConfiguration().directConnect(relayLocation).getNodes();
      } else {
        return configSetupManager.getGroupConfiguration().getNodes();
      }
    };

    ServerConfiguration l2DSOConfig = configSetupManager.getServerConfiguration();
    serverCount = configSetupManager.allCurrentlyKnownServers().length;
    
    this.groupPort = l2DSOConfig.getGroupPort().getPort();
    this.weightGeneratorFactory = weightGenerator;
    
    InetSocketAddress socketAddress;
    // proxy group port. use a different group port from tc.properties (if exist) than the one on tc-config
    // currently used by L2Reconnect proxy test.
    int groupConnectPort = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT, groupPort);

    socketAddress = new InetSocketAddress(l2DSOConfig.getGroupPort().getHostString(), groupConnectPort);
    init(socketAddress);
    Assert.assertNotNull(thisNodeID);
    setDiscover(new TCGroupMemberDiscoveryStatic(this, thisNode));
  }

  protected final String getVersion() {
    return this.version;
  }

  @Override
  public boolean isNodeConnected(NodeID sid) {
    TCGroupMember m = members.get((ServerID)sid);
    return (m != null) && m.getChannel().isOpen();
  }

  /*
   * for testing purpose only. Tester needs to do setDiscover().
   */
  public TCGroupManagerImpl(ConnectionPolicy connectionPolicy, String hostname, int port, int groupPort,
                            StageManager stageManager, WeightGeneratorFactory weightGenerator, Node[] servers) {
    this.connectionPolicy = connectionPolicy;
    this.stageManager = stageManager;
    this.bufferManagerFactory = new ClearTextSocketEndpointFactory();
    this.configuredNodes = ()->new HashSet<>(Arrays.asList(servers));

    this.groupPort = groupPort;
    this.relayLocation = null;
    this.version = "UNKNOWN";
    this.weightGeneratorFactory = weightGenerator;
    this.serverCount = 0;
    thisNodeID = new ServerID(new Node(hostname, port).getServerNodeName(), UUID.getUUID().toString().getBytes());
    init(new InetSocketAddress(TCSocketAddress.WILDCARD_IP, groupPort));
  }

  private void init(InetSocketAddress socketAddress) {

    TCProperties tcProperties = TCPropertiesImpl.getProperties();

    createTCGroupManagerStages();
    final NetworkStackHarnessFactory networkStackHarnessFactory = getNetworkStackHarnessFactory();

    final TCMessageRouter messageRouter = new TCMessageRouterImpl();
    initMessageRouter(messageRouter);

    final Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping = new HashMap<>();
    initMessageTypeClassMapping(messageTypeClassMapping);
    HealthCheckerConfig hcconfig = new HealthCheckerConfigImpl(tcProperties
                                                              .getPropertiesFor(TCPropertiesConsts.L2_L2_HEALTH_CHECK_CATEGORY), ServerEnv.getServer().getIdentifier() + " - TCGroupManager");
    
    if (connectionManager == null) {
      connectionManager = new TCConnectionManagerImpl(ServerEnv.getServer().getIdentifier() + " - " + CommunicationsManager.COMMSMGR_GROUPS, new ConnectionLogger("server"), serverCount <= 1 ? 0 :
        serverCount, bufferManagerFactory);
    }
    communicationsManager = new CommunicationsManagerImpl(new NullMessageMonitor(), messageRouter,
                                                          networkStackHarnessFactory, 
                                                          this.connectionManager,
                                                          this.connectionPolicy,
                                                          hcconfig,
                                                          thisNodeID, new TransportHandshakeErrorHandlerForGroupComm(),
                                                          messageTypeClassMapping, Collections.emptyMap(), bufferManagerFactory
    );

    groupListener = communicationsManager.createListener(socketAddress, (c)->true, new DefaultConnectionIdFactory(), (MessageTransport t)->true);
    // Listen to channel creation/removal
    groupListener.getChannelManager().addEventListener(this);

    registerForMessages(GroupZapNodeMessage.class, new ZapNodeRequestRouter());
  }

  private NetworkStackHarnessFactory getNetworkStackHarnessFactory() {
    return new PlainNetworkStackHarnessFactory();
  }

  private void createTCGroupManagerStages() {
    receiveGroupMessageStage = stageManager.createStage(ServerConfigurationContext.RECEIVE_GROUP_MESSAGE_STAGE, TCGroupMessageWrapper.class, new ReceiveGroupMessageHandler(this), 1);
    handshakeMessageStage = stageManager.createStage(ServerConfigurationContext.GROUP_HANDSHAKE_MESSAGE_STAGE, TCGroupHandshakeMessage.class, new TCGroupHandshakeMessageHandler(this), 1);
    discoveryStage = stageManager.createStage(ServerConfigurationContext.GROUP_DISCOVERY_STAGE, DiscoveryStateMachine.class, new TCGroupMemberDiscoveryHandler(this), 1, stageManager.getDefaultStageMaximumCapacity(), false, false);
  }

  private Map<TCMessageType, Class<? extends TCAction>> initMessageTypeClassMapping(Map<TCMessageType, Class<? extends TCAction>> messageTypeClassMapping) {
    messageTypeClassMapping.put(TCMessageType.GROUP_HANDSHAKE_MESSAGE, TCGroupHandshakeMessage.class);
    messageTypeClassMapping.put(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    return messageTypeClassMapping;
  }

  private void initMessageRouter(TCMessageRouter messageRouter) {
    messageRouter.routeMessageType(TCMessageType.GROUP_WRAPPER_MESSAGE, new TCMessageHydrateSink<>(receiveGroupMessageStage.getSink()));
    messageRouter.routeMessageType(TCMessageType.GROUP_HANDSHAKE_MESSAGE, new TCMessageHydrateSink<>(handshakeMessageStage.getSink()));
  }

  /*
   * getDiscoveryHandlerSink -- sink for discovery to enqueue tasks for open channel.
   */
  protected Sink<DiscoveryStateMachine> getDiscoveryHandlerSink() {
    return discoveryStage.getSink();
  }
  
  Set<Node> getConfiguredServers() {
    return this.configuredNodes.get();
  }

  /*
   * Once connected, both send NodeID to each other.
   */
  private void handshake(MessageChannel channel) {
    getOrCreateHandshakeStateMachine(channel);
  }

  public void receivedHandshake(TCGroupHandshakeMessage msg) {
    if (isDebugLogging()) {
      debugInfo("Received group handshake message from " + msg.getChannel());
    }

    MessageChannel channel = msg.getChannel();
    Assert.assertNotNull(channel);
    TCGroupHandshakeStateMachine stateMachine = getOrCreateHandshakeStateMachine(channel);
    if (stateMachine.getCurrentState() == stateMachine.initialState()) {
      ServerID node = msg.getNodeID();
      channel.addListener((event) -> {
        switch(event.getType()) {
          case TRANSPORT_DISCONNECTED_EVENT:
          case CHANNEL_CLOSED_EVENT:
          case TRANSPORT_CLOSED_EVENT:
            memberReceiver.remove(channel, node);
            break;
          default:
            // ignore
        }
      });
      // only add the channel if it is still open
      memberReceiver.compute(channel, (target, rNode)->channel.isConnected() ? node : null);
    }
    stateMachine.execute(msg);
  }

  @Override
  public ServerID getLocalNodeID() {
    return getNodeID();
  }

  private ServerID getNodeID() {
    return thisNodeID;
  }

  private void membersClear() {
    members.clear();
  }

  private boolean membersAdd(TCGroupMember member) {
    ServerID nodeID = member.getPeerNodeID();
    TCGroupMember old = members.putIfAbsent(nodeID, member);
    return old == null;
  }

  private void membersRemove(TCGroupMember member) {
    ServerID nodeID = member.getPeerNodeID();
    members.remove(nodeID);
  }

  private void removeIfMemberReconnecting(ServerID newNodeID) {
    members.entrySet().stream().filter(e->e.getKey().getName().equals(newNodeID.getName())).findFirst().ifPresent(e->{
      TCGroupMember oldMember = e.getValue();
      if ((oldMember.getPeerNodeID() != newNodeID)) {
        MessageChannel channel = oldMember.getChannel();
        if (!channel.isConnected()) {
          closeMember(oldMember);
          logger.warn("Removed old member " + oldMember + " for " + newNodeID);
        }
      }
    });
  }

  @Override
  public void shutdown() {
    stop();
  }

  public void stop() {
    isStopped.set(true);
    discover.stop();
    for (ServerID sid : members.keySet()) {
      closeMember(sid);
    }
    groupListener.stop();
    communicationsManager.shutdown();
    connectionManager.shutdown();
    handshakeTimer.cancel();
    for (TCGroupMember m : members.values()) {
      notifyAnyPendingRequests(m);
    }
    membersClear();
  }
  
  public TCConnectionManager getConnectionManager() {
    return this.connectionManager;
  }

  public boolean isStopped() {
    return (isStopped.get());
  }

  @Override
  public void registerForGroupEvents(GroupEventsListener listener) {
    groupListeners.add(listener);
  }

  @Override
  public void unregisterForGroupEvents(GroupEventsListener listener) {
    groupListeners.remove(listener);
  }
  
  private void fireNodeEvent(TCGroupMember member, boolean joined) {
    ServerID newNode = member.getPeerNodeID();
    member.setReady(joined);
    if (isDebugLogging()) {
      debugInfo("fireNodeEvent: joined = " + joined + ", node = " + newNode + ", channel: " + member.getChannel());
    }
    for (GroupEventsListener listener : groupListeners) {
      if (joined) {
        listener.nodeJoined(newNode);
      } else {
        listener.nodeLeft(newNode);
      }
    }
  }

  private boolean tryAddMember(TCGroupMember member) {
    if (!GuardianContext.validate(Guardian.Op.CONNECT_SERVER, "add:" + member.getPeerNodeID(), member.getChannel())) {
      return false;
    }
    if (isStopped.get()) {
      return false;
    }

    boolean added = membersAdd(member);
    if (added) {
      Properties props = new Properties();
      props.setProperty("nodeID", member.getPeerNodeID().toString());
      GuardianContext.validate(Guardian.Op.SECURITY_OP, "server member added to the group", props, member.getChannel());
      member.setTCGroupManager(this);
      return true;
    } else {
      return false;
    }
  }

  public NodeID directedJoin(String target, ChannelEventListener listener) throws GroupException {
    String[] hostPort = target.split("[:]");
    
    try {
      openChannel(hostPort[0], Integer.parseInt(hostPort[1]), listener);
    } catch (CommStackMismatchException | IOException | MaxConnectionsExceededException | NumberFormatException | TCTimeoutException e) {
      throw new GroupException(e);
    }
    
    return getNodeID();
  }

  @Override
  public NodeID join(GroupConfiguration groupConfiguration) throws GroupException {
    if (!alreadyJoined.compareAndSet(false, true)) { throw new GroupException("Already Joined"); }

    // discover must be started before listener thread to avoid missing nodeJoined group events.
    if (isDebugLogging()) {
      debugInfo("Starting discover... thisNode: " + groupConfiguration.getCurrentNode() + ", otherNodes: " + groupConfiguration.getNodes());
    }
    discover.setupNodes(groupConfiguration.getCurrentNode(), groupConfiguration.getNodes());
    discover.start();
    try {
      groupListener.start(new HashSet<>());
    } catch (IOException e) {
      throw new GroupException(e);
    }
    return (getNodeID());
  }
  
  @Override
  public void disconnect() {
    Collection<ServerID> check = new ArrayList<>(members.keySet());
    check.stream().filter(n->!n.equals(thisNodeID)).forEach(this::closeMember);
  }

  /**
   * Force close down the problematic peer Server member. Just a wrapper over the closeMember(TCGroupMember).
   */
  @Override
  public void closeMember(ServerID serverID) {
    TCGroupMember member = getMember(serverID);
    if (member != null) {
      logger.info("Closing down member for " + serverID + " - " + member);
      closeMember(member);
    } else {
      logger.warn("Closing down member for " + serverID + " - member doesn't exist.");
    }
  }
  
  @Override
  public void closeMember(String nodeName) {
    Collection<ServerID> check = new ArrayList<>(members.keySet());
    check.stream().filter(id->id.getName().equals(nodeName)).forEach(this::closeMember);
  }
  
  private void closeMember(TCGroupMember member) {
    Assert.assertNotNull(member);
    if (isDebugLogging()) {
      debugInfo("Closing member: " + member);
    }
    if (isStopped.get()) {
      shutdownMember(member);
      return;
    }
    member.setTCGroupManager(null);
    TCGroupMember m = members.get(member.getPeerNodeID());
    if ((m != null) && (m.getChannel() == member.getChannel())) {
      membersRemove(member);
      if (member.isJoinedEventFired()) {
        fireNodeEvent(member, false);
      }
      zappedSet.remove(member.getPeerNodeID());
      member.setJoinedEventFired(false);
      notifyAnyPendingRequests(member);
    }
    shutdownMember(member);
    if (isDebugLogging()) {
      debugInfo(getNodeID() + " removed " + member);
    }
  }

  private void shutdownMember(TCGroupMember member) {
    member.setReady(false);
    member.close();
    Properties props = new Properties();
    props.setProperty("nodeID", member.getPeerNodeID().toString());
    GuardianContext.validate(Guardian.Op.SECURITY_OP, "server member removed from the group", props, member.getChannel());
  }

  private void notifyAnyPendingRequests(TCGroupMember member) {
    synchronized (pendingRequests) {
      for (GroupResponseImpl response : pendingRequests.values()) {
        response.notifyMemberDead(member);
      }
    }
  }

  @Override
  public void sendAll(AbstractGroupMessage msg) {
    sendAll(msg, members.keySet());
  }

  @Override
  public void sendAll(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) {
    final boolean debug = msg instanceof L2StateMessage;
    for (TCGroupMember m : members.values()) {
      if (!nodeIDs.contains(m.getPeerNodeID())) {
        if (debug) {
          if (isDebugLogging()) {
            debugInfo("Not sending msg to " + m.getPeerNodeID() + ", " + msg + ", channel: " + m.getChannel());
          }
        }
        continue;
      }
      if (m.isReady()) {
        if (debug) {
          if (isDebugLogging()) {
            debugInfo("Sending msg to " + m.getPeerNodeID() + ", " + msg + ", channel: " + m.getChannel());
          }
        }
        m.sendIgnoreNotReady(msg);
      } else {
        logger.warn("Ignored sending msg to a not ready member=" + m + ", msg=" + msg);
      }
    }
  }

  @Override
  public void sendTo(NodeID node, AbstractGroupMessage msg) throws GroupException {
    // No callback in this case.
    Runnable sentCallback = null;
    internalSendTo(node, msg, sentCallback);
  }

  @Override
  public void sendTo(Set<String> nodes, AbstractGroupMessage msg) {
    sendAll(msg, members.keySet().stream().filter(id -> nodes.contains(id.getName())).collect(toSet()));
  }

  @Override
  public void sendToWithSentCallback(NodeID node, AbstractGroupMessage msg, Runnable sentCallback) throws GroupException {
    internalSendTo(node, msg, sentCallback);
  }

  private void internalSendTo(NodeID node, AbstractGroupMessage msg, Runnable sentCallback) throws GroupException {
    TCGroupMember member = getMember(node);
    if (member != null && member.isReady()) {
      if (msg instanceof L2StateMessage) {
        if (isDebugLogging()) {
          debugInfo("Sending msg to " + node + ", msg: " + msg + ", channel: " + member.getChannel());
        }
      }
      member.send(msg, sentCallback);
    } else {
      if (member != null) {
        closeMember(member);
      }
      throw new GroupException("Send to " + ((member == null) ? "non-exist" : "not ready") + " member of " + node);
    }
  }

  @Override
  public AbstractGroupMessage sendToAndWaitForResponse(NodeID nodeID, AbstractGroupMessage msg) throws GroupException {
    if (isDebugLogging()) {
      debugInfo("Sending to " + nodeID + " and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    TCGroupMember m = getMember(nodeID);
    if ((m != null) && m.isReady()) {
      GroupResponse<AbstractGroupMessage> old = pendingRequests.put(msgID, groupResponse);
      Assert.assertNull(old);
      groupResponse.sendTo(m, msg);
      pendingRequests.remove(msgID);
    } else {
      String errorMsg = "Node " + nodeID + " not present in the group. Ignoring Message : " + msg;
      logger.error(errorMsg);
      if (m != null) {
        closeMember(m);
      }
      throw new GroupException(errorMsg);
    }
    return groupResponse.getResponse(nodeID);

  }

  @Override
  public GroupResponse<AbstractGroupMessage> sendToAndWaitForResponse(Set<String> nodes, AbstractGroupMessage msg) throws GroupException {
    return sendAllAndWaitForResponse(msg, members.keySet().stream().filter(id -> nodes.contains(id.getName())).collect(toSet()));
  }

  @Override
  public GroupResponse<AbstractGroupMessage> sendAllAndWaitForResponse(AbstractGroupMessage msg) throws GroupException {
    return sendAllAndWaitForResponse(msg, members.keySet());
  }

  @Override
  public GroupResponse<AbstractGroupMessage> sendAllAndWaitForResponse(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) throws GroupException {
    if (isDebugLogging()) {
      debugInfo("Sending to " + nodeIDs + " and Waiting for Response : " + msg.getMessageID());
    }
    GroupResponseImpl groupResponse = new GroupResponseImpl();
    MessageID msgID = msg.getMessageID();
    GroupResponse<AbstractGroupMessage> old = pendingRequests.put(msgID, groupResponse);
    Assert.assertNull(old);
    groupResponse.sendAll(msg, nodeIDs);
    pendingRequests.remove(msgID);
    if (isDebugLogging()) {
      debugInfo("Complete from " + nodeIDs + " : " + msg.getMessageID());
    }
    return groupResponse;
  }

  private void openChannel(InetSocketAddress serverAddress, ChannelEventListener listener)
      throws TCTimeoutException, MaxConnectionsExceededException, IOException,
      CommStackMismatchException {

    if (isStopped.get()) return;

    communicationsManager.addClassMapping(TCMessageType.GROUP_WRAPPER_MESSAGE, TCGroupMessageWrapper.class);
    communicationsManager.addClassMapping(TCMessageType.GROUP_HANDSHAKE_MESSAGE, TCGroupHandshakeMessage.class);

    ProductID product = ProductID.DISCOVERY;
    ClientMessageChannel channel = communicationsManager.createClientChannel(product, 2_000 /*  timeout */);

    channel.addListener(listener);
    channel.open(serverAddress);

    handshake(channel);
  }

  public void openChannel(String hostname, int port, ChannelEventListener listener) throws TCTimeoutException,
      MaxConnectionsExceededException, IOException, CommStackMismatchException {
    openChannel(InetSocketAddress.createUnresolved(hostname, port), listener);
  }

  /*
   * Event notification when a new connection setup by channelManager channel opened from dst to src
   */
  @Override
  public void channelCreated(MessageChannel aChannel) {
    if (isStopped.get()) {
      aChannel.close();
      return;
    }
    handshake(aChannel);
  }

  /*
   * Event notification when a connection removed by DSOChannelManager
   */
  @Override
  public void channelRemoved(MessageChannel channel) {
    TCGroupHandshakeStateMachine stateMachine = getHandshakeStateMachine(channel);
    if (stateMachine != null) {
      stateMachine.disconnected();
    }
  }

  private TCGroupMember getMember(MessageChannel channel) {
    TCGroupHandshakeStateMachine stateMachine = getHandshakeStateMachine(channel);
    if (stateMachine != null) {
      ServerID sid = stateMachine.getPeerNodeID();
      if (sid != null) {
        return getMember(sid);
      }
    }
    return members.values().stream().filter(m->m.getChannel() == channel).findFirst().orElse(null);
  }

  private TCGroupMember getMember(NodeID nodeID) {
    return nodeID == null ? null : members.get((ServerID)nodeID);
  }

  public Collection<TCGroupMember> getMembers() {
    return Collections.unmodifiableCollection(members.values());
  }

  public final void setDiscover(TCGroupMemberDiscovery discover) {
    this.discover = discover;
  }

  public TCGroupMemberDiscovery getDiscover() {
    return discover;
  }

  private Timer getHandshakeTimer() {
    return (handshakeTimer);
  }

  /*
   * for testing only
   */
  int size() {
    return members.size();
  }

  public void messageReceived(AbstractGroupMessage message, MessageChannel channel) {

    if (isStopped()) {
      channel.close();
      return;
    }

    TCGroupMember m = getMember(channel);

    if (channel.isClosed()) {
      logger
          .warn(getNodeID() + " recd msg " + message.getMessageID() + " From closed " + channel + " Msg : " + message);
      return;
    }

    while (m == null) {
      TCGroupHandshakeStateMachine stateMachine = getHandshakeStateMachine(channel);
      String errInfo = "Received message for non-exist member from " + channel.getRemoteAddress() + " to "
                       + channel.getLocalAddress() + "; " + stateMachine
                       + "; msg: " + message;
      if (stateMachine != null) {
        // message received after node left
        if (stateMachine.isFailureState()) {
          logger.warn(errInfo);
          return;
        } else {
          m = getMember(channel);
        }
      } else if (isStopped()) {
        return;
      } else {
        throw new RuntimeException(errInfo);
      }
    }

    ServerID from = m.getPeerNodeID();
    MessageID requestID = message.inResponseTo();

    message.setMessageOrginator(from);
    if (requestID.isNull() || !notifyPendingRequests(requestID, message, from)) {
      fireMessageReceivedEvent(from, message);
    }
  }

  private boolean notifyPendingRequests(MessageID requestID, AbstractGroupMessage gmsg, ServerID nodeID) {
    GroupResponseImpl response = pendingRequests.get(requestID);
    if (response != null) {
      response.addResponseFrom(nodeID, gmsg);
      return true;
    }
    return false;
  }

  private static void validateExternalizableClass(Class<? extends GroupMessage> msgClass) {
    String name = msgClass.getName();
    try {
      Constructor<? extends GroupMessage> cons = msgClass.getDeclaredConstructor(new Class[0]);
      if ((cons.getModifiers() & Modifier.PUBLIC) == 0) { throw new AssertionError(
                                                                                   name
                                                                                       + " : public no arg constructor not found"); }
    } catch (NoSuchMethodException ex) {
      throw new AssertionError(name + " : public no arg constructor not found");
    }
  }

  @Override
  public <N extends AbstractGroupMessage> void registerForMessages(Class<? extends N> msgClass, GroupMessageListener<N> listener) {
    validateExternalizableClass(msgClass);
    GroupMessageListener<?> prev = messageListeners.put(msgClass.getName(), listener);
    if (prev != null) {
      logger.warn("Previous listener removed : " + prev);
    }
  }

  @Override
  public <N extends AbstractGroupMessage> void routeMessages(Class<? extends N> msgClass, Sink<N> sink) {
    registerForMessages(msgClass, new RouteGroupMessagesToSink<>(msgClass.getName(), sink));
  }

  // Suppress the case to the listener to receive GroupMessage.
  @SuppressWarnings("unchecked")
  private void fireMessageReceivedEvent(ServerID from, GroupMessage msg) {
    GroupMessageListener<? extends GroupMessage> listener = messageListeners.get(msg.getClass().getName());
    if (listener != null) {
      ((GroupMessageListener<GroupMessage>)listener).messageReceived(from, msg);
    } else {
      String errorMsg = "No Route for " + msg + " from " + from;
      errorMsg += " " + msg.getClass().getName() + " " + messageListeners.keySet();
      logger.error(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }

  @Override
  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor) {
    this.zapNodeRequestProcessor = processor;
  }

  @Override
  public void zapNode(NodeID nodeID, int type, String reason) {
    zappedSet.add(nodeID);
    TCGroupMember m = getMember(nodeID);
    if (m == null) {
      logger.warn("Ignoring Zap node request since Member is null");
    } else if (!zapNodeRequestProcessor.acceptOutgoingZapNodeRequest(nodeID, type, reason)) {
      logger.warn("Ignoring Zap node request since " + zapNodeRequestProcessor + " asked us to : " + nodeID
                  + " type = " + type + " reason = " + reason);
    } else {
      long weights[] = zapNodeRequestProcessor.getCurrentNodeWeights();
      logger.warn("Zapping node : " + nodeID + " type = " + type + " reason = " + reason + " my weight = "
                  + Arrays.toString(weights));
      AbstractGroupMessage msg = GroupZapNodeMessageFactory.createGroupZapNodeMessage(type, reason, weights);
      try {
        // Note that we have no interest in the sent callback for the zap path.
        sendTo(nodeID, msg);
      } catch (GroupException e) {
        logger.error("Error sending ZapNode Request to " + nodeID + " msg = " + msg);
      }
    }
  }

  private boolean isZappedNode(NodeID nodeID) {
    return (zappedSet.contains(nodeID));
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("className", this.getClass().getName());
    map.put("communications", this.communicationsManager.getStateMap());

    Map<String, Object> memberReport = new LinkedHashMap<>();
    map.put("members", memberReport);
    for (Entry<ServerID, TCGroupMember> entry : this.members.entrySet()) {
      memberReport.put(entry.getKey().toString(), entry.getValue());
    }

    List<Object> zapped = new ArrayList<>(this.zappedSet.size());
    map.put("zapped", zapped);
    this.zappedSet.forEach(node->zapped.add(node));
    return map;
  }

  private class GroupResponseImpl implements GroupResponse<AbstractGroupMessage> {

    private final Set<ServerID>      waitFor   = new HashSet<>();
    private final List<AbstractGroupMessage> responses = new ArrayList<>();

    GroupResponseImpl() {
    }

    @Override
    public synchronized List<AbstractGroupMessage> getResponses() {
      Assert.assertTrue(waitFor.isEmpty());
      return responses;
    }

    @Override
    public synchronized AbstractGroupMessage getResponse(NodeID nodeID) {
      Assert.assertTrue(waitFor.isEmpty());
      for (AbstractGroupMessage msg : responses) {
        if (nodeID.equals(msg.messageFrom())) {
          return msg;
        }
      }
      logger.warn("Missing response message from " + nodeID);
      return null;
    }

    public synchronized void sendTo(TCGroupMember member, AbstractGroupMessage msg) throws GroupException {
      if (member.isReady()) {
        Assert.assertNotNull(member.getPeerNodeID());
        waitFor.add(member.getPeerNodeID());
        // TODO:  Determine if the callers of this method want the sent callback.
        Runnable sentCallback = null;
        member.send(msg, sentCallback);
      } else {
        closeMember(member);
        throw new GroupException("Send to a not ready member " + member);
      }
      waitForResponses(getNodeID());
    }

    // public synchronized void sendAll(GroupMessage msg) {
    // sendAll(msg, manager.members.keySet());
    // }

    public synchronized void sendAll(AbstractGroupMessage msg, Set<? extends NodeID> nodeIDs) throws GroupException {
      final boolean debug = msg instanceof L2StateMessage;
      for (TCGroupMember m : getMembers()) {
        if (!nodeIDs.contains(m.getPeerNodeID())) {
          if (debug) {
            if (isDebugLogging()) {
              debugInfo("Not sending msg to " + m.getPeerNodeID() + ", msg: " + msg + ", channel: " + m.getChannel());
            }
          }
          continue;
        }
        if (m.isReady()) {
          Assert.assertNotNull(m.getPeerNodeID());
          waitFor.add(m.getPeerNodeID());
          if (debug) {
            if (isDebugLogging()) {
              debugInfo("Sending msg to " + m.getPeerNodeID() + ", msg: " + msg + ", channel: " + m.getChannel());
            }
          }
          m.sendIgnoreNotReady(msg);
        } else {
          logger.warn("SendAllAndWait to a not ready member " + m);
        }
      }
      waitForResponses(getNodeID());
    }

    public synchronized void addResponseFrom(ServerID nodeID, AbstractGroupMessage gmsg) {
      if (!waitFor.remove(nodeID)) {
        String message = "Recd response from a member not in list : " + nodeID + " : waiting For : " + waitFor
                         + " msg : " + gmsg;
        logger.error(message);
        throw new AssertionError(message);
      }
      if (gmsg instanceof L2StateMessage) {
        if (isDebugLogging()) {
          debugInfo("Received msg from: " + nodeID + ", msg: " + gmsg);
        }
      }
      responses.add(gmsg);
      notifyAll();
    }

    public synchronized void notifyMemberDead(TCGroupMember member) {
      logger.warn("Remove dead member from waitFor response list, dead member: " + member.getPeerNodeID());
      waitFor.remove(member.getPeerNodeID());
      notifyAll();
    }

    private void waitForResponses(ServerID sender) throws GroupException {
      long start = System.currentTimeMillis();
      while (!waitFor.isEmpty() && !isStopped()) {
        try {
          this.wait(5000);
          long end = System.currentTimeMillis();
          if (!waitFor.isEmpty() && (end - start) > 5000) {
            logger.warn(sender + " Still waiting for response from " + waitFor + ". Waited for " + (end - start)
                        + " ms");
            Iterator<ServerID> waiting = waitFor.iterator();
            while (waiting.hasNext()) {
              ServerID current = waiting.next();
              TCGroupMember member = getMember(current);
              if (member == null) {
                logger.warn("server {} is missing from the group, no longer waiting for response message", current);
                waiting.remove();
              } else if(!memberReceiver.containsKey(member.getChannel())) {
                logger.warn("closing {}, no receiver available for message", current);
                closeMember(current);
              }
            }
          }
        } catch (InterruptedException e) {
          throw new GroupException(e);
        }
      }
      if (isStopped()) {
        waitFor.clear();
      }
    }
  }

  private final class ZapNodeRequestRouter implements GroupMessageListener<GroupZapNodeMessage> {

    @Override
    public void messageReceived(NodeID fromNode, GroupZapNodeMessage zapMsg) {
      zapNodeRequestProcessor.incomingZapNodeRequest(zapMsg.messageFrom(), zapMsg.getZapNodeType(), zapMsg.getReason(),
                                                     zapMsg.getWeights());
    }
  }

  private synchronized TCGroupHandshakeStateMachine getOrCreateHandshakeStateMachine(MessageChannel channel) {
    TCGroupHandshakeStateMachine stateMachine = (TCGroupHandshakeStateMachine) channel
        .getAttachment(HANDSHAKE_STATE_MACHINE_TAG);
    if (stateMachine == null) {
      if (isDebugLogging()) {
        debugInfo("Creating handshake state machine for channel: " + channel);
      }
      stateMachine = new TCGroupHandshakeStateMachine(this, channel, getNodeID(), weightGeneratorFactory, relayLocation, version);
      channel.addAttachment(HANDSHAKE_STATE_MACHINE_TAG, stateMachine, false);
      channel.addListener(new HandshakeChannelEventListener(stateMachine));
      if (channel.isOpen()) {
        stateMachine.start();
      } else {
        stateMachine.disconnected();
      }
    }
    Assert.assertNotNull(stateMachine);
    return stateMachine;
  }

  private synchronized TCGroupHandshakeStateMachine getHandshakeStateMachine(MessageChannel channel) {
    TCGroupHandshakeStateMachine stateMachine = (TCGroupHandshakeStateMachine) channel
        .getAttachment(HANDSHAKE_STATE_MACHINE_TAG);
    return stateMachine;
  }

  /*
   * monitor channel events while doing group member handshaking
   */
  private static class HandshakeChannelEventListener implements ChannelEventListener {
    final private TCGroupHandshakeStateMachine stateMachine;

    HandshakeChannelEventListener(TCGroupHandshakeStateMachine stateMachine) {
      this.stateMachine = stateMachine;
    }

    @Override
    public void notifyChannelEvent(ChannelEvent event) {
      if (event.getChannel() == stateMachine.getChannel()) {
        if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
            || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
          stateMachine.disconnected();
        }
      }
    }
  }

  /*
   * TCGroupHandshakeStateMachine -- State machine for group handshaking
   */
  private static class TCGroupHandshakeStateMachine {
    private final HandshakeState     STATE_NEW         = new HandshakeState("NEW");
    private final HandshakeState     STATE_NODEID         = new NodeIDState();
    private final HandshakeState     STATE_TRY_ADD_MEMBER = new TryAddMemberState();
    private final HandshakeState     STATE_ACK_OK         = new AckOkState();
    private final HandshakeState     STATE_SUCCESS        = new SuccessState();
    private final HandshakeState     STATE_FAILURE        = new FailureState();

    private final static long        HANDSHAKE_TIMEOUT;
    static {
      HANDSHAKE_TIMEOUT = TCPropertiesImpl.getProperties()
          .getLong(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_HANDSHAKE_TIMEOUT);
    }

    private final TCGroupManagerImpl manager;
    private final MessageChannel     channel;
    private final ServerID           localNodeID;
    private final WeightGeneratorFactory weightGeneratorFactory;
    private final InetSocketAddress             relay;
    private final String               version;

    private HandshakeMonitor         current;
    private ServerID                 peerNodeID;
    private TimerTask                timerTask;
    private TCGroupMember            member;

    public TCGroupHandshakeStateMachine(TCGroupManagerImpl manager, MessageChannel channel, ServerID localNodeID,
                                        WeightGeneratorFactory weightGeneratorFactory, InetSocketAddress relay, String version) {
      this.manager = manager;
      this.channel = channel;
      this.localNodeID = localNodeID;
      this.weightGeneratorFactory = weightGeneratorFactory;
      this.relay = relay;
      this.version = version;
      this.current = STATE_NEW.createMonitor();
      this.current.complete();
    }

    public final void start() {
      switchToState(initialState());
    }

    public synchronized boolean isFailureState() {
      return (current.getState() == STATE_FAILURE);
    }

    public void execute(TCGroupHandshakeMessage msg) {
      if (isDebugLogging()) {
        debugInfo("[TCGroupHandshakeStateMachine]: Executing state machine, currentState=" + current + ", msg: " + msg
                + ", channel: " + channel);
      }
      getCurrentState().execute(msg);
    }

    private synchronized HandshakeState getCurrentState() {
      return current.getState();
    }

    protected HandshakeState initialState() {
      return (STATE_NODEID);
    }

    private String stateInfo(HandshakeState state) {
      String info = " switching to state: " + state + " channel: " + channel;
      if (member != null) return (member.toString() + info);
      if (peerNodeID == null) return (localNodeID.toString() + info);
      else return (peerNodeID.toString() + " -> " + localNodeID.toString() + info);
    }

    @Override
    public String toString() {
      return "TCGroupHandshakeStateMachine: " + stateInfo(current.getState());
    }

    protected void switchToState(HandshakeState state) {
      Assert.assertNotNull(state);
      if (isDebugLogging()) {
        debugInfo("[TCGroupHandshakeStateMachine]: Attempting to switch state (" + current + "->" + state + "): "
                + stateInfo(state));
      }
      HandshakeMonitor previous = null;
      HandshakeMonitor next = state.createMonitor();
      
      synchronized (this) {
        previous = this.current;
        if (current.getState() == STATE_FAILURE) {
          if (isDebugLogging()) {
            debugWarn("Ignored switching to " + state + " as current is " + current + ", " + stateInfo(state));
          }
          return;
        }
        this.current = next;
      }
      
      if (isDebugLogging()) {
        debugInfo("[TCGroupHandshakeStateMachine]: Entering state: " + state + ", for channel: " + channel);
      }
      previous.waitForCompletion();
      next.complete();
    }

    MessageChannel getChannel() {
      return channel;
    }
    
    ServerID getPeerNodeID() {
      return peerNodeID;
    }

    private synchronized void setTimerTask(long timeout) {
      TimerTask task = new TimerTask() {
        @Override
        public void run() {
          handshakeTimeout();
        }
      };
      timerTask = task;
      Timer timer = manager.getHandshakeTimer();
      timer.purge();
      timer.schedule(task, timeout);
    }

    private synchronized void cancelTimerTask() {
      if (timerTask != null) {
        this.timerTask.cancel();
        timerTask = null;
      }
    }

    void handshakeTimeout() {
      cancelTimerTask();
      synchronized (this) {
        if (current.getState() == STATE_SUCCESS) {
          if (isDebugLogging()) {
            debugInfo("Handshake successed. Ignore timeout " + stateInfo(current.getState()));
          }
          return;
        }
        logger.warn("Group member handshake timeout. " + stateInfo(current.getState()));
      }
      switchToState(STATE_FAILURE);
      channel.close();
    }

    void disconnected() {
      synchronized (this) {
        if (isDebugLogging()) {
          debugWarn("[TCGroupHandshakeStateMachine]: Group member handshake disconnected. " + stateInfo(current.getState())
                + ", for channel: " + channel);
        }
      }
      switchToState(STATE_FAILURE);
    }

    /*
     * HandshakeState -- base class for handshaking states
     */
    private class HandshakeState {
      private final String name;

      public HandshakeState(String name) {
        this.name = name;
      }

      public void enter() {
        // override me if you want
      }

      public void execute(TCGroupHandshakeMessage handshakeMessage) {
        // override me if you want
      }

      @Override
      public String toString() {
        return name;
      }

      public HandshakeMonitor createMonitor() {
        return new HandshakeMonitor() {
          boolean completed = false;
          Thread owner = null;
          @Override
          public HandshakeState getState() {
            return HandshakeState.this;
          }

          @Override
          public synchronized void waitForCompletion() {
            // don't block the current thread's execution if the thread started the execution
            while (Thread.currentThread() != owner && !completed) {
              try {
                wait();
              } catch (InterruptedException ee) {
                L2Utils.handleInterrupted(logger, ee);
              }
            }
          }

          private void start() {
            Assert.assertNull(owner);
            owner = Thread.currentThread();
            getState().enter();
          }

          @Override
          public synchronized void complete() {
            start();
            signalComplete();
          }

          private synchronized void signalComplete() {
            completed = true;
            notifyAll();
          }

        };
      }
    }

    private static interface HandshakeMonitor {
      HandshakeState getState();

      void waitForCompletion();

      void complete();
    }

    /*
     * NodeIDState -- Send NodeID to peer and expecting NodeID from peer.
     */
    private class NodeIDState extends HandshakeState {
      public NodeIDState() {
        super("Read-Peer-NodeID");
      }

      @Override
      public void enter() {
        setTimerTask(HANDSHAKE_TIMEOUT);
        writeNodeIDMessage();
      }

      @Override
      public void execute(TCGroupHandshakeMessage msg) {
        ServerID peer = msg.getNodeID();
        setPeerNodeID(peer);
        boolean valid = manager.getDiscover().isValidClusterNode(peer);
        if (!valid && relay != null) {
          valid = peer.getName().equals(TCSocketAddress.getStringForm(relay));
          if (!valid) {
            InetSocketAddress remote = msg.getChannel().getRemoteAddress();
            valid = remote.getHostName().equals(relay.getHostName()) && remote.getPort() == relay.getPort();
          }
        }
        if (!valid) {
          logger.warn("Drop connection from non-member node {} remote:{} relay:{}", peer, msg.getChannel().getRemoteAddress(), relay);
          switchToState(STATE_FAILURE);
          return;
        }
        /**
         * Restore Connection might have happened from the same peer member. Closing down the old and duplicate channel
         * for the same peer member.
         */
        manager.removeIfMemberReconnecting(peerNodeID);

        switchToState(STATE_TRY_ADD_MEMBER);
      }

      void setPeerNodeID(ServerID peer) {
        peerNodeID = peer;
      }

      void writeNodeIDMessage() {
        TCGroupHandshakeMessage msg = (TCGroupHandshakeMessage) channel
            .createMessage(TCMessageType.GROUP_HANDSHAKE_MESSAGE);
        msg.initializeNodeID(localNodeID, version, weightGeneratorFactory.generateWeightSequence());
        if (isDebugLogging()) {
          debugInfo("Sending group nodeID message to " + channel);
        }
        msg.send();
      }

      boolean checkWeights(TCGroupHandshakeMessage msg) {
        long[] myWeights = weightGeneratorFactory.generateWeightSequence();
        for (int i = 0; i < myWeights.length; i++) {
          if (myWeights[i] > msg.getWeights()[i]) {
            return true;
          } else if (msg.getWeights()[i] > myWeights[i]) {
            return false;
          }
        }
        return false;
      }
    }

    /*
     * TryAddMemberState -- Try to add member to group. Trying by high-priority-node first, low-priority-node adds to
     * group only after high-priority-node added. The priority is valued by NodeID's uuid.
     */
    private class TryAddMemberState extends HandshakeState {
      public TryAddMemberState() {
        super("Try-Add-Member");
      }

      @Override
      public void enter() {
        createMember();
        if (member.isHighPriorityNode()) {
          if (isDebugLogging()) {
            debugInfo("Try-Add-Member: Adding high priority member: " + member);
          }
          member.memberAddingInProcess();
          boolean isAdded = manager.tryAddMember(member);
          if (!isAdded) member.abortMemberAdding();
          signalToJoin(isAdded);
        } else {
          if (isDebugLogging()) {
            debugInfo("Try-Add-Member ignoring member as not high priority: " + member);
          }
        }
      }

      @Override
      public void execute(TCGroupHandshakeMessage msg) {
        boolean isOkToJoin = msg.isOkMessage();
        if (!member.isHighPriorityNode()) {
          if (isDebugLogging()) {
            debugInfo("Try-Add-Member: Adding not-high priority member: " + member);
          }
          if (isOkToJoin) {
            isOkToJoin = manager.tryAddMember(member);
            if (isOkToJoin) {
              member.memberAddingInProcess();
            } else {
              logger.warn("Unexpected bad handshake, abort connection.");
            }
          }
          signalToJoin(isOkToJoin);
        } else {
          if (isDebugLogging()) {
            debugInfo("Try-Add-Member not adding member as its highPriority: " + member);
          }
        }
        if (isOkToJoin) switchToState(STATE_ACK_OK);
        else switchToState(STATE_FAILURE);
      }

      private void createMember() {
        Assert.assertNotNull(localNodeID);
        Assert.assertNotNull(peerNodeID);
        member = new TCGroupMemberImpl(localNodeID, peerNodeID, channel);
      }

      private void signalToJoin(boolean ok) {
        Assert.assertNotNull(member);
        TCGroupHandshakeMessage msg = (TCGroupHandshakeMessage) channel
            .createMessage(TCMessageType.GROUP_HANDSHAKE_MESSAGE);
        if (ok) {
          if (isDebugLogging()) {
            debugInfo("Send ok message to " + member);
          }
          msg.initializeOk();
        } else {
          if (isDebugLogging()) {
            debugInfo("Send deny message to " + member);
          }
          msg.initializeDeny();
        }
        msg.send();
      }

    }

    /*
     * AckOkState -- Ack ok message
     */
    private class AckOkState extends HandshakeState {
      public AckOkState() {
        super("Ack-Ok");
      }

      @Override
      public void enter() {
        member.setReady(true);
        member.notifyMemberAdded();
        ackOk();
      }

      @Override
      public void execute(TCGroupHandshakeMessage msg) {
        if (msg.isAckMessage()) switchToState(STATE_SUCCESS);
        else switchToState(STATE_FAILURE);
      }

      private void ackOk() {
        TCGroupHandshakeMessage msg = (TCGroupHandshakeMessage) channel
            .createMessage(TCMessageType.GROUP_HANDSHAKE_MESSAGE);
        if (isDebugLogging()) {
          debugInfo("Send ack message to " + member);
        }
        msg.initializeAck();
        msg.send();
      }

    }

    /*
     * SucessState -- Both added to group. Fire nodeJoined event.
     */
    private class SuccessState extends HandshakeState {
      public SuccessState() {
        super("Success");
      }

      @Override
      public void enter() {
        cancelTimerTask();
        manager.fireNodeEvent(member, true);
        member.setJoinedEventFired(true);

        if (manager.isZappedNode(member.getPeerNodeID())) {
          logger.info("Aborting previously zapped node " + member);
          manager.zapNode(member.getPeerNodeID(), L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                          "Aborting the zapped node");
        }

      }
    }

    /*
     * FailureState -- Unsuccessful handshaking or member disappeared. Fire nodeLeft event if member is in group.
     */
    private class FailureState extends HandshakeState {
      public FailureState() {
        super("Failure");
      }

      @Override
      public void enter() {
        cancelTimerTask();
        if (member != null) {
          member.abortMemberAdding();
          manager.closeMember(member);
        } else {
          channel.close();
        }
      }
    }

  }

  /*
   * for testing purpose only
   */
  void addZappedNode(NodeID nodeID) {
    zappedSet.add(nodeID);
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    return this.discover.isServerConnected(nodeName);
  }
  
  public int getBufferCount() {
    return connectionManager.getBufferCount();
  }

  private static void debugInfo(String message) {
    L2DebugLogging.log(logger, LogLevel.INFO, message, null);
  }
  
  private static boolean isDebugLogging() {
    return L2DebugLogging.isDebugLogging();
  }

  private static void debugWarn(String message) {
    L2DebugLogging.log(logger, LogLevel.WARN, message, null);
  }

}
