/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.container;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.simulator.container.ContainerState;
import com.tc.simulator.listener.OutputListener;
import com.tc.simulator.listener.StatsListener;
import com.tcsimulator.listener.OutputListenerObject;
import com.tcsimulator.listener.StatsListenerObject;

import java.util.Properties;

public class ContainerStateObject implements ContainerState {

  private String            containerId;
  private final LinkedQueue statsOutputQueue;

  public ContainerStateObject(String containerId, LinkedQueue statsOutputQueue) {
    this.containerId = containerId;
    this.statsOutputQueue = statsOutputQueue;
  }

  public String getContainerId() {
    return containerId;
  }

  public StatsListener newStatsListener(Properties properties) {
    return new StatsListenerObject(properties, statsOutputQueue);
  }

  public OutputListener newOutputListener() {
    return new OutputListenerObject();
  }

}