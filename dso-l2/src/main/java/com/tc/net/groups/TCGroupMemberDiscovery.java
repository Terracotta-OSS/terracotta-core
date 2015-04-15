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
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;

public interface TCGroupMemberDiscovery extends GroupEventsListener {

  public void start() throws GroupException;
  
  public void addNode(Node node);

  public void stop(long timeout);

  public void setupNodes(Node local, Node[] nodes);
  
  public Node getLocalNode();
  
  public void discoveryHandler(EventContext context);
  
  public boolean isValidClusterNode(NodeID nodeID);

  public void removeNode(Node node);

  public boolean isServerConnected(String nodeName);
  
}
