/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private static final TCLogger                    logger                          = TCLogging
                                                                                       .getLogger(TCGroupMemberDiscoveryStatic.class);
  private final static long                        DISCOVERY_INTERVAL_MS;
  public static final String                       NHA_TCGPCOMM_DISCOVERY_INTERVAL = "l2.nha.tcgroupcomm.discovery.interval";
  static {
    DISCOVERY_INTERVAL_MS = TCPropertiesImpl.getProperties().getLong(NHA_TCGPCOMM_DISCOVERY_INTERVAL);
  }

  private final AtomicBoolean                      running                         = new AtomicBoolean(false);
  private final AtomicBoolean                      stopAttempt                     = new AtomicBoolean(false);
  private final Map<String, DiscoveryStateMachine> nodeStateMap                    = Collections
                                                                                       .synchronizedMap(new HashMap<String, DiscoveryStateMachine>());
  private final TCGroupManagerImpl                 manager;
  private Node                                     local;
  private Integer                                  joinedNodes                     = 0;
  private Integer                                  connectingCount                 = 0;

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

  private String getNodeName(Node node) {
    return (TCGroupManagerImpl.makeGroupNodeName(node.getHost(), node.getPort()));
  }

  public boolean isValidClusterNode(NodeID nodeID) {
    String nodeName = ((NodeIDImpl) nodeID).getName();
    return (nodeStateMap.get(nodeName) != null);
  }

  private void discoveryPut(DiscoveryStateMachine stateMachine) {
    manager.getDiscoveryHandlerSink().add(stateMachine);
  }

  public void discoveryHandler(EventContext context) {
    DiscoveryStateMachine stateMachine = (DiscoveryStateMachine) context;
    Assert.assertNotNull(stateMachine);
    Node node = stateMachine.getNode();

    if (stateMachine.isMemberInGroup() || stopAttempt.get()) { return; }

    try {
      if (logger.isDebugEnabled()) logger.debug(getLocalNodeID().toString() + " opens channel to " + node);
      incConnectingCount();
      manager.openChannel(node.getHost(), node.getPort(), stateMachine);
      stateMachine.connected();
    } catch (TCTimeoutException e) {
      stateMachine.connectTimeout();
      stateMachine.loggerWarn("Node:" + node + " " + e);
    } catch (UnknownHostException e) {
      stateMachine.unknownHost();
      stateMachine.loggerWarn("Node:" + node + " " + e);
    } catch (MaxConnectionsExceededException e) {
      stateMachine.maxConnExceed();
      stateMachine.loggerWarn("Node:" + node + " " + e);
    } catch (IOException e) {
      stateMachine.connetIOException();
      stateMachine.loggerWarn("Node:" + node + " " + e);
    } finally {
      decConnectingCount();
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

  private void incConnectingCount() {
    synchronized (local) {
      ++connectingCount;
    }
  }

  private void decConnectingCount() {
    synchronized (local) {
      --connectingCount;
      if (connectingCount == 0) local.notifyAll();
    }
  }

  private void waitTillNoConnecting(long timeout) {
    synchronized (local) {
      if (connectingCount > 0) {
        try {
          local.wait(timeout);
        } catch (InterruptedException e) {
          logger.debug("Timeouted while waiting for connecting completed");
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
    String nodeName = ((NodeIDImpl) nodeID).getName();
    nodeStateMap.get(nodeName).nodeJoined();
    joinedNodes++;
  }

  public synchronized void nodeLeft(NodeID nodeID) {
    joinedNodes--;
    String nodeName = ((NodeIDImpl) nodeID).getName();
    nodeStateMap.get(nodeName).nodeLeft();
    notifyAll();
  }

  public synchronized void pauseDiscovery() {
    while (joinedNodes == (nodeStateMap.size() - 1) && !stopAttempt.get()) {
      try {
        wait();
      } catch (InterruptedException e) {
        //
      }
    }
  }

  private static class DiscoveryStateMachine implements EventContext, ChannelEventListener {
    private final DiscoveryState STATE_INIT            = new InitState();
    private final DiscoveryState STATE_CONNECTING      = new ConnectingState();
    private final DiscoveryState STATE_CONNECTED       = new ConnectedState();
    private final DiscoveryState STATE_CONNECT_TIMEOUT = new ConnectTimeoutState();
    private final DiscoveryState STATE_MAX_CONNECTION  = new MaxConnExceedState();
    private final DiscoveryState STATE_IO_EXCEPTION    = new IOExceptionState();
    private final DiscoveryState STATE_UNKNOWN_HOST    = new UnknownHostState();
    private final DiscoveryState STATE_MEMBER_IN_GROUP = new MemberInGroupState();

    private DiscoveryState       current;
    private DiscoveryState       previousBadState;

    private final Node           node;
    private int                  badCount;
    private long                 timestamp;
    private long                 previousLogTimeStamp;

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
        logger.warn("Ignore switching " + node + ":" + current + " from " + from + " to " + to);
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

    void notifyDisconnected() {
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

    void connetIOException() {
      badConnect(STATE_IO_EXCEPTION);
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

      public void enter() {
        // do nothing
      }

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

      public void enter() {
        // do nothing
      }

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

      public void enter() {
        if ((previousBadState == null) || (previousBadState != current)) {
          badCount = 0;
          previousBadState = current;
          previousLogTimeStamp = 0;
        } else {
          ++badCount;
        }
      }

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
     * IOExceptionState --
     */
    private class IOExceptionState extends BadState {
      public IOExceptionState() {
        super("IO-Exception");
      }
    }

    /*
     * UnknowHostState --
     */
    private class UnknownHostState extends DiscoveryState {
      public UnknownHostState() {
        super("Unknown-Host");
      }

      public void enter() {
        if ((previousBadState == null) || (previousBadState != current)) {
          super.enter();
          timestamp = System.currentTimeMillis();
        }
      }

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

      public boolean isTimeToConnect() {
        return false;
      }
    }

    public void notifyChannelEvent(ChannelEvent event) {
      if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        //
      } else if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
                 || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
        notifyDisconnected();
      }
    }

  }

}