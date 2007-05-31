/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.ZapNodeRequestProcessor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class L2HAZapNodeRequestProcessor implements ZapNodeRequestProcessor {
  private static final TCLogger logger                    = TCLogging.getLogger(L2HAZapNodeRequestProcessor.class);

  public static final int       COMMUNICATION_ERROR       = 0x01;
  public static final int       PROGRAM_ERROR             = 0x02;
  public static final int       NODE_JOINED_WITH_DIRTY_DB = 0x03;

  private final TCLogger        consoleLogger;
  private final StateManager    stateManager;

  public L2HAZapNodeRequestProcessor(TCLogger consoleLogger, StateManager stateManager) {
    this.consoleLogger = consoleLogger;
    this.stateManager = stateManager;
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
      default:
        throw new AssertionError("Unknown type : " + type);
    }
  }

  private void assertOnType(int type, String reason) {
    switch (type) {
      case COMMUNICATION_ERROR:
      case PROGRAM_ERROR:
      case NODE_JOINED_WITH_DIRTY_DB:
        break;
      default:
        throw new AssertionError("Unknown type : " + type + " reason : " + reason);
    }
  }

  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason) {
    assertOnType(zapNodeType, reason);
    if (stateManager.isActiveCoordinator()) {
      logger.warn("Ignoring Zap request since in " + StateManager.ACTIVE_COORDINATOR + "\n"
                  + getFormatedError(nodeID, zapNodeType, reason));
      // TODO:: Handle split brain
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
