/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.exception.TCInternalError;
import com.tc.util.runtime.IOFlavor;

import java.lang.reflect.Constructor;

/**
 * Factory class for getting TCComm instances
 * 
 * @author teck
 */
public class TCCommFactory {

  private static TCCommFactoryIF factory;

  static {

    Class clazz;
    try {
      if (IOFlavor.isNioAvailable()) {
        clazz = Class.forName("com.tc.net.core.TCCommFactoryJDK14");
      } else {
        clazz = Class.forName("com.tc.net.core.TCCommFactoryJDK13");
      }

      Constructor cstr = clazz.getDeclaredConstructor(new Class[] {});
      factory = (TCCommFactoryIF) cstr.newInstance(new Object[] {});
    } catch (Exception e) {
      throw new TCInternalError(e);
    }
  }

  /**
   * Get a new instance of a TCComm instance
   * 
   * @param start true if <code>start()</code> should be called on the returned instance
   * @return a new TCComm instance
   */
  public TCComm getInstance(boolean start) {
    TCComm rv = factory.getInstance();

    if (start) {
      rv.start();
    }

    return rv;
  }

  /**
   * Get a new instance of a TCComm instance that is already <code>start()</code> 'ed. This is equivalent to calling
   * <code>TCCommFactory.getInstance(true)</code>
   * 
   * @return a new TCComm instance
   */
  public TCComm getInstance() {
    return getInstance(true);
  }
}