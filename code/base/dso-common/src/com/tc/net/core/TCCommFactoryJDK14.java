/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

/**
 * JDK14 implementation of comm factory interface
 * 
 * @author teck
 */
class TCCommFactoryJDK14 implements TCCommFactoryIF {

  public TCComm getInstance() {
    return new TCCommJDK14();
  }
}