/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol;

/**
 * @author teck
 */
public interface GenericNetworkMessageSink {
  void putMessage(GenericNetworkMessage msg);
}