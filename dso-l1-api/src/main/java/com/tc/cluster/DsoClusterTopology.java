/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.cluster;

import com.tcclient.cluster.DsoNode;

import java.util.Collection;

/**
 * Provides access to the topology of the cluster, viewed from the current node.
 * <p>
 * This only takes terracotta client nodes into account, TSA server nodes are not included in this topology view.
 * 
 * @since 3.0.0
 */
public interface DsoClusterTopology {
  /**
   * Returns a collection that contains a snapshot of the nodes that are part of the cluster at the time of the method
   * call.
   *
   * @return the snapshot of the nodes in the cluster
   */
  public Collection<DsoNode> getNodes();
}