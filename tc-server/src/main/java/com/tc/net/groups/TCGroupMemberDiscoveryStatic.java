/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.groups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.l2.L2DebugLogging;
import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.utils.L2Utils;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.terracotta.server.ServerEnv;

public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private static final Logger logger = LoggerFactory.getLogger(TCGroupMemberDiscoveryStatic.class);
  private final static long                        DISCOVERY_INTERVAL_MS;
  static {
    DISCOVERY_INTERVAL_MS = TCPropertiesImpl.getProperties()
        .getLong(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_DISCOVERY_INTERVAL);
  }

  private final AtomicBoolean                      running                 = new AtomicBoolean(false);
  private final AtomicBoolean                      stopAttempt             = new AtomicBoolean(false);
  private final Map<String, DiscoveryStateMachine> nodeStateMap            = new ConcurrentHashMap<>();
  private final TCGroupManagerImpl                 manager;
  private final Node                                     local;
  private int                                  joinedNodes             = 0;
  private final HashSet<String>                    nodeThreadConnectingSet = new HashSet<>();

  public TCGroupMemberDiscoveryStatic(TCGroupManagerImpl manager, Node local) {
    this.manager = manager;
    this.local = local;
  }

  @Override
  public void setupNodes(Node local, Set<Node> nodes) {
    Assert.assertEquals(this.local, local);
    for (Node node : nodes) {
      DiscoveryStateMachine stateMachine = new DiscoveryStateMachine(node);
      DiscoveryStateMachine old = nodeStateMap.put(getNodeName(node), stateMachine);
      Assert.assertNull("Duplicate nodes specified in config, please check " + getNodeName(node), old);
      stateMachine.start();
    }
  }

  private void addNode(Node node) {
    DiscoveryStateMachine stateMachine = new DiscoveryStateMachine(node);
    stateMachine.start();
    DiscoveryStateMachine old = nodeStateMap.put(getNodeName(node), stateMachine);
    Assert.assertNull("Duplicate nodes specified in config, please check " + getNodeName(node), old);

    if (stateMachine.isTimeToConnect()) {
      stateMachine.connecting();
      discoveryPut(stateMachine);
    }
    synchronized (this) {
      this.notifyAll();
    }
  }

  private void removeNode(Node node) {
    DiscoveryStateMachine old = nodeStateMap.remove(getNodeName(node));
    Assert.assertNotNull("Tried removing node which was not present", old);
  }

  private String getNodeName(Node node) {
    return TCSocketAddress.getStringForm(InetSocketAddress.createUnresolved(node.getHost(), node.getPort()));
  }

  @Override
  public boolean isValidClusterNode(NodeID nodeID) {
    String nodeName = ((ServerID) nodeID).getName();
    return (nodeStateMap.get(nodeName) != null);
  }

  private void discoveryPut(DiscoveryStateMachine stateMachine) {
    manager.getDiscoveryHandlerSink().addToSink(stateMachine);
  }

  @Override
  public void discoveryHandler(DiscoveryStateMachine stateMachine) {
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

  @Override
  public void start() throws GroupException {
    if (nodeStateMap.isEmpty()) { throw new GroupException("No nodes"); }

    if (running.getAndSet(true)) {
      throw Assert.failure("Do not start discovery second time");
    }

    manager.registerForGroupEvents(this);

    // run once before deamon thread does job
    openChannels();

    Thread discover = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!stopAttempt.get()) {
          openChannels();
          // sleep to allow nodes to connect
          ThreadUtil.reallySleep(DISCOVERY_INTERVAL_MS);
          Set<Node> servers = pauseDiscovery();
          if (!servers.isEmpty()) {
            for (Node n : servers) {
              if (!nodeStateMap.containsKey(getNodeName(n))) {
                addNode(n);
              }
            }
            for (DiscoveryStateMachine n : nodeStateMap.values()) {
              if (!servers.contains(n.getNode())) {
                removeNode(n.getNode());
              }
            }
          }
        }
        running.set(false);
      }
    }, ServerEnv.getServer().getIdentifier() + " - Static Member discovery");
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
      if (nodeThreadConnectingSet.isEmpty()) local.notifyAll();
    }
  }

  private void waitTillNoConnecting(long timeout) {
    synchronized (local) {
      if (!nodeThreadConnectingSet.isEmpty()) {
        try {
          local.wait(timeout);
          if (!nodeThreadConnectingSet.isEmpty()) {
            logger.debug("Timeout occurred while waiting for connecting completed");
          }
        } catch (InterruptedException e) {
          L2Utils.handleInterrupted(logger, e);
        }
      }
    }
  }

  @Override
  public void stop(long timeout) {
    stopAttempt.set(true);
    wake();
    // wait for all connections completed to avoid
    // IllegalStateException in TCConnectionManagerJDK14.checkShutdown()
    waitTillNoConnecting(timeout);
  }

  private synchronized void wake() {
    this.notifyAll();
  }

  @Override
  public Node getLocalNode() {
    return local;
  }

  @Override
  public synchronized void nodeJoined(NodeID nodeID) {
    String nodeName = ((ServerID) nodeID).getName();
    DiscoveryStateMachine n = nodeStateMap.get(nodeName);
    if (n != null) {
      n.nodeJoined();
      joinedNodes++;
    }
  }

  @Override
  public synchronized void nodeLeft(NodeID nodeID) {
    String nodeName = ((ServerID) nodeID).getName();
    DiscoveryStateMachine n = nodeStateMap.get(nodeName);
    if (n != null) {
      joinedNodes--;
      nodeStateMap.get(nodeName).nodeLeft();
      notifyAll();
    }
  }

  public synchronized Set<Node> pauseDiscovery() {
    while (!stopAttempt.get()) {
      try {
        this.wait(DISCOVERY_INTERVAL_MS);
        try {
          Set<Node> servers = manager.getConfiguredServers();
          if (joinedNodes != servers.size() - 1) {
            return servers;
          }
        } catch (Throwable t) {
          logger.info("discovery configuration update failed. pausing to try again", t);
        }
      } catch (InterruptedException e) {
        L2Utils.handleInterrupted(logger, e);
      }
    }
    return Collections.emptySet();
  }

  @Override
  public boolean isServerConnected(String nodeName) {
    DiscoveryStateMachine dsm = nodeStateMap.get(nodeName);
    if (dsm == null) { return false; }

    return dsm.isMemberInGroup();
  }
  
  private static void debugInfo(String message) {
    L2DebugLogging.log(logger, LogLevel.INFO, message, null);
  }
}
