/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

/**
 * 
 */
public class AbstractState implements State {

  private final String name;

  public AbstractState(String name) {
    this.name = name;
  }

  public void enter() {
    // override me if you want
  }

  public void execute(OOOProtocolMessage protocolMessage) {
    // override me if you want
  }

  public String toString() {
    return name;
  }
}
