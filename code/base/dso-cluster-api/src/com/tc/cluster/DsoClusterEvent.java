/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tcclient.cluster.DsoNode;

/**
 * Indicates that the state of a node in the DSO cluster has changed.
 * <p>
 * Instances of the {@code DsoClusterEvent} are provided as arguments of the {@link DsoClusterListener} methods.
 *
 * @since 3.0.0
 */
public interface DsoClusterEvent {
  /**
   * Retrieves the node that this event talks about.
   *
   * @return the instance of the related node
   */
  public DsoNode getNode();
}