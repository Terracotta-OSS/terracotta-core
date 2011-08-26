/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;

import java.util.List;

public interface TraversalAction {

  public void visit(List objects, GroupID gid);
}