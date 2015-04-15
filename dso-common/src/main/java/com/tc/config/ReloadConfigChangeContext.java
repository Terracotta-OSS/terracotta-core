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
package com.tc.config;

import com.tc.net.groups.Node;

import java.util.ArrayList;
import java.util.List;

public class ReloadConfigChangeContext {
  private final List<Node> nodesAdded = new ArrayList<Node>();
  private final List<Node> nodesRemoved = new ArrayList<Node>();
  
  public void update(ReloadConfigChangeContext context) {
    nodesAdded.addAll(context.nodesAdded);
    nodesRemoved.addAll(context.nodesRemoved);
  }

  public List<Node> getNodesAdded() {
    return nodesAdded;
  }

  public List<Node> getNodesRemoved() {
    return nodesRemoved;
  }

  @Override
  public String toString() {
    return "ReloadConfigChangeContext [nodesAdded=" + nodesAdded + ", nodesRemoved=" + nodesRemoved + "]";
  }
}
