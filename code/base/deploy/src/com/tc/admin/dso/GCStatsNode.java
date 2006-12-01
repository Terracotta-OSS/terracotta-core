/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.AdminClient;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.ComponentNode;

public class GCStatsNode extends ComponentNode {
  public GCStatsNode(ConnectionContext cc) {
    super();

    setLabel(AdminClient.getContext().getMessage("dso.gcstats"));
    setComponent(new GCStatsPanel(cc));
  }

  public void tearDown() {
    ((GCStatsPanel)getComponent()).tearDown();
    super.tearDown();
  }
}
