/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.Iterator;
import java.util.ServiceLoader;

public class ServiceUtil {

  public static <T> T loadService(Class<T> c) {
    ServiceLoader<T> serviceLoader = ServiceLoader.load(c, ServiceUtil.class.getClassLoader());

    T rv = null;
    for (Iterator<T> iter = serviceLoader.iterator(); iter.hasNext();) {
      if (rv == null) {
        rv = iter.next();
      } else {
        throw new AssertionError("Multiple service loaders present for " + c);
      }
    }

    if (rv == null) { throw new AssertionError("No service loader found for " + c); }

    return rv;

  }

}
