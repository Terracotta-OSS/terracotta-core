/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core.event;

import com.tc.net.core.TCConnection;

/**
 * A generic connection event. Not very interesting
 * 
 * @author teck
 */
public class TCConnectionEvent {

  private final TCConnection connection;

  public TCConnectionEvent(final TCConnection connection) {
    this.connection = connection;
  }

  /**
   * The source of this event (ie. the connection that generated it)
   */
  public final TCConnection getSource() {
    return connection;
  }
  
  public String toString() {
    return connection.toString();
  }
}
