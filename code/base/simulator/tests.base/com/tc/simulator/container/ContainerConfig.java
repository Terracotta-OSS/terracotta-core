/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.container;

public interface ContainerConfig {

  /**
   * The number of application instances to create.
   */
  public int getApplicationInstanceCount();

  /**
   * The maximum time a container should wait for other containers to join
   */
  public long getContainerStartTimeout();

  /**
   * The maximum time the container should wait for its local applications to join.
   */
  public long getApplicationStartTimeout();

  /**
   * The maximum time the container should wait for its local applications to complete.
   */
  public long getApplicationExecutionTimeout();

  /**
   * Whether or not this container is the master.
   */
  public boolean isMaster();

}