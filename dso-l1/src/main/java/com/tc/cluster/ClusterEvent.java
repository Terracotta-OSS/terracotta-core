/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.cluster;

import com.tcclient.cluster.Node;

/**
 * Indicates that the state of a node in the cluster has changed.
 * <p>
 * Instances of the {@code ClusterEvent} are provided as arguments of the {@link ClusterListener} methods.
 *
 * @since 3.0.0
 */
public interface ClusterEvent {
  /**
   * Retrieves the node that this event talks about.
   *
   * @return the instance of the related node
   */
  public Node getNode();
}