/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
