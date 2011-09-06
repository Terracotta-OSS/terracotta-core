/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
    UNKNOWN("unknown") {

      @Override
      public boolean isNodeJoined() {
        return false;
      }

      @Override
      public boolean areOperationsEnabled() {
        return false;
      }

      @Override
      public boolean isNodeLeft() {
        return false;
      }
    },
    NODE_JOINED("node-joined") {

      @Override
      public boolean isNodeJoined() {
        return true;
      }

      @Override
      public boolean areOperationsEnabled() {
        return false;
      }

      @Override
      public boolean isNodeLeft() {
        return false;
      }
    },
    OPERATIONS_ENABLED("operations-enabled") {

      @Override
      public boolean isNodeJoined() {
        return true;
      }

      @Override
      public boolean areOperationsEnabled() {
        return true;
      }

      @Override
      public boolean isNodeLeft() {
        return false;
      }
    },
    OPERATIONS_DISABLED("operations-disabled") {

      @Override
      public boolean isNodeJoined() {
        return true;
      }

      @Override
      public boolean areOperationsEnabled() {
        return false;
      }

      @Override
      public boolean isNodeLeft() {
        return false;
      }
    },
    NODE_LEFT("node-left") {

      @Override
      public boolean isNodeJoined() {
        return true;
      }

      @Override
      public boolean areOperationsEnabled() {
        return false;
      }

      @Override
      public boolean isNodeLeft() {
        return true;
      }
    };

    private final String name;

    ClusterNodeStateType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return "ClusterNodeState[name=" + name + "]";
    }

    public abstract boolean isNodeJoined();

    public abstract boolean areOperationsEnabled();

    public abstract boolean isNodeLeft();
  }
}