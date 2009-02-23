/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.cluster;

import com.tc.net.NodeID;

public interface DsoNodeInternal extends DsoNode {

  public NodeID getNodeID();

  public void setMetaData(DsoNodeMetaData metaData);

}