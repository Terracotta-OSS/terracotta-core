/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.container;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.simulator.container.ContainerState;
import com.tc.simulator.container.ContainerStateFactory;

public class ContainerStateFactoryObject implements ContainerStateFactory {

  private final LinkedQueue statsOutputQueue;

  public ContainerStateFactoryObject(LinkedQueue statsOutputQueue) {
    this.statsOutputQueue = statsOutputQueue;
  }
  
  public ContainerState newContainerState(String containerId) {
    return new ContainerStateObject(containerId, statsOutputQueue);
  }
}