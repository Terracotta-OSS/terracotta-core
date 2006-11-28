package com.tc.net.core.event;

import com.tc.net.core.TCConnection;

/**
 * A generic connection event. Not very interesting
 * 
 * @author teck
 */
public interface TCConnectionEvent {

  /**
   * The source of this event (ie. the connection that generated it)
   */
  public TCConnection getSource();
}