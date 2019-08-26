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

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.objectserver.impl.Topology;
import com.tc.objectserver.impl.TopologyManager;
import com.tc.text.PrettyPrintable;
import org.terracotta.config.FailoverBehavior;
import org.terracotta.config.ServerConfiguration;
import org.slf4j.Logger;

import java.util.Collection;

public interface ConsistencyManager extends PrettyPrintable {
  
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

  default boolean requestTransition(ServerMode mode, NodeID sourceNode, Transition newMode) throws IllegalStateException {
    return requestTransition(mode, sourceNode, TopologyManager.get().getTopology(), newMode);
  }

  boolean requestTransition(ServerMode mode, NodeID sourceNode, Topology topology, Transition newMode) throws IllegalStateException;

  boolean lastTransitionSuspended();

  void allowLastTransition();

  Collection<Transition> requestedActions();
  
  long getCurrentTerm();
  
  void setCurrentTerm(long term);
  
  Enrollment createVerificationEnrollment(NodeID lastActive, WeightGeneratorFactory weightFactory);
  
  static int parseVoteCount(FailoverBehavior priority, Collection<ServerConfiguration> config) {
    Logger consoleLogger = TCLogging.getConsoleLogger();
    if (config.size() == 1) {
      return -1;
    }
    if (priority == null) {
      consoleLogger.error("*****************************************************************************");
      consoleLogger.error("* Failover priority is not specified and it is a mandatory configuration. *");
      consoleLogger.error("*****************************************************************************");
      System.exit(-1);
    }
    if (priority.isAvailability()) {
      consoleLogger.info("Running the server in AVAILABILITY mode with the risk of split brain scenarios.");
      return -1;
    }
    try {
      consoleLogger.info("Running the server in CONSISTENCY mode with compromised availability of the cluster.");
      int voters = priority.getExternalVoters();
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
