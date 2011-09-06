/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.exception.ZapDirtyDbServerNodeException;
import com.tc.exception.ZapServerNodeException;
import com.tc.l2.state.Enrollment;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.ZapEventListener;
import com.tc.net.groups.ZapNodeRequestProcessor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class L2HAZapNodeRequestProcessor implements ZapNodeRequestProcessor {
  private static final TCLogger               logger                        = TCLogging
                                                                                .getLogger(L2HAZapNodeRequestProcessor.class);

  public static final int                     COMMUNICATION_ERROR           = 0x01;
  public static final int                     PROGRAM_ERROR                 = 0x02;
  public static final int                     NODE_JOINED_WITH_DIRTY_DB     = 0x03;
  public static final int                     COMMUNICATION_TO_ACTIVE_ERROR = 0x04;
  public static final int                     SPLIT_BRAIN                   = 0xff;

  private final TCLogger                      consoleLogger;
  private final StateManager                  stateManager;
  private final WeightGeneratorFactory        factory;

  private final GroupManager                  groupManager;
  private final List<ZapEventListener>        listeners                     = new CopyOnWriteArrayList<ZapEventListener>();

  public L2HAZapNodeRequestProcessor(TCLogger consoleLogger, StateManager stateManager, GroupManager groupManager,
                                     WeightGeneratorFactory factory) {
    this.consoleLogger = consoleLogger;
    this.stateManager = stateManager;
    this.groupManager = groupManager;
    this.factory = factory;
  }

  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason) {
    assertOnType(zapNodeType, reason);
    if (stateManager.isActiveCoordinator()
        || (zapNodeType == COMMUNICATION_TO_ACTIVE_ERROR && nodeID.equals(stateManager.getActiveNodeID()))) {
      consoleLogger.warn("Requesting node to quit : " + getFormatedError(nodeID, zapNodeType));
      return true;
    } else {
      logger.warn("Not allowing to Zap " + nodeID + " since not in " + StateManager.ACTIVE_COORDINATOR);
      return false;
    }
  }

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
        return "Newly Joined Node Contains dirty database. (Please clean up DB and restart node)";
      case SPLIT_BRAIN:
        return "Two or more Active servers detected in the cluster";
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
      case SPLIT_BRAIN:
        break;
      default:
        throw new AssertionError("Unknown type : " + type + " reason : " + reason);
    }
  }

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
          throw new ZapDirtyDbServerNodeException(message);
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
    for (int i = 0; i < l.length; i++) {
      sb.append(String.valueOf(l[i])).append(",");
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

  public void addZapEventListener(ZapEventListener listener) {
    this.listeners.add(listener);
  }

}
