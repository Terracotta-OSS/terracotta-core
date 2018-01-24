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

import com.tc.net.NodeID;
import org.terracotta.config.TcConfig;

public interface ConsistencyManager {
  
  public enum Transition {
    MOVE_TO_ACTIVE,
    CONNECT_TO_ACTIVE,
    ADD_CLIENT,
    REMOVE_PASSIVE,
    ADD_PASSIVE
  }

  boolean requestTransition(ServerMode mode, NodeID sourceNode, Transition newMode) throws IllegalStateException;
  
  static int parseVoteCount(TcConfig config) {
    try {
      if (config.getFailoverPriority().getAvailability() != null) {
        return -1;
      }
    } catch (NullPointerException npe) {
      // default to consistency
      return 0;
    }
    try {
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
