/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

/**
 * Describes a node in the Terracotta DSO cluster.
 *
 * @since 3.0.0
 */
public interface DsoNode {

  /**
   * Returns the unique string identifier that corresponds to the node.
   * <p>
   * This identifier is unique for the life-time of the cluster. However, if the cluster is completely shut down and
   * brought back up again, these identifiers might be recycled.
   *
   * @return string identifier for the node
   */
  public String getId();

  /**
   * Returns the IP address of the node.
   * <p>
   * This operation talks back to the server array the first time it's called. The result is cached for further use.
   *
   * @return the IP address of the node
   */
  public String getIp();

  /**
   * Returns the host name of the node.
   * <p>
   * This operation talks back to the server array the first time it's called. The result is cached for further use.
   *
   * @return the host name of the node
   */
  public String getHostname();
}