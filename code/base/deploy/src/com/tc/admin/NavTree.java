/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.XTree;
import com.tc.admin.common.XRootNode;

public class NavTree extends XTree {
  public NavTree() {
    super();
  }

  public XRootNode getRootNode() {
    return (XRootNode)getModel().getRoot();
  }
}
