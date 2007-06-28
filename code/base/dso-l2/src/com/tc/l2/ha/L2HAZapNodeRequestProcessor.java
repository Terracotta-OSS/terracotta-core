/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.state.Enrollment;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.ZapNodeRequestProcessor;
import com.tc.util.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;

public class L2HAZapNodeRequestProcessor implements ZapNodeRequestProcessor {
  private static final TCLogger        logger                    = TCLogging
                                                                     .getLogger(L2HAZapNodeRequestProcessor.class);

  public static final int              COMMUNICATION_ERROR       = 0x01;
  public static final int              PROGRAM_ERROR             = 0x02;
  public static final int              NODE_JOINED_WITH_DIRTY_DB = 0x03;
  public static final int              SPLIT_BRAIN               = 0xff;

  private final TCLogger               consoleLogger;
  private final StateManager           stateManager;
  private final WeightGeneratorFactory factory;

  private final GroupManager           groupManager;

  public L2HAZapNodeRequestProcessor(TCLogger consoleLogger, StateManager stateManager, GroupManager groupManager,
                                     WeightGeneratorFactory factory) {
    this.consoleLogger = consoleLogger;
    this.stateManager = stateManager;
    this.groupManager = groupManager;
    this.factory = factory;
  }

  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason) {
    assertOnType(zapNodeType, reason);
    if (stateManager.isActiveCoordinator()) {
      consoleLogger.warn("Requesting node to quit due to the following error\n"
                         + getFormatedError(nodeID, zapNodeType, reason));
      return true;
    } else {
      logger.warn("Not allowing to Zap " + nodeID + " since not in " + StateManager.ACTIVE_COORDINATOR);
      return false;
    }
  }

  public long[] getCurrentNodeWeights() {
    Assert.assertTrue(stateManager.isActiveCoordinator());
    return factory.generateWeightSequence();
  }

  private String getFormatedError(NodeID nodeID, int zapNodeType, String reason) {
    return "NodeID : " + nodeID + " Error Type : " + getErrorTypeString(zapNodeType) + " Details : " + reason;
  }

  private String getErrorTypeString(int type) {
    switch (type) {
      case COMMUNICATION_ERROR:
        return "COMMUNICATION ERROR";
      case PROGRAM_ERROR:
        return "PROGRAM ERROR";
      case NODE_JOINED_WITH_DIRTY_DB:
        return "Newly Joined Node Contains dirty database. (Please clean up DB and restart node)";
      case SPLIT_BRAIN:
        return "SPLIT BRAIN DEDUCTED";
      default:
        throw new AssertionError("Unknown type : " + type);
    }
  }

  private void assertOnType(int type, String reason) {
    switch (type) {
      case COMMUNICATION_ERROR:
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
        logger.warn(message);
        consoleLogger.warn(message);
        System.exit(zapNodeType);
      } else {
        logger.warn("Ignoring Zap Node since it did not come from " + StateManager.ACTIVE_COORDINATOR + " "
                    + activeNode + " but from " + getFormatedError(nodeID, zapNodeType, reason));
      }
    }
  }

  private void handleSplitBrainScenario(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    long myWeights[] = factory.generateWeightSequence();
    logger.warn("Possible Split Brain scenario : My weights = " + toString(myWeights) + " Other servers weights = "
                + toString(weights));
    Enrollment mine;
    try {
      mine = new Enrollment(groupManager.getLocalNodeID(), false, myWeights);
    } catch (GroupException e) {
      throw new AssertionError(e);
    }
    Enrollment hisOrHers = new Enrollment(nodeID, false, weights);
    if (hisOrHers.wins(mine)) {
      // The other node has more connected clients, so back off
      logger.warn(nodeID + " wins : Backing off : Exiting !!!");
      consoleLogger.warn("Found that " + nodeID
                         + " is active and has more clients connected to it than this server. Exiting ... !!");
      System.exit(zapNodeType);
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
    sw.write(" : Exception : \n");
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.flush();
    sw.write("\n");
    return sw.toString();
  }

}
