/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.core;

/**
 * JDK14 implementation of connection manager factory interface
 * 
 * @author teck
 */
class TCConnectionManagerJDK14Factory implements TCConnectionManagerFactoryIF {

  public TCConnectionManager getInstance(TCComm comm) {
    return new TCConnectionManagerJDK14(comm);
  }

}