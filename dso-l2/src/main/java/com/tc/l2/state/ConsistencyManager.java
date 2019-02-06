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
package com.tc.l2.state;

import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import org.slf4j.Logger;
import org.terracotta.config.TcConfig;

import java.util.Collection;

public interface ConsistencyManager {
  
  public enum Transition {
    MOVE_TO_ACTIVE,
    CONNECT_TO_ACTIVE,
    ADD_CLIENT {
      @Override
      boolean isStateTransition() {
        return false;
      }
    },
    REMOVE_PASSIVE,
    ADD_PASSIVE,
    ZAP_NODE {
      @Override
      boolean isStateTransition() {
        return false;
      }
    };
    
    boolean isStateTransition() {
      return true;
    }
  }

  boolean requestTransition(ServerMode mode, NodeID sourceNode, Transition newMode) throws IllegalStateException;

  boolean lastTransitionSuspended();

  void allowLastTransition();

  Collection<Transition> requestedActions();
  
  long getCurrentTerm();
  
  static int parseVoteCount(TcConfig config) {
    Logger consoleLogger = TCLogging.getConsoleLogger();
    if (config.getServers().getServer().size() == 1) {
      return -1;
    }
    if (config.getFailoverPriority() == null) {
      consoleLogger.error("*****************************************************************************");
      consoleLogger.error("* Failover priority is not specified and it is a mandatory configuration. *");
      consoleLogger.error("*****************************************************************************");
      System.exit(-1);
    }
    if (config.getFailoverPriority().getAvailability() != null) {
      consoleLogger.warn("Running the server in AVAILABILITY mode with the risk of split brain scenarios.");
      return -1;
    }
    try {
      consoleLogger.warn("Running the server in CONSISTENCY mode with compromised availability of the cluster.");
      int voters = config.getFailoverPriority().getConsistency().getVoter().getCount();
      if (voters < 0) {
        throw new IllegalArgumentException("the voter count cannot be negative");
      }
      return voters;
    } catch (NullPointerException npe) {
      // default to consistency with no outside voters
      return 0;
    }
  }
}
