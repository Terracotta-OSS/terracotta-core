/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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