/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
