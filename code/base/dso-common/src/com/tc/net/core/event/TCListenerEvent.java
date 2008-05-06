/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core.event;

import com.tc.net.core.TCListener;

/**
 * Event emitted from a TCListener
 * 
 * @author teck
 */
public class TCListenerEvent {
  private final TCListener listener;

  public TCListenerEvent(final TCListener listener) {
    this.listener = listener;
  }

  public final TCListener getSource() {
    return listener;
  }
}