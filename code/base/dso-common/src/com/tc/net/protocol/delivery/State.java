/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

/**
 * 
 */
public interface State {
  public void enter();
 
  public void execute(OOOProtocolMessage protocolMessage);
}