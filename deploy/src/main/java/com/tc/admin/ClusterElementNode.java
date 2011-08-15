/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ComponentNode;
import com.tc.admin.model.IClusterModelElement;

public class ClusterElementNode extends ComponentNode {
  private final IClusterModelElement clusterElement;

  protected ClusterElementNode(IClusterModelElement clusterElement) {
    super();
    setUserObject(this.clusterElement = clusterElement);
    setName(clusterElement.toString());
  }

  public IClusterModelElement getClusterElement() {
    return clusterElement;
  }
}
