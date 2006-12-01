/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.exception.TCRuntimeException;
import com.tc.util.runtime.IOFlavor;

import java.lang.reflect.Constructor;

/**
 * Factory class for creating ConnectionManger instances
 * 
 * @author teck
 */
public class TCConnectionManagerFactory {

  private static TCConnectionManagerFactoryIF factory;

  static {
    Class clazz;
    try {
      if (IOFlavor.isNioAvailable()) {
        clazz = Class.forName("com.tc.net.core.TCConnectionManagerJDK14Factory");
      } else {
        clazz = Class.forName("com.tc.net.core.TCConnectionManagerJDK13Factory");
      }

      Constructor cstr = clazz.getDeclaredConstructor(new Class[] {});
      factory = (TCConnectionManagerFactoryIF) cstr.newInstance(new Object[] {});
    } catch (Throwable t) {
      throw new TCRuntimeException(t);
    }
  }

  public TCConnectionManager getInstance() {
    return factory.getInstance(null);
  }

  public TCConnectionManager getInstance(TCComm comm) {
    return factory.getInstance(comm);
  }
}