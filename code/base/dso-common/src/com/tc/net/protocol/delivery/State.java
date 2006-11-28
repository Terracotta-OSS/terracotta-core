/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.delivery;

/**
 * 
 */
public interface State {
  public void enter();
 
  public void execute(OOOProtocolMessage protocolMessage);
}