/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.transport;


public interface ConnectionIdFactory {

  public ConnectionID nextConnectionId();

}
