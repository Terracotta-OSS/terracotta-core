/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

import java.io.Serializable;

/**
 * Describes a node in the Terracotta cluster.
 *
 * @since 3.0.0
 */
public interface Node extends Serializable {

  /**
   * Returns the unique string identifier that corresponds to the node.
   * <p>
   * This identifier is unique for the life-time of the cluster. However, if the cluster is completely shut down and
   * brought back up again, these identifiers might be recycled.
   *
   * @return string identifier for the node
   */
  public String getId();

}