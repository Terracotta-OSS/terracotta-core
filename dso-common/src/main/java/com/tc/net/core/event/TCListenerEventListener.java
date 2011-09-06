/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core.event;

/**
 * Interface for those interested in listening to events of TCListeners
 * 
 * @author teck
 */
public interface TCListenerEventListener {

  /**
   * Called once (and only once) after a listener is actually closed
   * 
   * @param event
   */
  public void closeEvent(TCListenerEvent event);
}