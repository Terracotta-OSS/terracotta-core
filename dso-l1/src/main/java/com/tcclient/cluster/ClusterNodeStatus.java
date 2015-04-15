/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tcclient.cluster;

public class ClusterNodeStatus {

  private volatile ClusterNodeStateType state = ClusterNodeStateType.UNKNOWN;

  public void nodeJoined() {
    state = ClusterNodeStateType.NODE_JOINED;
  }

  public void operationsEnabled() {
    state = ClusterNodeStateType.OPERATIONS_ENABLED;
  }

  public void operationsDisabled() {
    state = ClusterNodeStateType.OPERATIONS_DISABLED;
  }

  public void nodeLeft() {
    state = ClusterNodeStateType.NODE_LEFT;
  }

  public ClusterNodeStateType getState() {
    return state;
  }

  public static enum ClusterNodeStateType {
    // unknown
    UNKNOWN("unknown", false, false, false),
    // joined
    NODE_JOINED("node-joined", true, false, false),
    // ops enabled
    OPERATIONS_ENABLED("operations-enabled", true, true, false),
    // ops disabled
    OPERATIONS_DISABLED("operations-disabled", true, false, false),
    // node left
    NODE_LEFT("node-left", true, false, true);

    private final String  name;
    private final boolean nodeJoined;
    private final boolean opsEnabled;
    private final boolean nodeLeft;

    @Override
    public String toString() {
      return "ClusterNodeState[name=" + name + "]";
    }

    private ClusterNodeStateType(String name, boolean nodeJoined, boolean opsEnabled, boolean nodeLeft) {
      this.name = name;
      this.nodeJoined = nodeJoined;
      this.opsEnabled = opsEnabled;
      this.nodeLeft = nodeLeft;
    }

    public boolean isNodeJoined() {
      return nodeJoined;
    }

    public boolean areOperationsEnabled() {
      return opsEnabled;
    }

    public boolean isNodeLeft() {
      return nodeLeft;
    }
  }
}