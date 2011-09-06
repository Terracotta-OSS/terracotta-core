/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

/**
 * A generic network message sink
 * 
 * @author teck
 */
public interface NetworkMessageSink {
  public void putMessage(TCNetworkMessage message);
}