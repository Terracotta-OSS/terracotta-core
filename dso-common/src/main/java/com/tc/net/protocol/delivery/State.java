/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

/**
 * 
 */
public interface State {
  public void enter();
 
  public void execute(OOOProtocolMessage protocolMessage);
}
