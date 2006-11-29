/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol;

/**
 * @author teck
 */
public class NullSink implements GenericNetworkMessageSink {

  public void putMessage(GenericNetworkMessage msg) {
    return;
  }

}