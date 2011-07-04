/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.l2.L2DebugLogging;
import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ConcurrentHashMap;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private static final TCLogger                    logger                  = TCLogging
                                                                               .getLogger(TCGroupMemberDiscoveryStatic.class);
  private final static long                        DISCOVERY_INTERVAL_MS;
  static {
    DISCOVERY_INTERVAL_MS = TCPropertiesImpl.getProperties()
        .getLong(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_DISCOVERY_INTERVAL);
  }

  private final AtomicBoolean                      running                 = new AtomicBoolean(false);
  private final AtomicBoolean                      stopAttempt             = new AtomicBoolean(false);
  private final Map<String, DiscoveryStateMachine> nodeStateMap            = new ConcurrentHashMap<String, DiscoveryStateMachine>();
  private final TCGroupManagerImpl                 manager;
  private Node                                     local;
  private Integer                                  joinedNodes             = 0;
  private final HashSet<String>                    nodeThreadConnectingSet = new HashSet<String>();

  public TCGroupMemberDiscoveryStatic(TCGroupManagerImpl manager) {
    this.manager = manager;
  }

  public void setupNodes(Node local, Node[] nodes) {
    this.local = local;
    for (Node node : nodes) {
      DiscoveryStateMachine stateMachine = new DiscoveryStateMachine(node);
      DiscoveryStateMachine old = nodeStateMap.put(getNodeName(node), stateMachine);
      Assert.assertNull("Duplicate nodes specified in config, please check " + getNodeName(node), old);
      stateMachine.start();
    }
  }

  public void addNode(Node node) {
    DiscoveryStateMachine stateMachine = new DiscoveryStateMachine(node);
    DiscoveryStateMachine old = nodeStateMap.put(getNodeName(node), stateMachine);
    Assert.assertNull("Duplicate nodes specified in config, please check " + getNodeName(node), old);
    stateMachine.start();

    if (stateMachine.isTimeToConnect()) {
      stateMachine.connecting();
      discoveryPut(stateMachine);
    }
    synchronized (this) {
      this.notifyAll();
    }
  }

  public void removeNode(Node node) {
    DiscoveryStateMachine old = nodeStateMap.remove(getNodeName(node));
    Assert.assertNotNull("Tried removing node which was not present", old);
  }

  private String getNodeName(Node node) {
    return node.getServerNodeName();
  }

  public boolean isValidClusterNode(NodeID nodeID) {
    String nodeName = ((ServerID) nodeID).getName();
    return (nodeStateMap.get(nodeName) != null);
  }

  private void discoveryPut(DiscoveryStateMachine stateMachine) {
    manager.getDiscoveryHandlerSink().add(stateMachine);
  }

  public void discoveryHandler(EventContext context) {
    DiscoveryStateMachine stateMachine = (DiscoveryStateMachine) context;
    Assert.assertNotNull(stateMachine);
    Node node = stateMachine.getNode();
    String serverNodeName = node.getServerNodeName();

    if (stateMachine.isMemberInGroup() || stopAttempt.get()) { return; }

    if (!addNodeToConnectingSet(serverNodeName)) {
      logger.warn("Discovery for " + node + " is in progress. skipping it.");
      return;
    } else {
      debugInfo("Added node to connecting set: " + node);
    }

    try {
      debugInfo(getLocalNodeID().toString() + " opening channel to " + node);
      manager.openChannel(node.getHost(), node.getGroupPort(), stateMachine);
      removeNodeFromConnectingSet(serverNodeName);
      stateMachine.connected();
    } catch (TCTimeoutException e) {
      removeNodeFromConnectingSet(serverNodeName);
      stateMachine.connectTimeout();
      stateMachine.loggerWarn("Node:" + node + " not up. " + e.getMessage());
    } catch (UnknownHostException e) {
      removeNodeFromConnectingSet(serverNodeName);
      stateMachine.unknownHost();
      stateMachine.loggerWarn("Node:" + node + " not up. Unknown host.");
    } catch (MaxConnectionsExceededException e) {
      removeNodeFromConnectingSet(serverNodeName);
      stateMachine.maxConnExceed();
      stateMachine.loggerWarn("Node:" + node + " not up. " + e.getMessage());
    } catch (CommStackMismatchException e) {
      removeNodeFromConnectingSet(serverNodeName);
      stateMachine.commStackMismatch();
      stateMachine.loggerWarn("Node:" + node + " not up. " + e.getMessage());
    } catch (IOException e) {
      removeNodeFromConnectingSet(serverNodeName);
      stateMachine.connetIOException();
      stateMachine.loggerWarn("Node:" + node + " not up. IOException occured:" + e.getMessage());
    } catch (Throwable t) {
      removeNodeFromConnectingSet(serverNodeName);
      // catch all throwables to prevent discover from dying
      stateMachine.throwableException();
      stateMachine.loggerWarn("Node:" + node + " not up. Exception occured:" + t.getMessage());
    }
  }

  NodeID getLocalNodeID() {
    return (manager.getLocalNodeID());
  }

  public void start() throws GroupException {
    if (nodeStateMap.isEmpty()) { throw new GroupException("No nodes"); }

    if (running.getAndSet(true)) {
      Assert.failure("Not to start discovert second time");
    }

    manager.registerForGroupEvents(this);

    // run once before deamon thread does job
    openChannels();

    Thread discover = new Thread(new Runnable() {
      public void run() {
        while (!stopAttempt.get()) {
          openChannels();
          ThreadUtil.reallySleep(DISCOVERY_INTERVAL_MS);
          pauseDiscovery();
        }
        running.set(false);
      }
    }, "Static Member discovery");
    discover.setDaemon(true);
    discover.start();
  }

  /*
   * Open channel to unconnected nodes
   */
  protected void openChannels() {

    for (DiscoveryStateMachine stateMachine : nodeStateMap.values()) {
      // skip local one
      if (local.equals(stateMachine.getNode())) continue;

      if (stateMachine.isTimeToConnect()) {
        stateMachine.connecting();
        discoveryPut(stateMachine);
      }
    }
  }

  /**
   * Adds a node to the discovery in progress set.
   * 
   * @param nodeName : peer node for which discovery is happening
   * @return boolean : true - discovery can be performed for the added node ; false - discovery is already in progress
   *         for this node;
   */
  private boolean addNodeToConnectingSet(String nodeName) {
    synchronized (local) {
      return nodeThreadConnectingSet.add(nodeName);
    }
  }

  private void removeNodeFromConnectingSet(String nodeName) {
    synchronized (local) {
      nodeThreadConnectingSet.remove(nodeName);
      if (nodeThreadConnectingSet.size() == 0) local.notifyAll();
    }
  }

  private void waitTillNoConnecting(long timeout) {
    synchronized (local) {
      if (nodeThreadConnectingSet.size() > 0) {
        try {
          local.wait(timeout);
        } catch (InterruptedException e) {
          logger.debug("Timeouted while waiting for connecting completed");
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public void stop(long timeout) {
    stopAttempt.set(true);

    // wait for all connections completed to avoid
    // IllegalStateException in TCConnectionManagerJDK14.checkShutdown()
    waitTillNoConnecting(timeout);
  }

  public Node getLocalNode() {
    return local;
  }

  public synchronized void nodeJoined(NodeID nodeID) {
    String nodeName = ((ServerID) nodeID).getName();
    nodeStateMap.get(nodeName).nodeJoined();
    joinedNodes++;
  }

  public synchronized void nodeLeft(NodeID nodeID) {
    joinedNodes--;
    String nodeName = ((ServerID) nodeID).getName();
    nodeStateMap.get(nodeName).nodeLeft();
    notifyAll();
  }

  public synchronized void pauseDiscovery() {
    while (joinedNodes == (nodeStateMap.size() - 1) && !stopAttempt.get()) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static class DiscoveryStateMachine implements EventContext, ChannelEventListener {
    private final DiscoveryState STATE_INIT            = new InitState();
    private final DiscoveryState STATE_CONNECTING      = new ConnectingState();
    private final DiscoveryState STATE_CONNECTED       = new ConnectedState();
    private final DiscoveryState STATE_CONNECT_TIMEOUT = new ConnectTimeoutState();
    private final DiscoveryState STATE_MAX_CONNECTION  = new MaxConnExceedState();
    private final DiscoveryState STATE_STACK_MISMATCH  = new CommStackMismatchState();
    private final DiscoveryState STATE_IO_EXCEPTION    = new IOExceptionState();
    private final DiscoveryState STATE_THROWABLE_EXCEP = new ThrowableExceptionState();
    private final DiscoveryState STATE_UNKNOWN_HOST    = new UnknownHostState();
    private final DiscoveryState STATE_MEMBER_IN_GROUP = new MemberInGroupState();

    private DiscoveryState       current;
    private DiscoveryState       previousBadState;

    private final Node           node;
    private int                  badCount;
    private long                 timestamp;
    private long                 previousLogTimeStamp;
    private MessageChannel       connectedChannel;

    public DiscoveryStateMachine(Node node) {
      this.node = node;
    }

    // reduce logging to about once per min
    public void loggerWarn(String message) {
      if (System.currentTimeMillis() > (previousLogTimeStamp + 60000)) {
        logger.warn(message);
        previousLogTimeStamp = System.currentTimeMillis();
      }
    }

    public final void start() {
      switchToState(initialState());
    }

    protected DiscoveryState initialState() {
      return (STATE_INIT);
    }

    protected synchronized void switchToState(DiscoveryState state) {
      Assert.assertNotNull(state);
      debugInfo("DiscoverStateMachine [" + node + "]: switching to state: " + state);
      this.current = state;
      state.enter();
    }

    protected synchronized boolean switchToStateFrom(DiscoveryState from, DiscoveryState to) {
      Assert.assertNotNull(from);
      Assert.assertNotNull(to);
      if (this.current == from) {
        this.current = to;
        to.enter();
        return true;
      } else {
        logger.warn("DiscoverStateMachine [" + node + "]: Ignored switching state from: " + from + ", to: " + to
                    + ", current: " + current);
        return false;
      }
    }

    Node getNode() {
      return node;
    }

    synchronized boolean isMemberInGroup() {
      return (current == STATE_MEMBER_IN_GROUP);
    }

    synchronized boolean isTimeToConnect() {
      return current.isTimeToConnect();
    }

    void connecting() {
      Assert.eval(current != STATE_CONNECTING);
      switchToState(STATE_CONNECTING);
    }

    void connected() {
      switchToStateFrom(STATE_CONNECTING, STATE_CONNECTED);
    }

    private void notifyDisconnected() {
      if (!switchToStateFrom(STATE_CONNECTING, STATE_INIT)) {
        switchToStateFrom(STATE_CONNECTED, STATE_INIT);
      }
    }

    synchronized void badConnect(DiscoveryState state) {
      if (current == STATE_MEMBER_IN_GROUP) { return; }
      switchToState(state);
    }

    void connectTimeout() {
      badConnect(STATE_CONNECT_TIMEOUT);
    }

    void maxConnExceed() {
      badConnect(STATE_MAX_CONNECTION);
    }

    void commStackMismatch() {
      badConnect(STATE_STACK_MISMATCH);
    }

    void connetIOException() {
      badConnect(STATE_IO_EXCEPTION);
    }

    void throwableException() {
      badConnect(STATE_THROWABLE_EXCEP);
    }

    synchronized void unknownHost() {
      if (current == STATE_UNKNOWN_HOST) { return; }
      if (current == STATE_MEMBER_IN_GROUP) { return; }
      switchToState(STATE_UNKNOWN_HOST);
    }

    synchronized void nodeJoined() {
      switchToState(STATE_MEMBER_IN_GROUP);
    }

    synchronized void nodeLeft() {
      switchToState(STATE_INIT);
    }

    /*
     * DiscoveryState -- base class for member discovery state
     */
    private abstract class DiscoveryState {
      private final String name;

      public DiscoveryState(String name) {
        this.name = name;
      }

      public void enter() {
        badCount = 0;
        previousBadState = null;
        previousLogTimeStamp = 0;
      }

      public boolean isTimeToConnect() {
        // override me if you want
        return true;
      }

      @Override
      public String toString() {
        return name;
      }
    }

    /*
     * InitState --
     */
    private class InitState extends DiscoveryState {
      public InitState() {
        super("Init");
      }

      @Override
      public void enter() {
        // do nothing
      }

      @Override
      public boolean isTimeToConnect() {
        return true;
      }
    }

    /*
     * ConnectingState --
     */
    private class ConnectingState extends DiscoveryState {
      public ConnectingState() {
        super("Connecting");
      }

      @Override
      public void enter() {
        // do nothing
      }

      @Override
      public boolean isTimeToConnect() {
        return false;
      }
    }

    /*
     * ConnectedState --
     */
    private class ConnectedState extends DiscoveryState {
      public ConnectedState() {
        super("Connected");
      }

      @Override
      public boolean isTimeToConnect() {
        return false;
      }
    }

    /*
     * BadState -- abstract bad connection
     */
    private abstract class BadState extends DiscoveryState {
      public BadState(String name) {
        super(name);
      }

      @Override
      public void enter() {
        if ((previousBadState == null) || (previousBadState != current)) {
          badCount = 0;
          previousBadState = current;
          previousLogTimeStamp = 0;
        } else {
          ++badCount;
        }
      }

      @Override
      public boolean isTimeToConnect() {
        // check 60 times then every min
        if (badCount < 60) { return true; }
        if (System.currentTimeMillis() > (timestamp + DISCOVERY_INTERVAL_MS * 60)) {
          timestamp = System.currentTimeMillis();
          return true;
        } else {
          return false;
        }
      }
    }

    /*
     * VeryBadState -- abstract very bad connection, no more attempts to connect
     */
    private abstract class VeryBadState extends DiscoveryState {
      public VeryBadState(String name) {
        super(name);
      }

      @Override
      public boolean isTimeToConnect() {
        return false;
      }
    }

    /*
     * ConnetTimeoutState --
     */
    private class ConnectTimeoutState extends BadState {
      public ConnectTimeoutState() {
        super("Connection-Timeouted");
      }
    }

    /*
     * MaxConnExceedState --
     */
    private class MaxConnExceedState extends BadState {
      public MaxConnExceedState() {
        super("Max-Connections-Exceed");
      }
    }

    /*
     * CommStackMismatchState --
     */
    private class CommStackMismatchState extends VeryBadState {
      public CommStackMismatchState() {
        super("Comm-Stack-Mismatch");
      }
    }

    /*
     * IOExceptionState --
     */
    private class IOExceptionState extends BadState {
      public IOExceptionState() {
        super("IO-Exception");
      }
    }

    /*
     * ThrowableExceptionState --
     */
    private class ThrowableExceptionState extends BadState {
      public ThrowableExceptionState() {
        super("Connection-Throwable");
      }
    }

    /*
     * UnknowHostState --
     */
    private class UnknownHostState extends DiscoveryState {
      public UnknownHostState() {
        super("Unknown-Host");
      }

      @Override
      public void enter() {
        if ((previousBadState == null) || (previousBadState != current)) {
          super.enter();
          timestamp = System.currentTimeMillis();
        }
      }

      @Override
      public boolean isTimeToConnect() {
        // check every 5 min
        if (System.currentTimeMillis() > (timestamp + 1000 * 60 * 5)) {
          timestamp = System.currentTimeMillis();
          return true;
        } else {
          return false;
        }
      }
    }

    /*
     * MemberInGroup -- A valid connection established
     */
    private class MemberInGroupState extends DiscoveryState {
      public MemberInGroupState() {
        super("Member-In-Group");
      }

      @Override
      public boolean isTimeToConnect() {
        return false;
      }
    }

    public void notifyChannelEvent(ChannelEvent event) {
      if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        synchronized (this) {
          this.connectedChannel = event.getChannel();
        }
      } else if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
                 || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
        synchronized (this) {
          if (canNotifyDisconnect(event)) {
            notifyDisconnected();
            this.connectedChannel = null;
          }
        }
      }
    }

    private synchronized boolean canNotifyDisconnect(ChannelEvent event) {
      MessageChannel eventChannel = event.getChannel();
      boolean rv = (this.connectedChannel == null) ? false
          : ((this.connectedChannel == eventChannel)
             && (this.connectedChannel.getLocalAddress() == eventChannel.getLocalAddress()) && (this.connectedChannel
              .getRemoteAddress() == eventChannel.getRemoteAddress()));
      return rv;
    }
  }

  public boolean isServerConnected(String nodeName) {
    DiscoveryStateMachine dsm = nodeStateMap.get(nodeName);
    if (dsm == null) { return false; }

    return dsm.isMemberInGroup();
  }

  private static void debugInfo(String message) {
    L2DebugLogging.log(logger, LogLevel.INFO, message, null);
  }
}
