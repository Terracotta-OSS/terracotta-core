/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core.event;

import com.tc.net.core.TCListener;

/**
 * Event emitted from a TCListener
 * 
 * @author teck
 */
public interface TCListenerEvent {
  public TCListener getSource();
}