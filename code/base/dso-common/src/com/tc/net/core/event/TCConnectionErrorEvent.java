/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core.event;

import com.tc.net.protocol.TCNetworkMessage;

/**
 * A special flavor of TCConnectionEvent indicating an error on a specific connection
 * 
 * @author teck
 */
public interface TCConnectionErrorEvent extends TCConnectionEvent {

  /**
   * The exception thrown by an IO operation on this connection
   */
  public Exception getException();

  /**
   * If relevant, the message instance that was being used for the IO operation. Can be null
   */
  public TCNetworkMessage getMessageContext();
}