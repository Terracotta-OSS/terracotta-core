/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core.event;

/**
 * Interface for receiving connection events. These event handler will more than likely be called on the main
 * communications thread. It is unwise to do "heavy" operations in these handlers. In particular one should not be
 * calling back into the comms layer (for example to create a new connection or something)
 * <p>
 * Exceptions that occur in event handlers will caught and logged. An exception in any one event listener will not
 * prevent other listeners from also receiving the event
 * 
 * @author teck
 */
public interface TCConnectionEventListener {

  /**
   * Connect event will be called once per connection connect
   */
  public void connectEvent(TCConnectionEvent event);

  /**
   * Connection closed event will only be called once and will only be called if connection was actually ever connected.
   * If a non-connected channel is closed you will not receice this event
   */
  public void closeEvent(TCConnectionEvent event);

  /**
   * Error event will be called any time an exception occurs as part of doing an IO operation (read, write, connect,
   * accept) on a given connection
   */
  public void errorEvent(TCConnectionErrorEvent errorEvent);

  /**
   * EOF event is thrown when a clean EOF is read from the network. If a network exception prematurely causes a
   * connection to end, you won't see an EOF event
   */
  public void endOfFileEvent(TCConnectionEvent event);
}