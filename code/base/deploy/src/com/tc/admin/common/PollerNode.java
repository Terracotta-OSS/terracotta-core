/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
