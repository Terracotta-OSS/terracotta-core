/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.Component;

public class PollerNode extends ComponentNode {
  public PollerNode(String label, Component poller) {
    super(label, poller);
  }

  public void tearDown() {
    Poller poller = (Poller)getComponent();
    
    if(poller != null) {
      poller.stop();
    }

    super.tearDown();
  }
}
