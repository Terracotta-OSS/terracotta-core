/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTree;

public class NavTree extends XTree {
  public NavTree() {
    super();
  }

  public XRootNode getRootNode() {
    return (XRootNode) getModel().getRoot();
  }
}
