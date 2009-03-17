/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class ClusterEventsTestListener implements DsoClusterListener {

  @InjectedDsoInstance
  private DsoCluster  cluster;

  public List<String> events = new ArrayList<String>();

  public ClusterEventsTestListener() {
    System.out.println(">>>>>> ClusterEventsTestListener.cluster : "+cluster);
  }

  public List<String> getOccurredEvents() {
    return events;
  }

  public synchronized void nodeJoined(final DsoClusterEvent event) {
    try {
      System.out.println(">>>>>> " + cluster.getCurrentNode() + " - nodeJoined : " + event.getNode().getId());
      events.add(event.getNode().getId() + " JOINED");
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
    events.add(event.getNode().getId() + " LEFT");
    if (cluster.getCurrentNode().equals(event.getNode())) {
      Assert.assertFalse(cluster.isNodeJoined());
    }
  }

  public synchronized void operationsEnabled(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsEnabled : " + event.getNode().getId());
    events.add(event.getNode().getId() + " ENABLED");
    Assert.assertTrue(cluster.areOperationsEnabled());
  }

  public synchronized void operationsDisabled(final DsoClusterEvent event) {
    System.out.println(">>>>>> " + cluster.getCurrentNode() + " - operationsDisabled : " + event.getNode().getId());
    events.add(event.getNode().getId() + " DISABLED");
    Assert.assertFalse(cluster.areOperationsEnabled());
  }
}