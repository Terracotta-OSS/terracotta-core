/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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