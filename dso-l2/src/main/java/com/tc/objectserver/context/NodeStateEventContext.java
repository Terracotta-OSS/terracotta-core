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
package com.tc.objectserver.context;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.license.ProductID;
import com.tc.net.NodeID;

public class NodeStateEventContext implements MultiThreadedEventContext {
  public static final int CREATE = 0;
  public static final int REMOVE = 1;

  private final int       type;
  private final NodeID    nodeID;
  private final ProductID productId;

  public NodeStateEventContext(int type, NodeID nodeID, final ProductID productId) {
    this.type = type;
    this.nodeID = nodeID;
    this.productId = productId;
    if ((type != CREATE) && (type != REMOVE)) { throw new IllegalArgumentException("invalid type: " + type); }
  }

  public int getType() {
    return type;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  @Override
  public Object getKey() {
    return nodeID;
  }

  public ProductID getProductId() {
    return productId;
  }
}
