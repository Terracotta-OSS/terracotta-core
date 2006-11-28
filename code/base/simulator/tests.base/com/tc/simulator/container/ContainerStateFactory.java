/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.container;

public interface ContainerStateFactory {
  public ContainerState newContainerState(String containerId);
}