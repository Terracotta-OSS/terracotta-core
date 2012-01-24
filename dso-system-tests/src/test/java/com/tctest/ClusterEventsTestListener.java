/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.util.Assert;
import com.tcclient.cluster.DsoNode;
import com.tctest.builtin.ArrayList;

import java.util.List;

public class ClusterEventsTestListener implements DsoClusterListener {

  @InjectedDsoInstance
  private DsoCluster          cluster;

  private final List<String>  eventDescriptions = new ArrayList<String>();
  private final List<DsoNode> eventNodes        = new ArrayList<DsoNode>();

  public List<String> getOccurredEvents() {
    return eventDescriptions;
  }

  public List<DsoNode> getEventNodes() {
    return eventNodes;
  }

  public synchronized void nodeJoined(final DsoClusterEvent event) {
    try {
      System.out.println(">>>>>> " + cluster.getCurrentNode() + " - nodeJoined : " + event.getNode().getId());
      eventDescriptions.add(event.getNode().getId() + " JOINED");
      eventNodes.add(event.getNode());
      if (cluster.getCurrentNode().equals(event.getNode())) {
        Assert.assertTrue(cluster.isNodeJoined());
      }
    } catch (NullPointerException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public synchronized void nodeLeft(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - nodeLeft : " + event.getNode().getId());
    eventDescriptions.add(event.getNode().getId() + " LEFT");
    eventNodes.add(event.getNode());
    if (cluster.getCurrentNode().equals(event.getNode())) {
      Assert.assertFalse(cluster.isNodeJoined());
    }
  }

  public synchronized void operationsEnabled(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsEnabled : " + event.getNode().getId());
    eventDescriptions.add(event.getNode().getId() + " ENABLED");
    eventNodes.add(event.getNode());
    Assert.assertTrue(cluster.areOperationsEnabled());
  }

  public synchronized void operationsDisabled(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsDisabled : " + event.getNode().getId());
    eventDescriptions.add(event.getNode().getId() + " DISABLED");
    eventNodes.add(event.getNode());
    Assert.assertFalse(cluster.areOperationsEnabled());
  }
}
