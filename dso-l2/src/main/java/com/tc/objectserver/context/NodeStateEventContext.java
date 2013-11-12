/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
