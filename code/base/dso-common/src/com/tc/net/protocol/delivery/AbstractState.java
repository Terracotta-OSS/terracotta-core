/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

/**
 * 
 */
public class AbstractState implements State {

  public void enter() {
    // override me if you want
  }

  public void execute(OOOProtocolMessage protocolMessage) {
    // override me if you want
  }
}
