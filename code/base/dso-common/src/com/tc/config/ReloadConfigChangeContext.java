/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.groups.Node;

import java.util.ArrayList;
import java.util.List;

public class ReloadConfigChangeContext {
  private final List<Node> nodesAdded = new ArrayList<Node>();
  private final List<Node> nodesRemoved = new ArrayList<Node>();
  
  public void update(ReloadConfigChangeContext context) {
    nodesAdded.addAll(context.nodesAdded);
    nodesRemoved.addAll(context.nodesRemoved);
  }

  public List<Node> getNodesAdded() {
    return nodesAdded;
  }

  public List<Node> getNodesRemoved() {
    return nodesRemoved;
  }

  @Override
  public String toString() {
    return "ReloadConfigChangeContext [nodesAdded=" + nodesAdded + ", nodesRemoved=" + nodesRemoved + "]";
  }
}
