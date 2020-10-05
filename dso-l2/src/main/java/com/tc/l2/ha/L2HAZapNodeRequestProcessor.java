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
package com.tc.l2.ha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.exception.TCShutdownServerException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.exception.ZapServerNodeException;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.Enrollment;
import com.tc.l2.state.StateManager;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.ZapEventListener;
import com.tc.net.groups.ZapNodeRequestProcessor;
import com.tc.objectserver.persistence.ClusterStatePersistor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class L2HAZapNodeRequestProcessor implements ZapNodeRequestProcessor {
  private static final Logger logger = LoggerFactory.getLogger(L2HAZapNodeRequestProcessor.class);

  public static final int              COMMUNICATION_ERROR             = 0x01;
  public static final int              PROGRAM_ERROR                   = 0x02;
  public static final int              NODE_JOINED_WITH_DIRTY_DB       = 0x03;
  public static final int              COMMUNICATION_TO_ACTIVE_ERROR   = 0x04;
  public static final int              PARTIALLY_SYNCED_PASSIVE_JOINED = 0x05;
  public static final int              INSUFFICIENT_RESOURCES          = 0x06;
  public static final int              SPLIT_BRAIN                     = 0xff;

  private final Logger                      consoleLogger;
  private final StateManager                  stateManager;
  private final WeightGeneratorFactory        factory;

  private final GroupManager                  groupManager;
  private final List<ZapEventListener>        listeners                     = new CopyOnWriteArrayList<>();
  private final ClusterStatePersistor         clusterStatePersistor;
  private final ConsistencyManager            consistencyMgr;

  public L2HAZapNodeRequestProcessor(Logger consoleLogger, StateManager stateManager, GroupManager groupManager,
                                     WeightGeneratorFactory factory, ClusterStatePersistor clusterStatePersistor, ConsistencyManager consistency) {
    this.consoleLogger = consoleLogger;
    this.stateManager = stateManager;
    this.groupManager = groupManager;
    this.factory = factory;
    this.clusterStatePersistor = clusterStatePersistor;
    this.consistencyMgr = consistency;
  }

  @Override
  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason) {
    assertOnType(zapNodeType, reason);
    if (zapNodeType == COMMUNICATION_TO_ACTIVE_ERROR && nodeID.equals(stateManager.getActiveNodeID())) {
      consoleLogger.warn("Requesting active node to quit : " + getFormatedError(nodeID, zapNodeType));
      return true;
    }
    if (stateManager.isActiveCoordinator()) {
      if (consistencyMgr.requestTransition(stateManager.getCurrentMode(), nodeID, ConsistencyManager.Transition.ZAP_NODE)) {
        consoleLogger.warn("Requesting node to quit : " + getFormatedError(nodeID, zapNodeType));
        return true;
      } else {
        logger.warn("Not allowing to Zap " + nodeID + " since server does not have consistency votes");
        return false;
      }
    } else {
      logger.warn("Not allowing to Zap " + nodeID + " since not in " + StateManager.ACTIVE_COORDINATOR);
      return false;
    }
  }

  @Override
  public long[] getCurrentNodeWeights() {
    return factory.generateWeightSequence();
  }

  private String getFormatedError(NodeID nodeID, int zapNodeType) {
    return getFormatedError(nodeID, zapNodeType, null);
  }

  private String getFormatedError(NodeID nodeID, int zapNodeType, String reason) {
    return "NodeID : " + nodeID + " Error Type : " + getErrorTypeString(zapNodeType)
           + (reason != null ? (" Details : " + reason) : "");
  }

  private String getErrorTypeString(int type) {
    switch (type) {
      case COMMUNICATION_ERROR:
        return "COMMUNICATION ERROR";
      case COMMUNICATION_TO_ACTIVE_ERROR:
        return "COMMUNICATION TO ACTIVE SERVER ERROR";
      case PROGRAM_ERROR:
        return "PROGRAM ERROR";
      case NODE_JOINED_WITH_DIRTY_DB:
        return "Newly Joined Node Contains dirty database.";
      case PARTIALLY_SYNCED_PASSIVE_JOINED:
        return "Newly joined node in uninitialized state is already partially synced - this is not supported.";
      case SPLIT_BRAIN:
        return "Two or more Active servers detected in the cluster";
      case INSUFFICIENT_RESOURCES:
        return "L2 has insufficient resources to join the cluster. Check dataStorage and offheap settings and try again.";
      default:
        throw new AssertionError("Unknown type : " + type);
    }
  }

  private void assertOnType(int type, String reason) {
    switch (type) {
      case COMMUNICATION_ERROR:
      case COMMUNICATION_TO_ACTIVE_ERROR:
      case PROGRAM_ERROR:
      case NODE_JOINED_WITH_DIRTY_DB:
      case PARTIALLY_SYNCED_PASSIVE_JOINED:
      case SPLIT_BRAIN:
      case INSUFFICIENT_RESOURCES:
        break;
      default:
        throw new AssertionError("Unknown type : " + type + " reason : " + reason);
    }
  }

  @Override
  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    assertOnType(zapNodeType, reason);
    if (stateManager.isActiveCoordinator()) {
      logger.warn(StateManager.ACTIVE_COORDINATOR + " received Zap Node request from another "
                  + StateManager.ACTIVE_COORDINATOR + "\n" + getFormatedError(nodeID, zapNodeType, reason));
      handleSplitBrainScenario(nodeID, zapNodeType, reason, weights);
    } else {
      NodeID activeNode = stateManager.getActiveNodeID();
      if (activeNode.isNull() || activeNode.equals(nodeID)) {
        String message = "Terminating due to Zap request from " + getFormatedError(nodeID, zapNodeType, reason);
        logger.error(message);
        if (zapNodeType == NODE_JOINED_WITH_DIRTY_DB) {
          clusterStatePersistor.setDBClean(false);
          stateManager.moveToStopState();
          throw new ZapDirtyDbServerNodeException(message);
        } else if (zapNodeType == INSUFFICIENT_RESOURCES) {
          throw new TCShutdownServerException(message);
        } else {
          throw new ZapServerNodeException(message);
        }
      } else {
        logger.warn("Ignoring Zap Node since it did not come from " + StateManager.ACTIVE_COORDINATOR + " "
                    + activeNode + " but from " + getFormatedError(nodeID, zapNodeType, reason));
      }
    }
  }

  private void handleSplitBrainScenario(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    long myWeights[] = factory.generateWeightSequence();
    logger.warn("A Terracotta server tried to join the mirror group as a second ACTIVE : My weights = "
                + toString(myWeights) + " Other servers weights = " + toString(weights));
    for (ZapEventListener listener : this.listeners) {
      listener.fireSplitBrainEvent(this.groupManager.getLocalNodeID(), nodeID);
    }
    Enrollment mine = new Enrollment(groupManager.getLocalNodeID(), false, myWeights);
    Enrollment hisOrHers = new Enrollment(nodeID, false, weights);
    if (hisOrHers.wins(mine)) {
      // The other node has more connected clients, so back off
      logger.warn(nodeID + " wins : Backing off : Exiting !!!");
      String message = "Found that " + nodeID
                       + " is active and has more clients connected to it than this server. Exiting ... !!";
      for (ZapEventListener listener : this.listeners) {
        listener.fireBackOffEvent(nodeID);
      }
      throw new ZapServerNodeException(message);
    } else {
      logger.warn("Not quiting since the other servers weight = " + toString(weights)
                  + " is not greater than my weight = " + toString(myWeights));
      consoleLogger.warn("Ignoring Quit request from " + nodeID
                         + " since remote servers weight is not greater than local weight");
    }
  }

  private String toString(long[] l) {
    if (l == null) return "null";
    if (l.length == 0) return "empty";
    StringBuffer sb = new StringBuffer();
    for (long element : l) {
      sb.append(String.valueOf(element)).append(",");
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }

  public static String getErrorString(Throwable t) {
    StringWriter sw = new StringWriter();
    sw.write("\nException : ");
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.flush();
    sw.write("\n");
    return sw.toString();
  }

  @Override
  public void addZapEventListener(ZapEventListener listener) {
    this.listeners.add(listener);
  }

}
