/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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